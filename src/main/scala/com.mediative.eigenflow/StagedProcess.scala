package com.mediative.eigenflow

import java.util.Date

import com.mediative.eigenflow.domain.fsm.ExecutionPlan
import com.mediative.eigenflow.dsl.EigenFlowDSL

/**
 * The main trait which allows to define the execution stages.
 *
 */
trait StagedProcess extends EigenFlowDSL {
  def executionPlan: ExecutionPlan[_, _]

  def initialProcessingDate: Date = new Date()

  def nextProcessingDate(lastCompleted: Date): Date
}
