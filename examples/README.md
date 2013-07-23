# mod-lang-clojure Examples


1) The bin directory from the vertx distro must be on your PATH - this
   should have been done as part of the install procedure.

2) JDK/JRE 1.7.0+ must be installed and the JDK/JRE bin directory must
   be on your PATH

3) Until a mod-lang-clojure artifact is published, you'll need to build
   and install it locally to run the examples.

*all examples should be run from this directory unless otherwise stated*

    vertx run <example script name>

where <example script name> is, for example, echo/echo-server.clj

## Install

Install with:

    mvn install

Update `langs.properties` in your VERTX_HOME/conf with:

    clojure=io.vertx~lang-clojure~1.0.0-SNAPSHOT:io.vertx.lang.clojure.ClojureVerticleFactory
    .clj=clojure


## Examples

A simple echo server which echos back any sent to it

To run the server:

    vertx run echo/echo-server.clj

Then either telnet localhost 1234 and notice how text entered via telnet is echoed back

Instead of telnet you can also run a simple echo client in a different console:

    vertx run echo/echo-client.clj

## EventBus Point to Point

A simple point to point event bus example.

receiver.clj registers an event bus handler that displays a message
when a message is received and replies.

sender.clj sends a message every second, and prints any replies it
receives.

    vertx run eventbus_pointtopoint/receiver.clj -cluster

And in a different console:

    vertx run eventbus_pointtopoint/sender.clj -cluster


