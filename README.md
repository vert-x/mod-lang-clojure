mod-lang-clojure
================

A vert.x language implementation in Clojure


Setting langs.properties in your VERTX_HOME/conf with


clojure=me.streamis~lang-clojure~2.0.0-CR2-SNAPSHOT:org.vertx.java.platform.impl.ClojureVerticleFactory

.clj=clojure


make it work with command of vertx
vertx run example/echo/echo-server.clj

in another shell
vertx run example/echo/echo-client.clj
