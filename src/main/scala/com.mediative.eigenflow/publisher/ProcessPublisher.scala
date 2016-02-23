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

package com.mediative.eigenflow.publisher

import java.util.Date

import com.mediative.eigenflow.domain.ProcessContext
import com.mediative.eigenflow.domain.fsm.ProcessStage
import com.mediative.eigenflow.domain.messages._
import com.mediative.eigenflow.helpers.DateHelper._
import upickle.default._

import scala.language.implicitConversions

trait ProcessPublisher {
  def publisher: MessagingSystem

  def jobId: String

  // this method is used to forbid publishing during stage recovery to avoid publishing of duplicates or false events.
  def publishingActive: Boolean = true

  // high level helper functions
  def publishProcessStarting(context: ProcessContext): Unit = {
    publishProcessMessage(ProcessMessage(
      timestamp = System.currentTimeMillis(),
      jobId = jobId,
      processId = context.processId,
      processingDate = context.processingDate,
      state = Processing,
      duration = 0,
      message = ""
    ))
  }

  def publishProcessComplete(context: ProcessContext,
    nextProcessingDate: Date): Unit = {
    val now = System.currentTimeMillis()
    publishProcessMessage(ProcessMessage(
      timestamp = now,
      jobId = jobId,
      processId = context.processId,
      processingDate = context.processingDate,
      state = Complete,
      duration = now - context.startTime,
      message = nextProcessingDate
    ))
  }

  def publishProcessFailed(context: ProcessContext, failure: Throwable): Unit = {
    publishProcessMessage(ProcessMessage(
      timestamp = System.currentTimeMillis(),
      jobId = jobId,
      processId = context.processId,
      processingDate = context.processingDate,
      state = Failed,
      duration = 0,
      message = failure
    ))
  }

  def publishStageStarting(processId: String, stageWillStart: ProcessStage, message: String): Unit = {
    publishStageMessage(StageMessage(
      timestamp = System.currentTimeMillis(),
      jobId = jobId,
      processId = processId,
      stage = stageWillStart,
      state = Processing,
      duration = 0,
      message = message
    ))
  }

  def publishStageComplete(context: ProcessContext, message: String): Unit = {
    val now = System.currentTimeMillis()
    publishStageMessage(StageMessage(
      timestamp = now,
      jobId = jobId,
      processId = context.processId,
      stage = context.stage,
      state = Complete,
      duration = now - context.startTime,
      message = message
    ))
  }

  def publishStageRetrying(context: ProcessContext): Unit = {
    publishStageMessage(StageMessage(
      timestamp = System.currentTimeMillis(),
      jobId = jobId,
      processId = context.processId,
      stage = context.stage,
      state = Retrying,
      duration = 0,
      message = ""
    ))
  }

  def publishStageFailed(context: ProcessContext, failure: Throwable): Unit = {
    publishStageMessage(StageMessage(
      timestamp = System.currentTimeMillis(),
      jobId = jobId,
      processId = context.processId,
      stage = context.stage,
      state = Failed,
      duration = 0,
      message = failure
    ))
  }

  def publishMetrics(context: ProcessContext, message: Map[String, Double]): Unit = {
    publishMetricsMessage(MetricsMessage(
      timestamp = System.currentTimeMillis(),
      jobId = jobId,
      processId = context.processId,
      stage = context.stage,
      message = message
    ))
  }

  // low level helper functions
  private def publishProcessMessage(message: ProcessMessage): Unit = {
    if (publishingActive) {
      publisher.publish("jobs", message)
    }
  }

  private def publishStageMessage(message: StageMessage): Unit = {
    if (publishingActive) {
      publisher.publish("stages", message)
    }
  }

  private def publishMetricsMessage(message: MetricsMessage): Unit = {
    if (publishingActive) {
      publisher.publish("metrics", message)
    }
  }

  // implicits
  private implicit def processMessageToString(message: ProcessMessage): String = write[ProcessMessage](message)

  private implicit def stageMessageToString(message: StageMessage): String = write[StageMessage](message)

  private implicit def metricsMessageToString(message: MetricsMessage): String = write[MetricsMessage](message)

  implicit def dateToString(date: Date): String = TimeFormat.format(date)

  implicit def stateRecordToString(state: StateRecord): String = state.identifier

  implicit def stageToString(stage: ProcessStage): String = stage.identifier

  implicit def throwableToString(t: Throwable): String = s"${t.getClass.getName}: ${t.getMessage}"
}
