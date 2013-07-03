mod-lang-clojure
================

A vert.x language implementation in Clojure


Setting langs.properties in your VERTX_HOME/conf with

    clojure=io.vertx~lang-clojure~1.0.0-SNAPSHOT:io.vertx.lang.clojure.ClojureVerticleFactory
    .clj=clojure


make it work with command of vertx
`vertx run examples/echo/echo-server.clj`

in another shell
`vertx run examples/echo/echo-client.clj`
