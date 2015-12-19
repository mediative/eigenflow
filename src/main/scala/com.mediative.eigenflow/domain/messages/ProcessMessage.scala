package com.mediative.eigenflow.domain.messages

/**
 * Message for process state changes.
 *
 * @param timestamp Time when the message was created.
 * @param processId Process identification.
 * @param processingDate Processing date of the process.
 * @param state Process state.
 * @param duration How long did it take to switch to the given state.
 *                 Initial state's duration should be 0.
 * @param message Additional information relevant to the process state.
 *                For example: the complete state message will be the next processing date,
 *                or the failed state message will contain exception details.
 */
case class ProcessMessage(
  timestamp: Long,
  processId: String,
  processingDate: String,
  state: String,
  duration: Long,
  message: String)
