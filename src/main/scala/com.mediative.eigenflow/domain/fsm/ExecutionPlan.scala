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

package com.mediative.eigenflow.domain.fsm

import com.mediative.eigenflow.domain.{ ProcessContext, RecoveryStrategy }

import scala.concurrent.Future

/**
 * ExecutionPlan is a linked data structure which describes:
 * - stage transitions
 * - error handling strategy per stage
 * - custom messages for publishing
 *
 * @param stage The stage the plan describes.
 * @param f Stage business logic.
 * @param from Deserializer from String to A.
 *             Where the string is a serialized result of the previous stage and A is expected input type for this stage.
 * @param to Serializer from B to String. Serializes the execution result of this stage to String.
 * @param previous Link to previous stage. None for the first stage.
 * @param recoveryStrategy A strategy to handle exceptions thrown during stage execution.
 *                      If no strategy defined for a particular exception the stage will fail.
 * @param recoveryTimeout Time limit for the stage retries.
 *                       When one retry strategy is defined it's fairly easy to calculate timeout:
 *                         ~ (interval + stage_execution_time_before_exception) * number_of_times
 *                       If multiple strategies are defined and different exceptions are thrown time to time
 *                       the total timeout will be a sum of all timeouts. To limit this value globally use
 *                       this timeout parameter, it will stop retrying even if not all attempts are made.
 *
 *                       If timeout reached the stage fails.
 *
 *                       Note: this timeout will NOT interrupt a normal run, it will only have effect on recovery.
 * @param publishMetricsMap A map which allows to calculate different values based on the stage execution result and
 *                          publish those values to the given topic. Typical use case is publishing some execution
 *                          statistical data, like number of records processed or size of file downloaded etc.
 * @tparam A Expected input data type.
 * @tparam B Stage execution result type.
 */
case class ExecutionPlan[A, B](
  stage: ProcessStage, f: ProcessContext => A => Future[B],
  from: String => A, to: B => String,
  recoveryStrategy: Throwable => RecoveryStrategy = RecoveryStrategy.default, recoveryTimeout: Option[Long] = None,
  previous: Option[ExecutionPlan[_, A]] = None,
  publishMetricsMap: Option[(String, B => Map[String, Double])] = None)
