# eigenflow

Eigenflow is an orchestration platform which allows to build resilient and scalable data pipelines.

Quick example:

```
case object Download extends ProcessStage
case object Transform extends ProcessStage
case object Analyze extends ProcessStage
case object SendReport extends ProcessStage

val download = Download {
  downloadReport() // returns file path/url to downloaded report
} retry (1.minute, 10) // in case of error retry every minute 10 times before failing

val transform = Transform { reportFile =>
  buildParquet(reportFile) // returns file path/url
}

val analyze = Analyze { parquetFile =>
  callSparkToAnalyze(parquetFile) // returns new report file path/url
}

val sendReport = SendReport { newReportFile =>
  sendReport(newReportFile)
}

override def executionPlan = download ~> transform ~> analyze
```

You just need to define the logic of stage methods (`downloadReport`, `buildParquet` etc in the example above) the rest
will be done automatically, namely:

* The system will automatically do "checkpoints" on every stage switch, thus in case of failure it restarts by running the
failed stage again.
* It can automatically catch-up by processing missing days, and many more - see [complete list of features](#main-features).

## Main Features

* Stages: the platform encourage developers to split processes in stages,
where the result of a stage processing will be an input of the next stage.
* Time Management: configurable strategy for catching up when "run cycles" are missing.
* Recovery: if a process failed the platform starts replaying the failed stage, by default.
* Error Handling: configurable recovery strategies per stage.
* Metrics: Each `process run`, `stage switch or failure` and `custom messages` are published as events to a messaging system.
Kafka is supported by default, but a custom messaging system adapter can be easily integrated.
* Monitoring: A module which exports metrics to InfluxDB based on metrics events is provided independently.
A custom monitoring system can be developed based on metrics messages published by `Eigenflow`.
Internally we use `grafana` and `influxDB` to monitor processes.
* Notifications: A module which sends notifications to an `email` and `slack` is also provided independently.
A custom monitoring system can be developed based on metrics messages published by `Eigenflow`.
* DSL: an elegant DSL is provided to describe `stages`, `stage transitions`, `retry strategies` and `custom events publishing`.

## Getting Started

#### SBT

project/plugins.sbt

```javascript
resolvers += Resolver.url("YPG-Data SBT Plugins", url("https://dl.bintray.com/ypg-data/sbt-plugins"))(Resolver.ivyStylePatterns)

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.1")
addSbtPlugin("com.mediative.sbt" % "sbt-mediative-core" % "0.1.1")
addSbtPlugin("com.mediative.sbt" % "sbt-mediative-oss" % "0.1.1")
```

build.sbt

```javascript
resolvers += Resolver.bintrayRepo("ypg-data", "maven")
resolvers += Resolver.bintrayRepo("krasserm", "maven")

libraryDependencies ++= Seq(
  "com.mediative" %% "eigenflow" % "0.1.0"
)
```

#### Create Process

Define custom process stages:

```
import com.mediative.eigenflow.domain.fsm.ProcessStage

case object Extract extends ProcessStage
case object Transform extends ProcessStage
case object Load extends ProcessStage
```

all stages must extend `ProcessStage` trait.

Create a process class:

```
class SimpleProcess extends StagedProcess {
}
```

The `StagedProcess` will require to define `executionPlan` method, this method describes the pipeline stages flow.
Describe it using DSL:

```
val extract = Extract {
  ...
}

val transform = Transform { resultFromExtract =>
  ...
}

val load = Load { resultFromTransform =>
  ...
}

override def executionPlan: ExecutionPlan[_, _] = extract ~> transform ~> load

```

Please see [Stages](#stages) section for important details.

#### Main Class

```
object SimpleProcessApp extends App with EigenflowBootstrap {
  override def process: StagedProcess = new SimpleProcess
}
```

#### Basic Configuration

Create `resources/application.conf` file with the content:

```javascript
process {
  id = "simpleProcess"
}
```

Run it

```
sbt run
```


Note: by default it uses akka inmem storage and PrintMessagingSystem (which simply prints messages to logs), what means it won't restore after a failure.

To configure storage and messaging system use `application.conf`

Example of storage configuration for a local cassandra installation

```
akka {
  persistence {
    journal {
      plugin = "cassandra-journal"
    }
  }
}

cassandra-journal {
  contact-points = ["localhost:9042"]
}
```

Example of messaging system configuration for a local kafka installation

```
eigenflow {
  messaging = "com.mediative.eigenflow.publisher.kafka.KafkaConfiguration"

  kafka {
    bootstrap.servers = "localhost:9092"
  }
}
```

## Stages

Eigenflow requires at least one stage to run the process.
To define process stages create case objects which extend `ProcessStage` trait. For example:

```
case object StageName extends ProcessStage
```

The stages are mainly used as checkpoints during the process.
`Eigenflow` persists each stage completion and the result of that stage.
The result of stage execution must be returned in a `future`, to define stage execution logic use DSL:

```
StageName {
  process()
}
```

the `process` function in this example must return a `Future[A]`, then the next stage will receive A as argument.
The `A` can be anything but serialization/deserialization implicit functions must be available:

```
A => String
String => A
```

`Eigenflow` provides the `PrimitiveImplicits` object which can be imported to enable some primitives serialization.

If you need access to process context, for example to calculate for which date to fetch the report you can use `withContext` method.
Here is an example of downloading and archiving a report.

```
case object Initial extends ProcessStage
case object Download extends ProcessStage
case object Archive extends ProcessStage

val init = Initial withContext { context: ProcessContext =>
  Future {
    context.processingDate.minusDays(1) // means always download report from yesterday (relating to the current processingDate)
  }
}

val download = Download { reportDate =>
  downloadReportForDate(reportDate) // returns Future[String] which contains file name
}

val archive = Archive { fileName =>
  archive(fileName)
}

 override def executionPlan = init ~> download ~> archive
```

**IMPORTANT**
- Design stages like a reboot may happen between stages execution, thus NEVER share a state
between stages. Only, the standard way, when the result returned from one stage and passed to another stage is safe,
this result must contain all information shared between stages.
- Stages must run sequentially, thus the `Future` returned from stage must be the final
 one. If other futures are created during stage execution you should combine them all using `Future.sequence`.
- It's NOT recommended to pass big data between stages, store data in a storage (file, db, hadoop etc.)
and pass the information how to find it.

### Time Management

If a process must run once an hour, day, week etc. you can have an additional control which allows automatically

* Catching up, for example if a process should run daily but the last successful run was 3 days ago it will automatically
run for every missing day until now.
* Protects from double run, if a process should run daily and it **successfully** finished execution today it won't run for today
again even if it was executed again, unless it was explicitly asked to do so.
Thus you shouldn't worry about occasional loading of the same data twice.
Note: to have a guaranteed double run protection, the environment must satisfy 2 conditions:
    1. Persistence layer must not be eventual consistent. We use cassandra configured for consistency.
    1. No simultaneous runs of the same process. We use mesos with chronos to schedule processes in cluster.

Override `nextProcessingDate(lastCompleted: Date): Date` method to define next run date should be, based on the last completed date.

TODO: examples

### Recovery

`eigenflow` remembers the last stage and the processing date.
Thus if a process fails or crashes, it will re-run the failed stage automatically when restarted.
When processing is complete for a date the `nextProcessingDate` function will be called to define if it should "catch-up".
If the `nextProcessingDate` returns a date in the past the process continues to run with the new date until the `nextProcessingDate`
returns a date in future.

Think of it as a time line with repeating stages and the system always tries to execute all stages up to now,
if a stage cannot be complete, it stuck processing in a `stage-time` point.

To control the time function see: [Time Management](#time-management)

### Error Handling

By default, if no strategy is defined, when an exception happens the process switches to the `failed` stage remembering
the stage it was trying to execute and exists.

The simplest possible strategy would be: no matter what happens retry n-times with given interval:

```
SomeStage {
  ...
} retry (1.minute, 10)
```

You can define multiple strategies depending on exception type:

```
SomeStage {
  ...
} retry {
  case _: ConnectionTimeoutException => Retry(3.minutes, 10)
  case _: IOException => Retry(10.minutes, 3)
} retriesTimeout(45.minutes)
```

`retriesTimeout` helps to limit the total number of time may potentially be spent on retries.
In the example above the potential maximum time the stage can spend retrying is 1 hour
(30 minutes for ConnectionTimeout and 30 minutes for IOException),
but we can say maximum 30 minutes per each exception but if it takes more than 45 minutes in retries then just fail.

`retriesTimeout` is optional, no limit by default.
Thus the time spent on retries will be limited only by a sum of all `Retry` settings + time spent on actual executions.

**IMPORTANT**: `retriesTimeout` applies only when stage actually retrying, it has no effect on normal stage execution!
  If execution takes 5 hours and it does not throw any exceptions nothing will interrupt it!

If an exception which is not defined occurs it will apply default behaviour (fail the process)

For better understanding how it works let's see different scenarios which may happen:

1. If multiple strategies are defined and the exceptions happen in a random order the system will keep counting
 number of retries per exception.
For the example (for the code above) if exceptions come in the order:
`ConnectionTimeoutException - IOException - IOException - ConnectionTimeoutException` the retries counter for
`ConnectionTimeout` will not be reset when `IOException` happens.
1. If a system reboot/failure happens in a middle of retries the system will restore counters and retries timeout on start.
Be aware: if `retriesTimeout` is specified and the system was down for a period of time longer that the time left for retries
it will fail on start right away, must be restarted again to start over, see point below.
1. If number of retries exhausted and process exits with failed stage, then the next time it starts, it re-runs the failed stage
  with all counters reset to 0.
