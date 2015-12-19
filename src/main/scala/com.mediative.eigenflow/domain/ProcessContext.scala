package com.mediative.eigenflow.domain

import java.util.Date

import com.mediative.eigenflow.domain.fsm.ProcessStage

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