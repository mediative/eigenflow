package com.mediative.eigenflow.publisher

trait MessagingSystem {
  def publish(topic: String, message: String): Unit
  def stop(): Unit = ()
}
