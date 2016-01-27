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

import akka.persistence.fsm.PersistentFSM.FSMState

/**
 * A state trait for FSM, extend it for custom stages.
 */
trait ProcessStage extends FSMState {
  def identifier = toString
}

/**
 * Always the first stage of any process.
 */
case object Initial extends ProcessStage

/**
 * Final stage of a successfully ended process.
 */
case object Complete extends ProcessStage

/**
 * Indicates that a stage failed, for either unhandled exception or exhausted retries.
 */
case object Failed extends ProcessStage

/**
 * Indicates that there is a stage failed and is waiting for a scheduled retry.
 */
case object Retrying extends ProcessStage
