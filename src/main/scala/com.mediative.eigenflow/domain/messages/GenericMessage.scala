package com.mediative.eigenflow.domain.messages

/**
 * A generic message to be sent on stage complete.
 *
 * @param timestamp Time when the message was created.
 * @param runId Identification of the running process for which the message was created.
 * @param stage Stage name for which the message was created.
 * @param message A generic message.
 */
case class GenericMessage(timestamp: Long, runId: String, stage: String, message: String)