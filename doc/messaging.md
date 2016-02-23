# Messaging

<!-- toc -->

## Standard Messages

`Eigenflow` publishes messages for each run start/failure/complete and stage switches.

(Subject to change) Currently each process has it's own set of topics:

- `process_id` for messages on process level: run start/failure/complete events
- `process_id-stages` for messages on a process run level: stage switches
- `process_id-<custom>` for metrics messages, see [Custom Messages](#custom-messages)

Published messages are json serialized instances of classes defined in [messages package](https://github.com/ypg-data/eigenflow/tree/master/src/main/scala/com.mediative.eigenflow/domain/messages).

## Custom Messages

There are 2 types of custom messages currently supported:

### Metrics messages

Metrics messages must be added to each stage and provide `StageResult => Map[String, Double]` function to generate metrics. 
For example:

```scala
val stage = Stage {
  Future { ... }
} publishMetrics(_ => Map("total" -> 1.0))
```

In this case a message with process, stage and metrics information will be published to `process_id-statistics` topic.

### Direct messaging

To get access to messaging system define implicit of `MessagingSystem` in process class constructor, for example:

```scala
class MyProcess(implicit ms: MessagingSystem) extends StagedProcess
```

now a message can be published using `publish` function:

```
ms.publish(topicName, message)
```

The `message` parameter can be any string.

Suggestion: [GenericMessage](https://github.com/ypg-data/eigenflow/blob/master/src/main/scala/com.mediative.eigenflow/domain/messages/GenericMessage.scala)
can be used to add basic process information fields (not data!) along with the custom message, but it's optional.

## Configuration

By default `Eigenflow` uses `PrintMessagingSystem` which simply logs messages using the standard logger.

To enable kafka the following settings must be set in `application.conf`

```
eigenflow {
  messaging = "com.mediative.eigenflow.publisher.kafka.KafkaMessagingSystem"

  kafka {
    bootstrap.servers = "..."
    topic.prefix = "..." // Optional. The Default value is "eigenflow".
  }
}
```
