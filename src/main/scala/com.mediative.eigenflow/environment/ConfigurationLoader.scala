package com.mediative.eigenflow.environment

import com.typesafe.config.ConfigFactory

object ConfigurationLoader {
  lazy val config = Option(System.getenv("config")).fold(ConfigFactory.load())(filePath => ConfigFactory.load(filePath))
}
