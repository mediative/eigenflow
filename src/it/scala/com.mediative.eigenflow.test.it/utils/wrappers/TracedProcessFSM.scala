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

package com.mediative.eigenflow.test.it.utils.wrappers

import java.util.Date

import akka.actor.Props
import akka.event.LoggingAdapter
import com.mediative.eigenflow.StagedProcess
import com.mediative.eigenflow.domain.ProcessContext
import com.mediative.eigenflow.domain.fsm.{ ProcessEvent, ProcessStage }
import com.mediative.eigenflow.process.ProcessFSM
import com.mediative.eigenflow.process.ProcessFSM.Continue
import com.mediative.eigenflow.publisher.MessagingSystem

object TracedProcessFSM {

  implicit def messagingSystem = new MessagingSystem {
    override def publish(topic: String, message: String)(implicit log: LoggingAdapter): Unit = () // ignore
  }

  def props(process: StagedProcess, date: Date, reset: Boolean = false) = Props(new TracedProcessFSM(process, date, reset))

  case object Start

  class TracedProcessFSM(process: StagedProcess, date: Date, reset: Boolean) extends ProcessFSM(process, date, reset) {
    private var originalSender = sender
    private var stagesRegistry: Seq[ProcessStage] = Seq.empty

    def testReceive: PartialFunction[Any, Unit] = {
      case Start =>
        originalSender = sender()
        self ! Continue
    }

    override def applyEvent(domainEvent: ProcessEvent, processContext: ProcessContext): ProcessContext = {
      // register the last stage just before switching it, but only for the current run.
      // if this actor is restored form a previous run, the history recovery will not be registered,
      // it allows to reason about stages within one run context.
      // if a need for the full history arises then another registry should be created.
      if (!recoveryRunning) {
        stagesRegistry = stagesRegistry.:+(stateName)
      }
      super.applyEvent(domainEvent, processContext)
    }

    override def postStop(): Unit = {
      stagesRegistry = stagesRegistry.:+(stateName) // add the last state
      originalSender ! stagesRegistry
      super.postStop()
    }

    override def receive = testReceive orElse super.receive
  }

}
