# mod-lang-clojure

THIS IS A WORK IN PROGRESS, AND FAR FROM COMPLETE.

A vert.x language implementation in Clojure

Build with:

    mvn install

Update `langs.properties` in your VERTX_HOME/conf with:

    clojure=io.vertx~lang-clojure~1.0.0-SNAPSHOT:io.vertx.lang.clojure.ClojureVerticleFactory
    .clj=clojure

## Documentation

See the current user manual at
[docs/core_manual_clojure.md](docs/core_manual_clojure.md).  The
latest API docs are available from
[CI](https://projectodd.ci.cloudbees.com/job/vertx.mod-lang-clojure/lastSuccessfulBuild/artifact/target/html-docs/index.html).

## Examples

See [examples/README.md](examples/README.md).
