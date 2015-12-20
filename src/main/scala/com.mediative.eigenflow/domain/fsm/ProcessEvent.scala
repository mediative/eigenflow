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

import com.mediative.eigenflow.domain.Retry

/**
 * Events of this type are persisted and replayed by akka PersistentFSM to restore the latest state.
 *
 * They are used internally by eigenflow to control context changes, logging and events publishing on stage transitions.
 */
sealed trait ProcessEvent

case class StageComplete(nextStage: ProcessStage, result: String) extends ProcessEvent

case class StageFailed(failure: Throwable) extends ProcessEvent

case class StageRetry(failure: Throwable, retry: Retry, timeout: Option[Long]) extends ProcessEvent
