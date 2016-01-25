# Recovery

`Eigenflow` remembers the last stage and the processing date.
Thus if a process fails or crashes, it will re-run the failed stage automatically when restarted.
When processing is complete for a date the `nextProcessingDate` function will be called to define if it should "catch-up".
If the `nextProcessingDate` returns a date in the past the process continues to run with the new date until the `nextProcessingDate`
returns a date in future.

Think of it as a time line with repeating stages and the system always tries to execute all stages up to now,
if a stage cannot be complete, it stuck processing in a `stage-time` point.

To control the time function see: [Time Management](time-management.md)
