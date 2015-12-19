package com.mediative.eigenflow.domain.messages

/**
 * A higher level GenericMessage which allows to define a map of values instead of plain string.
 * Serialization to json format is handled by platform.
 *
 * @see GenericMessage
 */
case class MetricsMessage(timestamp: Long, processId: String, stage: String, message: Map[String, Double])
