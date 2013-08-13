# mod-lang-clojure

A fledgling Vert.x language implementation for Clojure

## Usage

The latest stable release is *0.1.0*. Until this module becomes part
of Vert.x proper, you will need to make Vert.x aware of it my editing
`VERTX_HOME/conf/langs.properties` and adding:

    clojure=io.vertx~lang-clojure~0.1.0:io.vertx.lang.clojure.ClojureVerticleFactory
    .clj=clojure


If you would rather run the latest from source, checkout this repo, then:

    mvn install

and update your `langs.properties` with:

    clojure=io.vertx~lang-clojure~0.2.0-SNAPSHOT:io.vertx.lang.clojure.ClojureVerticleFactory
    .clj=clojure

**Note that this module only works with Vert.x 2.0.0 or greater.**

## Documentation

For 0.1.0:

* [User Manual](/vert-x/mod-lang-clojure/blob/0.1.0/docs/core_manual_clojure.md)
* [API docs](http://tcrawley.org/mod-lang-clojure/0.1.0/html-docs/)

For HEAD:

* [User Manual](docs/core_manual_clojure.md)
* [API docs](https://projectodd.ci.cloudbees.com/job/vertx.mod-lang-clojure/lastSuccessfulBuild/artifact/api/target/html-docs/index.html)

## Examples

See [examples/README.md](examples/README.md).
