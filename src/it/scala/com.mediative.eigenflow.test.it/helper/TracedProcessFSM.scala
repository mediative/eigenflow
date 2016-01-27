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

package com.mediative.eigenflow.test.it.helper

import java.util.Date

import akka.actor.Props
import com.mediative.eigenflow.StagedProcess
import com.mediative.eigenflow.domain.ProcessContext
import com.mediative.eigenflow.domain.fsm.{ ProcessEvent, ProcessStage }
import com.mediative.eigenflow.process.ProcessFSM
import com.mediative.eigenflow.process.ProcessFSM.Continue
import com.mediative.eigenflow.publisher.MessagingSystem

object TracedProcessFSM {

  implicit def messagingSystem = new MessagingSystem {
    override def publish(topic: String, message: String): Unit = () // ignore
  }

  def props(process: StagedProcess, date: Date) = Props(new TracedProcessFSM(process, date))

  case object Start

  class TracedProcessFSM(process: StagedProcess, date: Date) extends ProcessFSM(process, date) {
    private var originalSender = sender
    private var stagesRegistry: Seq[ProcessStage] = Seq.empty

    def testReceive: PartialFunction[Any, Unit] = {
      case Start =>
        originalSender = sender()
        self ! Continue
    }

    override def applyEvent(domainEvent: ProcessEvent, processContext: ProcessContext): ProcessContext = {
      stagesRegistry = stagesRegistry.:+(stateName) // register the last state just before switching it
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
