# Time Management

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
