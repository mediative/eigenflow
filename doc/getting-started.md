# Getting Started

For setting up an environment on Mac OS follow the instructions for the
[DevOps script](../devops/README.md).

## SBT

Add a library dependency on Eigenflow in `build.sbt`

```scala
libraryDependencies += "com.mediative" %% "eigenflow" % "x.x.x"
```

## Create Process

Define custom process stages:

```scala
import com.mediative.eigenflow.domain.fsm.ProcessStage

case object Extract extends ProcessStage
case object Transform extends ProcessStage
case object Load extends ProcessStage
```

all stages must extend `ProcessStage` trait.

Create a process class:

```scala
class SimpleProcess extends StagedProcess {
}
```

`StagedProcess` implementations must implement the `executionPlan` method, which describes the pipeline stages flow.
Describe it using the DSL:

```scala
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

See [the Process Stages section](process-stages.md) for important details.

## Main Class

```scala
object SimpleProcessApp extends App with EigenflowBootstrap {
  override def process: StagedProcess = new SimpleProcess
}
```

## Basic Configuration

Create `resources/application.conf` file with the content:

```json
process {
  id = "simpleProcess"
}
```

Run it

```
sbt run
```


Note: by default it uses Akka inmem storage and PrintMessagingSystem (which simply prints messages to logs), what means it won't restore after a failure.

To configure storage and messaging system use `application.conf`

Example of storage configuration for a local cassandra installation

```json
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

Example of messaging system configuration for a local Kafka installation

```json
eigenflow {
  messaging = "com.mediative.eigenflow.publisher.kafka.KafkaConfiguration"

  kafka {
    bootstrap.servers = "localhost:9092"
  }
}
```
