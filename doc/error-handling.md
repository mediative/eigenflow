# Error Handling

By default, if no strategy is defined, when an exception happens the process switches to the `failed` stage remembering
the stage it was trying to execute and exits.

The simplest possible strategy would be: no matter what happens retry _n_-times with given interval:

```scala
SomeStage {
  ...
} retry (1.minute, 10)
```

You can define multiple strategies depending on exception type:

```scala
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
