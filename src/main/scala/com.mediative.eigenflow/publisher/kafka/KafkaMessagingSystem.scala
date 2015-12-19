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
