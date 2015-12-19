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

package com.mediative.eigenflow.domain.fsm

import com.mediative.eigenflow.domain.{ ProcessContext, Retry }

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
 * @param retryStrategy A strategy to handle exceptions thrown during stage execution.
 *                      If no strategy defined for a particular exception the stage will fail.
 * @param retriesTimeout Time limit for the stage retries.
 *                       It is straightforward to predict timeout when one retry strategy is defined:
 *                         ~ (interval + stage_execution_time_before_exception) * number_of_times
 *                       If multiple strategies are defined and different handled exceptions are thrown time to time
 *                       the total time out will be a sum of all strategies, thus:
 *                       this timeout parameter defines when to stop retrying even if not all attempts are made.
 *
 *                       If timeout was reached the stage will fail.
 *
 *                       Note: this timeout will NOT interrupt a normal run, it will only have effect on retries.
 * @param publishMetricsMap A map which allows to calculate different values based on the stage execution result and
 *                          publish those values to the given topic. Typical use case is publishing some execution
 *                          statistical data, like number of records processed or size of file downloaded etc.
 * @tparam A Expected input data type.
 * @tparam B Stage execution result type.
 */
case class ExecutionPlan[A, B](
  stage: ProcessStage, f: ProcessContext => A => Future[B],
  from: String => A, to: B => String,
  retryStrategy: Throwable => Option[Retry] = _ => None, retriesTimeout: Option[Long] = None,
  previous: Option[ExecutionPlan[_, A]] = None,
  publishMetricsMap: Option[(String, B => Map[String, Double])] = None)
