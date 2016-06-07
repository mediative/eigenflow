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

package com.mediative.eigenflow

import akka.actor.ActorSystem
import com.mediative.eigenflow.environment.ConfigurationLoader
import com.mediative.eigenflow.helpers.DateHelper._
import com.mediative.eigenflow.process.ProcessManager.Continue
import com.mediative.eigenflow.process.ProcessSupervisor
import com.mediative.eigenflow.publisher.MessagingSystem
import com.typesafe.config.Config

import scala.concurrent._
import scala.concurrent.duration._
import scala.util.Try

/**
 * Creates the actor system and start processing.
 * Mix it in with App to run the process from the main program.
 */
trait EigenflowBootstrap {
  /**
   * Create the process to run.
   */
  def process: StagedProcess

  private val config: Config = loadConfig()
  protected def loadConfig(): Config = ConfigurationLoader.config

  // bootstrap the system: initialize akka, message publisher ...
  implicit val messagingSystem = MessagingSystem.create(config)

  // load environment variables
  private val startDate = Option(System.getenv("start")).flatMap(parse)

  // initialize actor system as late as possible (fix for Issue #29)
  implicit val system = ActorSystem("DataFlow", config)

  private val processTypeId = config.getString("process.id")

  // create main actor and tell to proceed.
  system.actorOf(ProcessSupervisor.props(process, startDate, processTypeId, stopSystem(system))) ! Continue

  /**
   * System shutdown logic.
   */
  private def stopSystem(system: ActorSystem)(code: Int): Unit = {
    Try(messagingSystem.stop())

    // in case if termination process hangs
    try {
      val terminationProcess = system.terminate()

      Await.result(terminationProcess, 10.seconds)
      System.exit(code)
    } catch {
      case t: Throwable =>
        t.printStackTrace()
        System.err.println("Timeout actor system termination. Forcing system exit.")
        Runtime.getRuntime.halt(1) // force quit with status code 1
    }
  }
}
