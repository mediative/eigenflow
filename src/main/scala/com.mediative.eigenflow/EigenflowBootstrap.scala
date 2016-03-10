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

import akka.actor.{ Props, ActorSystem }
import akka.event.LoggingAdapter
import com.mediative.eigenflow.environment.ConfigurationLoader
import com.mediative.eigenflow.helpers.DateHelper._
import com.mediative.eigenflow.process.ProcessManager
import com.mediative.eigenflow.process.ProcessManager.Continue
import com.mediative.eigenflow.publisher.MessagingSystem

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{ Failure, Success }

/**
 * Creates the actor system and start processing.
 * Mix it in with App to run the process from the main program.
 */
trait EigenflowBootstrap {
  /**
   * Create the process to run.
   */
  def process: StagedProcess

  // bootstrap the system: initialize akka, message publisher ...
  implicit val messagingSystem = Class.forName(ConfigurationLoader.config.getString("eigenflow.messaging")).getConstructor().newInstance().asInstanceOf[MessagingSystem]

  // load environment variables
  private val startDate = Option(System.getenv("start")).flatMap(parse)

  // initialize actor system as late as possible (fix for Issue #29)
  implicit val system = ActorSystem("DataFlow", ConfigurationLoader.config)

  // create main actor and tell to proceed.
  system.actorOf(Props(new ProcessManager(process, startDate, stopSystem _))) ! Continue

  /**
   * System shutdown logic.
   */
  private def stopSystem(code: Int): Unit = {
    messagingSystem.stop()

    system.terminate().onComplete {
      case Success(_) => System.exit(code)
      case Failure(_) => System.exit(1)
    }
  }
}
