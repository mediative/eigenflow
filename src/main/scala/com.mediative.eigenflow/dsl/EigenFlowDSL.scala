/*
 * Copyright 2015 Mediative
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

package com.mediative.eigenflow.dsl

import com.mediative.eigenflow.domain.{ Retry, ProcessContext }
import com.mediative.eigenflow.domain.fsm.{ ExecutionPlan, ProcessStage }

import scala.concurrent.Future
import scala.concurrent.duration.{ Duration, FiniteDuration }
import scala.language.implicitConversions

trait EigenFlowDSL {

  implicit class ExecutionPlanWrapper[A, B](executionPlan: ExecutionPlan[A, B]) {
    /**
     * Link execution plans.
     */
    def ~>[C](nextPlan: ExecutionPlan[B, C]): ExecutionPlan[B, C] =
      nextPlan.copy(previous = Some(executionPlan))

    /**
     * Define a simple retry logic, one retry logic for all types of exceptions.
     *
     * @param interval Interval between retries.
     * @param attempts Number of retries.
     */
    def retry(interval: FiniteDuration, attempts: Int): ExecutionPlan[A, B] =
      executionPlan.copy(retryStrategy = _ => Some(Retry(interval, attempts)))

    /**
     * Define advanced retry strategy with individual case per expected exception.
     * For what is not covered by the given partial a NoRetry strategy will be applied, what means propagate exception further (JobFailed).
     *
     * @param f Partial function which maps exception to a retry strategy.
     */
    def retry(f: PartialFunction[Throwable, Retry]): ExecutionPlan[A, B] =
      executionPlan.copy(retryStrategy = t => Some((f orElse Retry.NoRetry)(t)))

    /**
     * Publish custom data to the given topic.
     *
     * @param topic Topic to publish messages to.
     * @param f Function to build a key map value from the stage execution result.
     */
    def publishMetrics(topic: String, f: B => Map[String, Double]): ExecutionPlan[A, B] =
      executionPlan.copy(publishMetricsMap = Some((topic, f)))

    /**
     * Define retries timeout. Makes sense only in combination with 'retry' method.
     *
     * @param timeout Timeout after which the stage should fail if exception happend.
     */
    def retriesTimeout(timeout: Duration): ExecutionPlan[A, B] =
      executionPlan.copy(retriesTimeout = Some(timeout.toMillis))
  }

  /**
   * A wrapper over JobStage, helps to build DSL where stage logic can be described right beside the stage class, for example:
   *
   * val downloading = Downloading {
   * ...
   * }
   *
   * @param stage Stage to wrap
   */
  implicit class ProcessStageWrapper(val stage: ProcessStage) {
    /**
     * Define a stage execution function for stages w/o input date, usually initial stages.
     */
    def apply[B](f: => Future[B])(implicit to: B => String): ExecutionPlan[Unit, B] =
      ExecutionPlan(stage, (_: ProcessContext) => _ => f, _ => (), to)

    /**
     * Define a stage execution function with the previous stage's result as input.
     */
    def apply[A, B](f: A => Future[B])(implicit from: String => A, to: B => String): ExecutionPlan[A, B] =
      ExecutionPlan(stage, (_: ProcessContext) => f, from, to)

    /**
     * Define a stage execution function with access to the context data.
     * The context data usually useful on the very first stage where the access to 'processingDate' makes the most sense.
     *
     * Other stages (after the first one) should pass context data using stage messages and do not rely on the ProcessContext,
     * with a very rare exceptions, see: withContext[A, B]
     *
     */
    def withContext[B](f: ProcessContext => Future[B])(implicit to: B => String): ExecutionPlan[String, B] =
      ExecutionPlan(stage, (data: ProcessContext) => { _: String => f(data) }, x => x, to)

    /**
     * Define a stage execution function with access to the run context and the result of previous stage.
     *
     * This function covers such rare cases when a stage tries to behave differently depending on failure,
     * the state can be propagated through the exception and the stage can read it from the context.failure field.
     * Note: this technique should be used as the last resort, when it's absolutely necessary to keep state between retries.
     *
     */
    def withContext[A, B](f: ProcessContext => A => Future[B])(implicit from: String => A, to: B => String): ExecutionPlan[A, B] =
      ExecutionPlan(stage, f, from, to)
  }
}
