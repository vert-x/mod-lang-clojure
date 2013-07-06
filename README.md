# mod-lang-clojure

THIS IS A WORK IN PROGRESS, AND FAR FROM COMPLETE.

A vert.x language implementation in Clojure

Build with:

    mvn install

Update `langs.properties` in your VERTX_HOME/conf with:

    clojure=io.vertx~lang-clojure~1.0.0-SNAPSHOT:io.vertx.lang.clojure.ClojureVerticleFactory
    .clj=clojure


## Examples

Run the echo server:

    vertx run examples/echo/echo-server.clj

and, in another shell, run the echo client:

    vertx run examples/echo/echo-client.clj

For the eventbus example:

    vertx run examples/eventbus/handler.clj -cluster
    
and in another shell:

    vertx run examples/eventbus/sender.clj -cluster
