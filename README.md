# eigenflow

Eigenflow is an orchestration platform which allows to build resilient and scalable data pipelines.
 
## Main Features
* Split a process (data pipeline) into persistent stages.
* Automatically restarts from the last successful stage, thus no need to re-run from the beginning in case of a failure.
* Configurable recovery strategies per stage and types of failures.
* Configurable strategy for catching up when "run cycles" are missing.
* Monitoring and Notification systems using Grafana and Slack. 
* Easy integration: Eigenflow publishes events for every process stage (uses kafka by default), just consume those events.
* Well integrated with Mesos (see DevOps)
* Elegant DSL.

## Getting Started

### SBT

project/plugins.sbt 

```
resolvers += Resolver.url("YPG-Data SBT Plugins", url("https://dl.bintray.com/ypg-data/sbt-plugins"))(Resolver.ivyStylePatterns)

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.1")
addSbtPlugin("com.mediative.sbt" % "sbt-mediative-core" % "0.1.1")
addSbtPlugin("com.mediative.sbt" % "sbt-mediative-oss" % "0.1.1")
```

build.sbt

  ```
  resolvers += Resolver.bintrayRepo("ypg-data", "maven")
  resolvers += Resolver.bintrayRepo("krasserm", "maven")

  libraryDependencies ++= Seq(
    "com.mediative" %% "eigenflow" % "0.1.0"
  )

  ```

### Create Simple Process

Define custom process stages:

  ```
  import com.mediative.eigenflow.domain.fsm.ProcessStage

  case object Extract extends ProcessStage
  case object Transform extends ProcessStage
  case object Load extends ProcessStage
  ```

all stages extend `ProcessStage` trait.

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
 
Create main class

  ```
  object SimpleProcessApp extends App with EigenflowBootstrap {
    override def process: StagedProcess = new SimpleProcess
  }
  ```
 
Create application configuration. 
resources/application.conf

  ```
  process {
    id = "simpleProcess"
  }
  ```
  
Run it

  ```
  $ sbt run
  ```


Note: by default it uses akka inmem storage, what means it won't restore after a failure. 
Define akka.persistence in application.conf to use a real storage (e.g.: leveldb) to test recovery. 

### Resiliency

If a stage fails due to an issue which may be solved by simply keep retrying a retry strategy can be defined.
The simplest strategy could be something like: no matter what happens retry 3 times every 10 seconds.

  ```
  import scala.concurrent.duration._
  
  val transform = Transform { input =>
    ...
  } retry (10.seconds, 3)
  ```
  
A more sophisticated strategy allows to specify number of retries per exception:
  
  ```
  import scala.concurrent.duration._
    
  val transform = Transform { input =>
    ...
  } retry {
    case IOException => Retry(10.seconds, 3)
    case ConnectionTimeout => Retry(1.minute, 5)
  }
  ```
  
If no retry strategy is defined for an exception or the stage constantly fails (after all number of attempts are made) the process 
will be terminated. The traces of failure can be found in both process log and published messages.

If a process failed simply restart it when issue is solved and it will re-start from the failed stage.
See the "Recovery" topic below.

### Recovery

Note: to test recovery you must define `akka.persistence` in `application.conf`. see: http://doc.akka.io/docs/akka/snapshot/scala/persistence.html

Recovery allows to re-start the process from the failed stage.
If a stage failure happened during the process execution, Eigenflow will keep the information of the failed stage, and will try to 
re-run it when the next time started.
 
If a hardware failure happened or process was killed then it will either start the next stage after the last successful 
(according to the execution plan) or will keep re-trying, depending on what was happening at the moment when it was stopped.

Obviously that the storage must be configured the way that it can survive a hardware failure (use a multi-node cassandra, for example).
 
See our "Mesos" configuration to increase the chances of hardware failures.

### Process Monitoring



### Time Control for Periodic Processes

### Elegant DSL

### Mesos
