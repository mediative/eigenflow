package com.mediative.eigenflow.publisher.kafka

import java.util.Properties

import com.mediative.eigenflow.environment.ConfigurationLoader

object KafkaConfiguration {
  private val config = ConfigurationLoader.config

  def properties(): Properties = {
    val propertiesKeys = Seq(
      "bootstrap.servers",
      "acks",
      "retries",
      "batch.size",
      "linger.ms",
      "buffer.memory",
      "key.serializer",
      "value.serializer")

    val properties = new Properties()
    propertiesKeys.foreach(key => properties.setProperty(key, config.getString(s"eigenflow.kafka.${key}")))

    properties
  }
}
