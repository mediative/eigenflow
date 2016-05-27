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

package com.mediative.eigenflow.publisher.kafka

import java.util.Properties

import com.mediative.eigenflow.environment.ConfigurationLoader
import com.typesafe.config.Config

object KafkaConfiguration {
  def properties(config: Config): Properties = {
    val propertiesKeys = Seq(
      "bootstrap.servers",
      "acks",
      "retries",
      "batch.size",
      "linger.ms",
      "buffer.memory",
      "key.serializer",
      "value.serializer",
      "topic.prefix")

    val properties = new Properties()
    propertiesKeys.foreach(key => properties.setProperty(key, config.getString(s"eigenflow.kafka.${key}")))

    properties
  }
}
