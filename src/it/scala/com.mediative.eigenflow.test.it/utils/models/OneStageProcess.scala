package com.mediative.eigenflow.test.it.utils.models

import java.util.Date

import com.mediative.eigenflow.StagedProcess
import com.mediative.eigenflow.domain.fsm.ExecutionPlan
import com.mediative.eigenflow.test.Stage1

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object OneStageProcess {
  def create(f: Date => Date): StagedProcess = new OneStageProcess {
    override def nextProcessingDate(lastCompleted: Date): Date = f(lastCompleted)
  }
}

trait OneStageProcess extends StagedProcess {
  val a = Stage1 {
    Future { "" }
  }

  override def executionPlan: ExecutionPlan[_, _] = a
}