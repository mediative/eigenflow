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
package dsl

import java.io.{ FileNotFoundException, IOException }

import com.mediative.eigenflow.domain.RecoveryStrategy.{ Fail, Retry }
import com.mediative.eigenflow.dsl.EigenflowDSL
import org.scalatest.FreeSpec
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.prop.GeneratorDrivenPropertyChecks

import scala.concurrent.duration._

class EigenflowDSLTest extends FreeSpec with ScalaFutures with EigenflowDSL with GeneratorDrivenPropertyChecks {
  "Eigenflow DSL" - {
    "stage { logic }" - {
      "must generate correct ExecutionPlan" in {

        val value = randomString

        val executionPlan = Stage1 {
          toFuture(value)
        }

        whenReady(executionPlan.f(defaultProcessContext)(())) { s =>
          assert(s == value)
        }

        assert(executionPlan.stage == Stage1)
        assert(executionPlan.previous.isEmpty)
      }
    }

    "a ~> b" - {
      "must link both ExecutionPlan's" in {
        val a = Stage2 {
          stringFunction
        }

        val b = Stage3 { _: String =>
          stringFunction
        }

        val executionPlan = a ~> b

        assert(executionPlan.stage == Stage3)
        assert(!executionPlan.previous.isEmpty && executionPlan.previous.get.stage == Stage2)
      }
    }

    "stage { logic } retry(n, m)" - {
      "must register retry strategy in the ExecutionPlan" in {
        forAll { (interval: Int, attempts: Int) =>
          val executionPlan = Stage1 {
            stringFunction
          } retry (interval.seconds, attempts)

          val strategy = executionPlan.recoveryStrategy(new RuntimeException)
          val retry = strategy.asInstanceOf[Retry]

          assert(retry.interval.toSeconds == interval)
          assert(retry.attempts == attempts)
        }
      }
    }

    "stage { logic } onFailure { case ... => ... }" - {
      "must register retry strategy per exception in the ExecutionPlan" in {
        val fileNotFoundAttempts = 3
        val ioAttempts = 4

        val executionPlan = Stage1 {
          stringFunction
        } onFailure {
          case _: FileNotFoundException => Retry(3.seconds, fileNotFoundAttempts)
          case _: IOException => Retry(3.seconds, ioAttempts)
        }

        val fileNotFoundExceptionStrategy = executionPlan.recoveryStrategy(new FileNotFoundException())
        val ioExceptionStrategy = executionPlan.recoveryStrategy(new IOException())
        val unexpectedExceptionStrategy = executionPlan.recoveryStrategy(new RuntimeException())

        assert(fileNotFoundExceptionStrategy.isInstanceOf[Retry])
        assert(ioExceptionStrategy.isInstanceOf[Retry])

        val fileNotFoundRetry = fileNotFoundExceptionStrategy.asInstanceOf[Retry]
        assert(fileNotFoundRetry.attempts == fileNotFoundAttempts)

        val ioRetry = ioExceptionStrategy.asInstanceOf[Retry]
        assert(ioRetry.attempts == ioAttempts)

        unexpectedExceptionStrategy match {
          case Fail => assert(true)
          case _ => assert(false, "Wrong RecoveryStrategy type assigned by default")
        }
      }
    }
  }

}
