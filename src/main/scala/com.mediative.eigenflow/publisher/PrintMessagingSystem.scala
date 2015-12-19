package com.mediative.eigenflow.publisher

import akka.event.LoggingAdapter

class PrintMessagingSystem(log: LoggingAdapter) extends MessagingSystem {
  override def publish(topic: String, message: String): Unit = {
    log.info(s"Publishing to $topic :\n$message\n")
  }
}
