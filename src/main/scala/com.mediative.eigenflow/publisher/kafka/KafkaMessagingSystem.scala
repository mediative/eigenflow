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

import akka.event.LoggingAdapter
import com.mediative.eigenflow.publisher.MessagingSystem
import com.typesafe.config.Config
import org.apache.kafka.clients.producer.{ Callback, KafkaProducer, ProducerRecord, RecordMetadata }

class KafkaMessagingSystem(config: Config) extends MessagingSystem(config) {
  private val properties = KafkaConfiguration.properties(config)
  private val producer = new KafkaProducer[String, String](properties)
  private val topicPrefix = properties.getProperty("topic.prefix")

  override def publish(topic: String, message: String)(implicit log: LoggingAdapter): Unit = {
    val topicName = s"$topicPrefix-$topic"

    log.info(s"Publishing to $topicName :\n$message\n")

    producer.send(new ProducerRecord[String, String](topicName, message), new Callback {
      override def onCompletion(metadata: RecordMetadata, exception: Exception): Unit = {
        if (exception != null) {
          log.error(s"Cannot publish to $topicName. Caused by: ${exception.getMessage}", exception)
        }
      }
    })
    ()
  }

  override def stop(): Unit = {
    producer.close()
  }
}
