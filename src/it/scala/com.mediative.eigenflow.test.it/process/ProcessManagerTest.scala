package com.mediative.eigenflow.test.it.process

import java.util.{ UUID, Date }

import com.mediative.eigenflow.process.ProcessManager.{ Continue, ProcessingDateState }
import com.mediative.eigenflow.test.it.utils.wrappers.TracedProcessManager
import com.mediative.eigenflow.test.it.utils.models.{ FailingProcess, OneStageProcess }
import com.mediative.eigenflow.test.it.utils.EigenflowIntegrationTest
import org.scalacheck.Gen

import scala.concurrent.duration._

class ProcessManagerTest extends EigenflowIntegrationTest {
  "ProcessManager" - {
    "when at least one or more run cycles missed" - {
      "must run process as many times as many runs are missing" in {

        // should be long enough so that all runs execution time will not exceed it,
        // otherwise it will execute one more cycle
        val intervalInMs = 10000L

        // add time interval to the previous date.
        def processingDate(intervalInMs: Long): Date => Date = date => new Date(date.getTime + intervalInMs)

        // generate tests with 1-3 run cycles.
        forAll(Gen.choose(1, 3), minSuccessful(4)) { runs =>
          val persistenceId = Some(UUID.randomUUID().toString)

          val dateIterator = processingDate(intervalInMs)
          val now = System.currentTimeMillis()

          // start with the time sufficient to run exactly the given number of cycles
          val startDate = new Date(now - runs * intervalInMs)
          val process = OneStageProcess.create(dateIterator)

          val processManager = system.actorOf(TracedProcessManager.props(
            process = process,
            receiver = self,
            date = Some(startDate),
            id = persistenceId))

          processManager ! Continue

          val dates = Iterator.iterate(startDate)(dateIterator).
            takeWhile(date => date.before(new Date(now)) || date.equals(new Date(now))).
            flatMap(date => Seq(ProcessingDateState(date, false), ProcessingDateState(date, true))).toSeq

          expectMsgAllOf[ProcessingDateState](5.seconds, dates: _*)

          expectNoMsg(2.seconds)
        }
      }
    }

    "when run is complete" - {
      "must not run until the next processing date" in {
        val persistenceId = Some(UUID.randomUUID().toString)
        val farInFuture = 1000000000L

        val process = OneStageProcess.create(_ => new Date(System.currentTimeMillis() + farInFuture))
        val processManager = system.actorOf(TracedProcessManager.props(
          process = process,
          receiver = self,
          id = persistenceId))

        processManager ! Continue
        // expect exactly 2 messages: first for run registration second for run complete
        expectMsgType[ProcessingDateState]
        expectMsgType[ProcessingDateState]
        expectNoMsg

        // simulate restart by creating ProcessManager with the same persistenceId
        val processManager2 = system.actorOf(TracedProcessManager.props(
          process = process,
          receiver = self,
          id = persistenceId))

        processManager2 ! Continue
        // since the process was completed in previous run and the next processing date should be in the future
        // no runs should be executed, thus no messages received.
        expectNoMsg
      }

      "must re-run if start date is set" in {
        val persistenceId = Some(UUID.randomUUID().toString)
        val farInFuture = 1000000000L

        val process = OneStageProcess.create(_ => new Date(System.currentTimeMillis() + farInFuture))

        val startDate = new Date // fix the date to be able to restart from the same point

        val processManager = system.actorOf(TracedProcessManager.props(
          process = process,
          receiver = self,
          date = Some(startDate),
          id = persistenceId))

        processManager ! Continue
        // expect exactly 2 messages: first for run registration second for run complete
        expectMsgType[ProcessingDateState]
        expectMsgType[ProcessingDateState]
        expectNoMsg

        val processManager2 = system.actorOf(TracedProcessManager.props(
          process = process,
          receiver = self,
          date = Some(startDate),
          id = persistenceId))

        processManager2 ! Continue
        // same messages again, for re-run was forced by startDate
        expectMsgAllOf(ProcessingDateState(startDate, false), ProcessingDateState(startDate, true))
        expectNoMsg

      }
    }

    "when run fails" - {
      "must not persist complete status" in {
        val farInFuture = 1000000000L
        val process = FailingProcess.create(_ => new Date(System.currentTimeMillis() + farInFuture))

        val processManager = system.actorOf(TracedProcessManager.props(process = process, receiver = self))
        processManager ! Continue
        val response = expectMsgType[ProcessingDateState]
        expectNoMsg()

        assert(!response.complete)
      }

      "must restart from the failed run" in {
        val persistenceId = Some(UUID.randomUUID().toString)
        val farInFuture = 1000000000L
        val process = FailingProcess.create(_ => new Date(System.currentTimeMillis() + farInFuture))

        val processManager = system.actorOf(TracedProcessManager.props(process = process, receiver = self, id = persistenceId))
        processManager ! Continue
        val response = expectMsgType[ProcessingDateState]
        expectNoMsg()

        assert(!response.complete)

        val processManager2 = system.actorOf(TracedProcessManager.props(process = process, receiver = self, id = persistenceId))
        processManager2 ! Continue
        val response2 = expectMsgType[ProcessingDateState]
        expectNoMsg()

        assert(response.date.equals(response2.date))
      }
    }
  }
}
