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

package com.mediative.eigenflow.test
package it.process

import java.util.Date

import akka.actor.ActorSystem
import akka.testkit.{ ImplicitSender, TestKit }
import com.mediative.eigenflow.domain.fsm.{ Retrying, Complete, Initial, ProcessStage }
import com.mediative.eigenflow.dsl.EigenflowDSL
import com.mediative.eigenflow.test.it.helper.TracedProcessFSM
import com.mediative.eigenflow.test.it.helper.TracedProcessFSM.Start
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{ BeforeAndAfterAll, FreeSpecLike, Matchers }

import scala.concurrent.duration._

class ProcessFSMTest(_system: ActorSystem)
    extends TestKit(_system) with ImplicitSender
    with FreeSpecLike with ScalaFutures with GeneratorDrivenPropertyChecks with Matchers with BeforeAndAfterAll
    with EigenflowDSL {

  def this() = this(ActorSystem("EigenflowTestActorSystem"))

  "ProcessFSM" - {
    "for Stage1 ~> Stage2" - {
      "expect transition: Initial -> Stage1 -> Stage2 -> Complete" in {
        system.actorOf(
          TracedProcessFSM.props(`Stage1 ~> Stage2`, new Date)
        ) ! Start

        val result = expectMsgType[Seq[ProcessStage]](5.seconds)

        assert(result == Seq(Initial, Stage1, Stage2, Complete))
      }
    }

    "for Stage1(retry) ~> Stage2" - {
      "expect transition: Initial -> Stage1 -> Retry -> Stage1 -> Stage2 -> Complete" in {
        system.actorOf(
          TracedProcessFSM.props(`Stage1(retry) ~> Stage2`, new Date)
        ) ! Start

        val result = expectMsgType[Seq[ProcessStage]](5.seconds)

        assert(result == Seq(Initial, Stage1, Retrying, Stage1, Stage2, Complete))
      }
    }

    "for Stage1(Complete) ~> Stage2" - {
      "expect transition: Initial -> Stage1 -> Complete" in {
        system.actorOf(
          TracedProcessFSM.props(`Stage1(Complete) ~> Stage2`, new Date)
        ) ! Start

        val result = expectMsgType[Seq[ProcessStage]](5.seconds)

        assert(result == Seq(Initial, Stage1, Complete))
      }
    }
  }
  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }
}