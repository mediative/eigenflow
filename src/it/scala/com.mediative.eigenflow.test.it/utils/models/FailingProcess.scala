package com.mediative.eigenflow.test.it.utils.models

import java.util.Date

import com.mediative.eigenflow.StagedProcess
import com.mediative.eigenflow.domain.fsm.ExecutionPlan
import com.mediative.eigenflow.test.Stage1

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object FailingProcess {
  def create(f: Date => Date): FailingProcess = new FailingProcess {
    override def nextProcessingDate(lastCompleted: Date): Date = f(lastCompleted)
  }
}

trait FailingProcess extends StagedProcess {
  val a = Stage1 {
    Future[String] {
      throw new RuntimeException
    }
  }

  override def executionPlan: ExecutionPlan[_, _] = a
}
