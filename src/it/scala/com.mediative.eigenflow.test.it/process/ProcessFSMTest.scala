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

import com.mediative.eigenflow.domain.fsm._
import com.mediative.eigenflow.test.it.utils.wrappers.TracedProcessFSM
import TracedProcessFSM.Start
import com.mediative.eigenflow.test.it.utils.EigenflowIntegrationTest

import scala.concurrent.duration._

class ProcessFSMTest extends EigenflowIntegrationTest {
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

    "for Stage1 ~> Stage2(FailOnce) ~> Stage3" - {
      "expect transition: Initial -> Stage1 -> Stage2 -> Failed" in {
        system.actorOf(
          TracedProcessFSM.props(`Stage1 ~> Stage2(FailOnce) ~> Stage3`, new Date)
        ) ! Start

        val result = expectMsgType[Seq[ProcessStage]](5.seconds)

        assert(result == Seq(Initial, Stage1, Stage2, Failed))
      }

      "expect restart from the failed stage" in {
        val date = new Date
        system.actorOf(
          TracedProcessFSM.props(`Stage1 ~> Stage2(FailOnce) ~> Stage3`, date)
        ) ! Start

        val result = expectMsgType[Seq[ProcessStage]](5.seconds)

        assert(result == Seq(Initial, Stage1, Stage2, Failed))

        system.actorOf(
          TracedProcessFSM.props(`Stage1 ~> Stage2(FailOnce) ~> Stage3`, date)
        ) ! Start

        val result2 = expectMsgType[Seq[ProcessStage]](5.seconds)

        assert(result2 == Seq(Failed, Stage2, Complete))
      }

      "start over again if forced" in {
        val date = new Date
        system.actorOf(
          TracedProcessFSM.props(`Stage1 ~> Stage2(FailOnce) ~> Stage3`, date)
        ) ! Start

        val result = expectMsgType[Seq[ProcessStage]](5.seconds)

        assert(result == Seq(Initial, Stage1, Stage2, Failed))

        system.actorOf(
          TracedProcessFSM.props(`Stage1 ~> Stage2(FailOnce) ~> Stage3`, date, reset = true)
        ) ! Start

        val result2 = expectMsgType[Seq[ProcessStage]](5.seconds)

        assert(result2 == Seq(Initial, Stage1, Stage2, Failed))
      }
    }
  }
}