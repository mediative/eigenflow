package com.mediative.eigenflow.test.publisher

import java.util.Date

import com.mediative.eigenflow.domain.ProcessContext
import com.mediative.eigenflow.domain.messages._
import com.mediative.eigenflow.helpers.DateHelper._
import com.mediative.eigenflow.publisher.{ MessagingSystem, ProcessPublisher }
import org.scalatest.FreeSpec
import upickle.default._

import scala.collection.mutable
import scala.language.{ implicitConversions, reflectiveCalls }

class ProcessPublisherTest extends FreeSpec {

  class TestContext {
    val processContext = ProcessContext.default(new Date(0))

    val mockMessagingSystem = new MessagingSystem {
      val publishedMessages = mutable.ArrayBuffer[(String, String)]()

      override def publish(topic: String, message: String): Unit = {
        publishedMessages.append(topic -> message)
      }
    }

    val publisher = new ProcessPublisher {
      override def jobId: String = "TestJob"

      override def publisher: MessagingSystem = mockMessagingSystem
    }
  }

  def context(f: (TestContext) => Unit): Unit = {
    f(new TestContext)
  }

  def defaultProcessMessage(context: TestContext): ProcessMessage = {
    ProcessMessage(
      timestamp = 0,
      jobId = context.publisher.jobId,
      processId = context.processContext.processId,
      processingDate = TimeFormat.format(context.processContext.processingDate),
      state = Processing.identifier,
      duration = 0,
      message = ""
    )
  }

  def defaultStageMessage(context: TestContext): StageMessage = {
    StageMessage(
      timestamp = 0,
      jobId = context.publisher.jobId,
      processId = context.processContext.processId,
      stage = context.processContext.stage.identifier,
      state = Processing.identifier,
      duration = 0,
      message = ""
    )
  }

  implicit class ProcessMessageHelper(message: ProcessMessage) {
    def ignoreTimeFields(): ProcessMessage = message.copy(timestamp = 0, duration = 0)
  }
  implicit class StageMessageHelper(message: StageMessage) {
    def ignoreTimeFields(): StageMessage = message.copy(timestamp = 0, duration = 0)
  }
  implicit class MetricsMessageHelper(message: MetricsMessage) {
    def ignoreTimeFields(): MetricsMessage = message.copy(timestamp = 0)
  }

  "publishProcessStarting" - {
    "publish the expected message" in context { context =>
      context.publisher.publishProcessStarting(context.processContext)

      val expectedMessage = defaultProcessMessage(context).copy(
        state = Processing.identifier
      )

      val actualMessages = context.mockMessagingSystem.publishedMessages.map {
        case (key, value) => key -> read[ProcessMessage](value).ignoreTimeFields()
      }

      assert(actualMessages === List("jobs" -> expectedMessage))
    }
  }
  "publishProcessComplete" - {
    "publish the expected message" in context { context =>
      val nextProcessingDate = new Date()
      context.publisher.publishProcessComplete(context.processContext, nextProcessingDate)

      val expectedMessage = defaultProcessMessage(context).copy(
        state = Complete.identifier,
        message = TimeFormat.format(nextProcessingDate)
      )

      val actualMessages = context.mockMessagingSystem.publishedMessages.map {
        case (key, value) => key -> read[ProcessMessage](value).ignoreTimeFields()
      }

      assert(actualMessages === List("jobs" -> expectedMessage))
    }
  }
  "publishProcessFailed" - {
    "publish the expected message" in context { context =>
      val failure = new RuntimeException("Boom")

      context.publisher.publishProcessFailed(context.processContext, failure)

      val expectedMessage = defaultProcessMessage(context).copy(
        state = Failed.identifier,
        message = s"${failure.getClass.getName}: ${failure.getMessage}"
      )

      val actualMessages = context.mockMessagingSystem.publishedMessages.map {
        case (key, value) => key -> read[ProcessMessage](value).ignoreTimeFields()
      }

      assert(actualMessages === List("jobs" -> expectedMessage))
    }
  }
  "publishStageStarting" - {
    "publish the expected message" in context { context =>
      val message = "Some Message"

      context.publisher.publishStageStarting(context.processContext.processId, context.processContext.stage, message)

      val expectedMessage = defaultStageMessage(context).copy(
        state = Processing.identifier,
        message = message
      )

      val actualMessages = context.mockMessagingSystem.publishedMessages.map {
        case (key, value) => key -> read[StageMessage](value).ignoreTimeFields()
      }

      assert(actualMessages === List("stages" -> expectedMessage))
    }
  }
  "publishStageComplete" - {
    "publish the expected message" in context { context =>
      val message = "Some Message"

      context.publisher.publishStageComplete(context.processContext, message)

      val expectedMessage = defaultStageMessage(context).copy(
        state = Complete.identifier,
        message = message
      )

      val actualMessages = context.mockMessagingSystem.publishedMessages.map {
        case (key, value) => key -> read[StageMessage](value).ignoreTimeFields()
      }

      assert(actualMessages === List("stages" -> expectedMessage))
    }
  }
  "publishStageRetrying" - {
    "publish the expected message" in context { context =>
      context.publisher.publishStageRetrying(context.processContext)

      val expectedMessage = defaultStageMessage(context).copy(
        state = Retrying.identifier,
        message = context.processContext.message
      )

      val actualMessages = context.mockMessagingSystem.publishedMessages.map {
        case (key, value) => key -> read[StageMessage](value).ignoreTimeFields()
      }

      assert(actualMessages === List("stages" -> expectedMessage))
    }
  }
  "publishStageFailed" - {
    "publish the expected message" in context { context =>
      val failure = new RuntimeException("Boom")

      context.publisher.publishStageFailed(context.processContext, failure)

      val expectedMessage = defaultStageMessage(context).copy(
        state = Failed.identifier,
        message = s"${failure.getClass.getName}: ${failure.getMessage}"
      )

      val actualMessages = context.mockMessagingSystem.publishedMessages.map {
        case (key, value) => key -> read[StageMessage](value).ignoreTimeFields()
      }

      assert(actualMessages === List("stages" -> expectedMessage))
    }
  }
  "publishMetrics" - {
    "publish the expected message" in context { context =>
      val metricType = "Type"

      val message = Map(
        "k1" -> 1.0d,
        "k2" -> 2.0d,
        "k2" -> 3.0d
      )

      context.publisher.publishMetrics(context.processContext, message)

      val expectedMessage = MetricsMessage(
        timestamp = 0,
        jobId = context.publisher.jobId,
        processId = context.processContext.processId,
        stage = context.processContext.stage.identifier,
        message = message)

      val actualMessages = context.mockMessagingSystem.publishedMessages.map {
        case (key, value) => key -> read[MetricsMessage](value).ignoreTimeFields()
      }

      assert(actualMessages === List("metrics" -> expectedMessage))
    }
  }

}
