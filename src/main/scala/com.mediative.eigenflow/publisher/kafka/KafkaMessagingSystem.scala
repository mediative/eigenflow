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
import org.apache.kafka.clients.producer.{ Callback, KafkaProducer, ProducerRecord, RecordMetadata }

class KafkaMessagingSystem(log: LoggingAdapter) extends MessagingSystem {
  private val producer = new KafkaProducer[String, String](KafkaConfiguration.properties)

  override def publish(topic: String, message: String): Unit = {
    log.info(s"Publishing to $topic :\n$message\n")

    producer.send(new ProducerRecord[String, String](topic, message), new Callback {
      override def onCompletion(metadata: RecordMetadata, exception: Exception): Unit = {
        if (exception != null) {
          log.error(s"Cannot publish to $topic. Caused by: ${exception.getMessage}", exception)
        }
      }
    })
    ()
  }

  override def stop(): Unit = {
    producer.close()
  }
}
