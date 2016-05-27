package com.mediative.eigenflow.publisher

import com.typesafe.config.{ ConfigValueFactory, ConfigObject, ConfigValue, ConfigFactory }
import org.scalatest.FreeSpec

class MessagingSystemTest extends FreeSpec {
  "create" - {
    "should create the appropriate messaging config" in {
      val config = ConfigFactory.empty().withValue("eigenflow.messaging", ConfigValueFactory.fromAnyRef("com.mediative.eigenflow.publisher.PrintMessagingSystem"))

      val messagingSystem = MessagingSystem.create(config)

      assert(messagingSystem.isInstanceOf[PrintMessagingSystem])
    }
    "should fail if the messaging system does not exist." in {
      val config = ConfigFactory.empty().withValue("eigenflow.messaging", ConfigValueFactory.fromAnyRef("Doesn't Exist"))

      val _ = intercept[ClassNotFoundException] {
        MessagingSystem.create(config)
      }

    }
  }
}
