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