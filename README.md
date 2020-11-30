Cloudstate shopping cart example smoketest/loadtest
===================================================

# Getting started

This project allows you to run a load test against the Cloudstate shopping cart example. A configurable number of users will make 30 requests each to the shopping cart service, adding items, getting the cart then removing items. For the test to pass all the requests must succeed within 3 seconds and none must fail.

# Prerequisites

* Get [Your Akka Serverless Account](https://docs.cloudstate.com/getting-started/lightbend-account.html)
* Install [akkasls](https://docs.cloudstate.com/getting-started/set-up-development-env.html)

# Running the shopping cart smoke test

Using the Docker image (assumes you build the Docker image or use the public one)

Run a smoke test

Be sure to change GATLING_HOST and GATLING_PORT to correctly your service. Some examples:

Targeting a Akka Serverless deployment:

Your service will need to be exposed using the `akkasls svc expose` command. This gives you the hostname. The port will be 443. Additionally, you need to ensure that SSL is enabled (the default is to communicate with the service in plaintext mode).

* GATLING_USE_SSL=true
* GATLING_HOST=yourhostname.us-east1.apps.lbcs.dev
* GATLING_PORT=443

When running against a local service without an SSL gateway, or perhaps using the docker-compose config in the sample-ui-shoppingcart repository, you would configure this way.

* GATLING_USE_SSL=false
* GATLING_HOST=host.docker.internal
* GATLING_PORT=9000

```
docker run --rm --name shoppingcart-load-test \
   -it --volume $PWD/results:/opt/docker/results \
   --env JAVA_OPTS="-Dgatling.charting.noReports=true" \
   --env GATLING_NUM_USERS=1 \
   --env GATLING_NUM_REPS=10 \
   --env GATLING_HOST=host.docker.internal \
   cloudstateio/samples-loadtest-shoppingcart:0.2.0-SNAPSHOT
```

All config options:

* GATLING_HOST is the hostname your shopping cart service is running on
* GATLING_PORT is the port number of your cart service (default is 9000)
* GATLING_USE_SSL you need this if your service is behind an SSL gateway, otherwise the load test will use plaintext
* GATLING_NUM_USERS is the number of users to simulate. They will be simulated in parallel and ramped up over 30 seconds (by default)
* GATLING_NUM_REPS is the number of reps per user to simulate. Each user will add, remove and get the cart contents this number of times
* GATLING_RAMP_UP_SECONDS is the number of seconds over which the users will be added to the load test (important when you have large numbers)

# Running the shopping cart load test and gathering reports

Run the load test as above but you need to also configure the JAVA_OPTS to enable reports. Don't forget to change the hostname example with the hostname you get when you expose your cart service.

```
docker run --rm --name shoppingcart-load-test \
  -it --volume $PWD/results:/opt/docker/results \
  --env JAVA_OPTS="-Dgatling.charting.noReports=false" \
  --env GATLING_NUM_USERS=100 \
  --env GATLING_NUM_REPS=100 \
  --env GATLING_HOST=dark-voice-4467.us-east1.apps.lbcs.dev \
  --env GATLING_PORT=443 \
  --env GATLING_USE_SSL=true \
  cloudstateio/samples-loadtest-shoppingcart:0.2.0-SNAPSHOT
```

# Running with sbt

Using sbt, you can use the same environment variables as above and run like this:

`sbt run`

# Technical notes

This project uses Gatling as a plugin along with grpc to handle the protobuf compilation and gatling-grpc so that we can write Gatling load tests over gRPC instead of HTTP.

Run the shopping cart simulation at the command line and direct to a particular server. Note that if you are running the shopping cart service in Kubernetes cluster, you need to port forward from the akka-sidecar container so you can access it directly with the load test. (The load test uses gRPC not grpc-web protocol).

# Building the Docker image

`sbt docker:publishLocal`

# Pushing the Docker image

Note to push to your own respository set the `dockerUsername` in `build.sbt` to your Docker hub username.

If you want to push to a different repository then add a custom `dockerRepository`.

`sbt docker:publish`

## References

https://gatling.io/docs/current/
https://github.com/gatling/gatling-sbt-plugin-demo
https://github.com/phiSgr/gatling-grpc
https://medium.com/@georgeleung_7777/a-demo-of-gatling-grpc-bc92158ca808
