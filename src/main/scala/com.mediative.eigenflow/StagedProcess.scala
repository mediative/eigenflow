/*
 * Copyright 2015 Mediative
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
