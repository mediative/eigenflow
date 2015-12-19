package com.mediative.eigenflow.environment

case class ProcessConfiguration(id: String, dataPath: String)

object ProcessConfiguration {
  lazy val load = {
    val id = ConfigurationLoader.config.getString("process.id")
    ProcessConfiguration(
      id = id,
      dataPath = Option(System.getenv("data")).getOrElse(s"/mnt/eigenflow/process/$id/data")
    )
  }
}
