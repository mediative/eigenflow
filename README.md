# Eigenflow

Eigenflow is an orchestration platform for building resilient and scalable data pipelines.

Pipelines can be split into multiple process stages which are persisted, resumed and monitored automatically.

[![Build Status](https://travis-ci.org/ypg-data/eigenflow.svg?branch=master)](https://travis-ci.org/ypg-data/eigenflow)
[![Latest Version](https://api.bintray.com/packages/ypg-data/maven/eigenflow/images/download.svg)](https://bintray.com/ypg-data/maven/eigenflow/_latestVersion)

Quick example:

```scala
case object Download extends ProcessStage
case object Transform extends ProcessStage
case object Analyze extends ProcessStage
case object SendReport extends ProcessStage

val download = Download {
  downloadReport() // returns file path/url to downloaded report
} retry (1.minute, 10) // in case of error retry every minute 10 times before failing

val transform = Transform { reportFile =>
  buildParquetFile(reportFile) // returns file path/url
}

val analyze = Analyze { parquetFile =>
  callSparkToAnalyze(parquetFile) // returns new report file path/url
}

val sendReport = SendReport { newReportFile =>
  sendReportToDashboard(newReportFile)
}

override def executionPlan = download ~> transform ~> analyze ~> sendReport
```

Once the stage methods (`downloadReport`, `buildParquetFile` etc in the example above) are defined the rest
is done automatically: see [complete list of features](#main-features).

### What it is good for

`Eigenflow` was created for managing periodic long-running ETL processes with automatic recovery of failures.
When stages performance is important and there is a need to collect statistics and monitor processes.


### What it may not be good for

`Eigenflow` is a platform somewhere between "simple cron jobs" and complex enterprise processes,
where an ESB software would usually be used.
Thus, it probably should not be considered for primitive jobs and very complex processes where SOA is involved.


## Main Features

* Stages: DSL for building type safe data pipeline (Scala only).
* Time Management: configurable strategy for catching up when "run cycles" are missing.
* Recovery: if a process failed the platform starts replaying the failed stage, by default.
* Error Handling: configurable recovery strategies per stage.
* Metrics: Each `process run`, `stage switch or failure` and `custom messages` are published as events to a messaging system.
Kafka is supported by default, but a custom messaging system adapter can be integrated.
* Monitoring: provided as a module, which uses `grafana` and `influxDB` to store and display statistics.
* Notifications: provided as a module, supports `email` and `slack` notifications.

Custom monitoring and notification systems can be developed.
Messages are pushed to a message queue (Kafka is supported out of the box) and can be consumed by a message queue consumer.

Note: there is no connectors to 3rd party systems out of the box.


## System Requirements

### Runtime

* JVM 8

### Development

* Scala 2.11.0 or higher
* Sbt 0.13.7
* DevOps scripts are currently tested on Mac OS 10.10 only

