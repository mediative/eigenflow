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

import java.util.Date

import akka.actor.SupervisorStrategy.Escalate
import akka.actor.{ ActorLogging, OneForOneStrategy, Props, SupervisorStrategy }
import akka.persistence.PersistentActor
import com.mediative.eigenflow.StagedProcess
import com.mediative.eigenflow.helpers.DateHelper._
import com.mediative.eigenflow.publisher.MessagingSystem

private[eigenflow] object ProcessManager {

  // Persistent Event
  case class ProcessingDateState(date: Date, complete: Boolean)

  // Commands
  case object Continue

  case object ProcessComplete

  case object ProcessFailed

  val SuccessCode = 0
  val FailureCode = 1
}

/**
 * The process parent actor which creates FSM actors which actually run processes based on processingDate.
 *
 */
private[eigenflow] class ProcessManager(process: StagedProcess, startDate: Option[Date], processTypeId: String)(implicit val messagingSystem: MessagingSystem) extends PersistentActor with ActorLogging {

  import com.mediative.eigenflow.process.ProcessManager._

  override def supervisorStrategy: SupervisorStrategy = {
    OneForOneStrategy() {
      case t => Escalate
    }
  }

  override protected def onRecoveryFailure(cause: Throwable, event: Option[Any]): Unit = fail

  override protected def onPersistFailure(cause: Throwable, event: Any, seqNr: Long): Unit = fail

  override def persistenceId: String = s"${processTypeId}-manager"

  override def receiveRecover: Receive = {
    case ProcessingDateState(date, complete) =>
      val dateToProcess = if (complete) {
        process.nextProcessingDate(date) // the stage was complete successfully switch to the next processing date
      } else {
        date
      }
      context.become(ready(startDate.getOrElse(dateToProcess)))
  }

  override def receiveCommand: Receive = ready(startDate.getOrElse(process.initialProcessingDate))

  def ready(processingDate: Date): Receive = {
    case Continue =>
      val now = new Date
      if (processingDate.before(now) || processingDate.equals(now)) {
        persist(ProcessingDateState(processingDate, complete = false)) { event =>
          // TODO: think of a better way to couple `startDate` and `reset`
          val processFSM = context.actorOf(Props(new ProcessFSM(process, processingDate, processTypeId, reset = startDate.isDefined)))

          context.become(waitResult(processingDate))
          processFSM ! ProcessFSM.Continue
        }
      } else {
        log.info(s"The process is idle until: ${TimeFormat.format(processingDate)}")
        done
      }
  }

  def waitResult(processingDate: Date): Receive = {
    case ProcessComplete =>
      persist(ProcessingDateState(processingDate, complete = true)) { event =>
        val nextProcessingDate = process.nextProcessingDate(event.date)

        context.become(ready(nextProcessingDate))
        self ! Continue
      }

    case ProcessFailed =>
      fail
  }

  private def fail() = context.parent ! ProcessSupervisor.Failed
  private def done() = context.parent ! ProcessSupervisor.Done
}
