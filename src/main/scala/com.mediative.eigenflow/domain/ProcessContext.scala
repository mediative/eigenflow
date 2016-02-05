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

package com.mediative.eigenflow.domain

import java.util.{ UUID, Date }

import com.mediative.eigenflow.domain.fsm.{ Initial, ProcessStage }

/**
 * A set of basic attributes which help to maintain stage transitions logic
 * and carry over data between stages within one process.
 *
 * @param timestamp Timestamp of the last stage transition (except Retrying stage!).
 * @param processingDate Current processing date.
 *
 *                      The processingDate is important for recovery process, and for ability to catch up.
 *
 *                      for example:
 *                      A process should run every day, but the last run was 3 days ago.
 *                      If the 'nextProcessingDate' is incrementing by 1 day, the system will automatically process
 *                      all reports day be day until now.
 *
 *                      Note: the dates used for processing are not necessarily the same as the processingDate.
 *                      For example: A report's date could be "processingDate - 1 day".
 *
 * @param processId Process identification.
 * @param startTime Time when the process started.
 * @param stage The current stage.
 * @param failure Last failure of the current stage, if any.
 * @param retryRegistry Retry registry keeps records of retry logic for the current stage.
 * @param message Serialized data from the previous stage.
 *
 */
case class ProcessContext(timestamp: Long = System.currentTimeMillis(),
  processingDate: Date,
  processId: String,
  startTime: Long,
  stage: ProcessStage,
  failure: Option[Throwable] = None,
  retryRegistry: Option[RetryRegistry] = None,
  message: String)

object ProcessContext {
  def default(processingDate: Date) = ProcessContext(
    timestamp = System.currentTimeMillis(),
    processingDate = processingDate,
    processId = UUID.randomUUID().toString,
    startTime = System.currentTimeMillis(),
    stage = Initial,
    message = ""
  )
}
