mod-lang-clojure
================

A vert.x language implementation in Clojure

Build with:

    mvn install

Update `langs.properties` in your VERTX_HOME/conf with:

    clojure=io.vertx~lang-clojure~1.0.0-SNAPSHOT:io.vertx.lang.clojure.ClojureVerticleFactory
    .clj=clojure


Run the echo server:

    vertx run examples/echo/echo-server.clj

and, in another shell, run the echo client:

    vertx run examples/echo/echo-client.clj
