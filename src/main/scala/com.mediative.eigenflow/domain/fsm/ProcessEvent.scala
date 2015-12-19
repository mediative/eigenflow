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