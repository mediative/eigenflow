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

package com.mediative.eigenflow.domain

import scala.concurrent.duration.FiniteDuration

/**
 * Retry strategy.
 *
 * @param interval Interval between retries.
 * @param attempts Number of retries.
 */
case class Retry(interval: FiniteDuration, attempts: Int)

object Retry {

  import scala.concurrent.duration._

  def NoRetry: PartialFunction[Throwable, Retry] = {
    case _ => Retry(interval = 0.seconds, attempts = 0)
  }
}
