# Process Stages

Eigenflow requires at least one stage to run the process.
To define process stages create case objects which extend `ProcessStage` trait. For example:

```scala
case object StageName extends ProcessStage
```

The stages are mainly used as checkpoints during the process.
`Eigenflow` persists each stage completion and the result of that stage.
The result of stage execution must be returned in a `future`, to define stage execution logic use DSL:

```scala
StageName {
  process()
}
```

the `process` function in this example must return a `Future[A]`, then the next stage will receive A as argument.
The `A` can be anything but serialization/deserialization implicit functions must be available:

```scala
A => String
String => A
```

`Eigenflow` provides the `PrimitiveImplicits` object which can be imported to enable some primitives serialization.

If you need access to process context, for example to calculate for which date to fetch the report you can use `withContext` method.
Here is an example of downloading and archiving a report.

```scala
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
