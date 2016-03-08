package com.mediative.eigenflow.test.it.utils.wrappers

import java.util.Date

import akka.actor.{ ActorRef, Props }
import akka.event.LoggingAdapter
import com.mediative.eigenflow.StagedProcess
import com.mediative.eigenflow.process.ProcessManager
import com.mediative.eigenflow.process.ProcessManager.ProcessingDateState
import com.mediative.eigenflow.publisher.MessagingSystem

object TracedProcessManager {
  private implicit def messagingSystem = new MessagingSystem {
    override def publish(topic: String, message: String)(implicit log: LoggingAdapter): Unit = () // ignore
  }

  def props(process: StagedProcess,
    receiver: ActorRef,
    date: Option[Date] = None,
    id: Option[String] = None) = Props(new TracedProcessManager(process, date, receiver, id))

  class TracedProcessManager(process: StagedProcess,
      date: Option[Date],
      receiver: ActorRef,
      id: Option[String] = None) extends ProcessManager(process, date, _ => ()) {

    override def persistenceId: String = id.getOrElse(super.persistenceId)

    override def persist[A](event: A)(handler: (A) => Unit): Unit = {
      event match {
        case p @ ProcessingDateState(_, _) =>
          receiver ! p
      }
      super.persist(event)(handler)
    }
  }

}
