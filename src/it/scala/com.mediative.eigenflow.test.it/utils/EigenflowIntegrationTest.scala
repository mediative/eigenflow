package com.mediative.eigenflow.test.it.utils

import akka.actor.ActorSystem
import akka.testkit.{ ImplicitSender, TestKit }
import com.mediative.eigenflow.dsl.EigenflowDSL
import org.scalatest.{ BeforeAndAfterAll, Matchers, FreeSpecLike }
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.prop.{ Checkers, GeneratorDrivenPropertyChecks }

class EigenflowIntegrationTest(_system: ActorSystem)
    extends TestKit(_system) with ImplicitSender
    with FreeSpecLike with ScalaFutures with GeneratorDrivenPropertyChecks with Matchers with BeforeAndAfterAll
    with Checkers
    with EigenflowDSL {

  def this() = this(ActorSystem("EigenflowTestActorSystem"))

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

}
