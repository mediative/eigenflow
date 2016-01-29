/*
 * Copyright 2016 Mediative
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mediative.eigenflow.process

import java.util.{ Date, UUID }

import akka.actor.{ Actor, ActorLogging }
import akka.persistence.fsm.PersistentFSM
import com.mediative.eigenflow.StagedProcess
import com.mediative.eigenflow.domain.RecoveryStrategy.{ Fail, Retry }
import com.mediative.eigenflow.domain.fsm._
import com.mediative.eigenflow.domain.{ RecoveryStrategy, ProcessContext, RetryRegistry }
import com.mediative.eigenflow.environment.ProcessConfiguration
import com.mediative.eigenflow.process.ProcessFSM.{ CompleteExecution, Continue, FailExecution }
import com.mediative.eigenflow.process.ProcessManager.{ ProcessComplete, ProcessFailed }
import com.mediative.eigenflow.publisher.{ MessagingSystem, ProcessPublisher }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.reflect.ClassTag
import scala.util.{ Failure, Success }

/**
 * State Machine implementation.
 * This class persists stage transitions with the process context, thus if the process failed it can be restored
 * and start from the failed stage.
 * The processingDate parameter is used to restore a process from the previous stage.
 *
 */
class ProcessFSM(process: StagedProcess, processingDate: Date)(implicit override val domainEventClassTag: ClassTag[ProcessEvent], override val publisher: MessagingSystem)
    extends Actor with PersistentFSM[ProcessStage, ProcessContext, ProcessEvent] with ProcessPublisher with ActorLogging {
  private val processTypeId = ProcessConfiguration.load.id

  // define persistence id depending on the processing date, it makes processes independent of each other.
  // it also gives flexibility to redefine processing flow w/o need to clean the history,
  // otherwise persistence wouldn't be able to restore the state.
  override def persistenceId: String = s"$processTypeId-${processingDate.getTime}"

  // used as the main topic and prefix for sub topics (stages, metrics) for the messaging system (kafka)
  override def baseTopic = processTypeId

  // don't publish messages during recovery!
  override def publishingActive = !recoveryRunning

  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    buildTransitions(process.executionPlan)
    super.preStart()
  }

  /**
   * Update context data and publish events as process moves from stage to stage.
   */
  override def applyEvent(domainEvent: ProcessEvent, processContext: ProcessContext): ProcessContext = {
    domainEvent match {

      case StageComplete(nextStage, message) =>
        if (!recoveryRunning) log.info(s"Stage transition: $currentStage ~> $nextStage")

        (currentStage, nextStage) match {
          case (Initial, toStage) =>
            publishProcessStarting(processContext)
            publishStageStarting(processContext.processId, nextStage, message)
            processContext.updateStage(toStage)

          case (Retrying, _) =>
            publishStageRetrying(processContext)
            processContext

          case (Failed, _) =>
            publishStageStarting(processContext.processId, nextStage, message)
            processContext.
              updateStage(nextStage).
              emptyRetryRegistry()

          case (fromStage, Complete) =>
            publishStageComplete(processContext, message)
            publishProcessComplete(processContext, process.nextProcessingDate(processContext.processingDate))
            processContext.
              updateStage(nextStage).
              emptyFailureMessage().
              emptyRetryRegistry()

          case (fromStage, toStage) =>
            publishStageComplete(processContext, message)
            publishStageStarting(processContext.processId, nextStage, message)
            processContext.
              updateStage(toStage).
              updateMessage(message).
              emptyFailureMessage().
              emptyRetryRegistry()
        }

      case StageRetry(failure, retry, timeout) =>
        val registeredFailure = RetryRegistry.addFailure(processContext.retryRegistry, failure, retry, timeout)

        if (!recoveryRunning) {
          val failureRegistry = RetryRegistry.findFailure(Some(registeredFailure), failure)
          failureRegistry.foreach { registry =>
            log.warning(s"Retry stage $currentStage in ${retry.interval} " +
              s"(attempt ${registry.attemptsMade} of ${registry.maxAttempts}). " +
              s"Caused by: $failure")
          }
        }

        processContext.
          addFailureMessage(failure).
          updateRetryRegistryWith(registeredFailure)

      case StageFailed(failure) =>
        if (!recoveryRunning) log.error(s"Process failed on stage $currentStage. Caused by: $failure")
        if (!recoveryRunning) failure.printStackTrace()

        publishStageFailed(processContext, failure)
        publishProcessFailed(processContext, failure)
        processContext.
          addFailureMessage(failure)

    }
  }

  //
  // Transition logic from the given stage.
  //
  startWith(Initial, ProcessContext(
    timestamp = System.currentTimeMillis(),
    processingDate = processingDate,
    processId = UUID.randomUUID().toString,
    startTime = System.currentTimeMillis(),
    stage = Initial,
    message = ""
  ))

  when(Initial) {
    case Event(Continue, _) =>
      moveTo(firstStage)
  }

  when(Complete) {
    case Event(Continue, _) =>
      context.parent ! ProcessComplete
      stop()
  }

  when(Retrying) {
    case Event(Continue, processContext) =>
      moveTo(processContext.stage, processContext.message)
  }

  when(Failed) {
    case Event(Continue, processContext) =>
      moveTo(processContext.stage, processContext.message)
  }

  /**
   * Register stages based on the given execution plan.
   *
   */
  private def buildTransitions[A, B](plan: ExecutionPlan[A, B], nextStage: ProcessStage = Complete): Unit = {
    when(plan.stage) {
      case Event(Continue, processContext) =>
        try {
          plan.f(processContext)(plan.from(processContext.message)).onComplete {
            case Success(result) =>
              plan.publishMetricsMap.foreach {
                case (topic, toMap) =>
                  publishMetrics(topic, processContext, toMap(result))
              }
              self ! CompleteExecution(plan.to(result))
            case Failure(failure: Throwable) =>
              self ! FailExecution(failure)
          }
        } catch {
          case failure: Throwable =>
            self ! FailExecution(failure)
        }
        stay()
      case Event(CompleteExecution(message), _) =>
        moveTo(nextStage, message)
      case Event(FailExecution(failure), processContext) =>
        plan.recoveryStrategy(failure) match {
          case retry @ Retry(_, _) => // stage has a recovery plan, check if it's valid for next retry then retry or fail
            if (RetryRegistry.isValidForNextRetry(processContext.retryRegistry, failure)) {
              retryStage(failure, retry, plan.recoveryTimeout)
            } else {
              failProcess(failure)
            }
          case RecoveryStrategy.Complete => // skip this instance and move on (mark as complete)
            moveTo(Complete, s"forced to complete by recovery strategy. failure: ${failure.getMessage}")
          case Fail => // no recovery plan for this stage, fail the process
            failProcess(failure)
        }
    }

    plan.previous.foreach(buildTransitions(_, plan.stage))
  }

  //
  // Helper methods and classes
  //
  private def moveTo(stage: ProcessStage, message: String = "") = {
    goto(stage) applying StageComplete(stage, message) andThen (_ => self ! Continue)
  }

  private def failProcess(failure: Throwable) = {
    goto(Failed) applying StageFailed(failure) andThen (_ => context.parent ! ProcessFailed)
  }

  private def retryStage(failure: Throwable, retry: Retry, timeout: Option[Long]) = {
    goto(Retrying) applying StageRetry(failure, retry, timeout) andThen { _ =>
      val now = System.currentTimeMillis()
      val timeDefinedInRetry = now + retry.interval.toMillis
      val nextRetryTime = timeout.map { deadline =>
        timeDefinedInRetry.min(deadline)
      }.getOrElse(timeDefinedInRetry)

      val nextRetry = FiniteDuration((nextRetryTime - now) / 1000, SECONDS)
      context.system.scheduler.scheduleOnce(nextRetry, self, Continue)
      log.debug(s"stage retry scheduled in ${nextRetry} seconds")
    }
  }

  private lazy val firstStage: ProcessStage = {
    def stageUp(plan: ExecutionPlan[_, _]): ProcessStage = plan.previous.fold(plan.stage)(stageUp)
    stageUp(process.executionPlan)
  }

  /**
   * Alias for the stateName function to make it explicit that it's the current stage.
   *
   */
  private def currentStage = stateName

  /**
   * A context wrapper class for helper functions
   */
  private implicit class ProcessContextHelper(processContext: ProcessContext) {
    def emptyFailureMessage() = processContext.copy(failure = None)

    def addFailureMessage(failure: Throwable) = processContext.copy(failure = Some(failure))

    def emptyRetryRegistry() = processContext.copy(retryRegistry = None)

    def updateRetryRegistryWith(retryRegistry: RetryRegistry) = {
      processContext.copy(retryRegistry = Some(retryRegistry))
    }

    def updateStage(stage: ProcessStage) = processContext.copy(timestamp = System.currentTimeMillis(), stage = stage)

    def updateMessage(message: String) = processContext.copy(message = message)
  }

}

object ProcessFSM {

  case object Continue

  // Message to trigger transition after a successful stage execution.
  case class CompleteExecution(data: String)

  // Message to trigger recovery or failure after a stage execution failure.
  case class FailExecution(failure: Throwable)

}
