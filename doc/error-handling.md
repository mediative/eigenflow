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
} onFailure {
  case _: ConnectionTimeoutException => Retry(3.minutes, 10)
  case _: IOException => Retry(10.minutes, 3)
} globalRecoveryTimeout(45.minutes)
```

`globalRecoveryTimeout` helps to limit the deadline on all retries.
In the example above the potential maximum time the stage will be retrying is 1 hour
(30 minutes for ConnectionTimeout and 30 minutes for IOException),
 but the `globalRecoveryTimeout` will limit it to ~45 minutes.
Note: if a stage started execution, it won't be interrupted even if the timeout reached!

`globalRecoveryTimeout` is optional, no limit by default.
Thus the time spent on retries will be limited only by a sum of all `Retry` settings + time spent on actual executions.

**IMPORTANT**: `globalRecoveryTimeout` applies only when stage is actually retrying, it has no effect on normal stage execution!
  If execution takes 5 hours and it does not throw any exceptions nothing will interrupt it!

The uncovered exceptions will fail the process (default behaviour when no recovery strategy defined).

If system crashed during stage retry it will restore all counters and timeout points.

If system "normally" fails due to timeout or "tried max number of attempts" then all counters and timeouts will be reset on restart.

Example of strategy recovery in different scenarios:

1. If multiple strategies are defined and the exceptions happen in a random order the system will keep counting
 number of retries per exception.
For the example (for the code above) if exceptions happen in the following order:
`ConnectionTimeoutException - IOException - IOException - ConnectionTimeoutException` the retries counter for
`ConnectionTimeout` will not be reset when `IOException` is thrown.
1. If a system reboot/failure happens during stage retries, the system will restore counters and retries timeout on start.
Note: if `globalRecoveryTimeout` is defined and the system was re-started after some delay it may be stopped due to timeout reached.
But the next restart will reset all counters.
1. If number of retries exhausted or timeout reached the process fails. The new restart will reset all counters and timeout.
