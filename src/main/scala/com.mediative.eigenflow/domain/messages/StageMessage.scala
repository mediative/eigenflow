package com.mediative.eigenflow.domain.messages

/**
 * Message for stage state changes.
 *
 * @param timestamp Time when the message was created.
 * @param processId Process identification.
 * @param stage Stage name.
 * @param state Stage state.
 * @param duration How long did it take to switch to the given state.
 *                 Initial state's duration should be 0.
 * @param message Data passed between stages.
 */
case class StageMessage(timestamp: Long,
  processId: String,
  stage: String,
  state: String,
  duration: Long,
  message: String)
