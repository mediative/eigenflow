# DevOps

For a quick start on Mac OS use the `devops/mac/eigenflow` script:

Check docker installation and setup port forwarding

    $ ./eigenflow setup

Start containers

    $ ./eigenflow start

Note: The state will be reset on every start (empty database and message queues).


Check the docker container status

    $ ./eigenflow state


Print `Eigenflow` configuration suggestion

    $ ./eigenflow config

Stop containers

    $ ./eigenflow stop
