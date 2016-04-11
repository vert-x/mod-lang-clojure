# mod-lang-clojure

The Clojure language implementation for Vert.x

## Usage

First, you'll need to [install Vert.x](http://vertx.io/install.html).

The latest stable release is *1.0.5*. Vert.x 2.1.2 ships with 1.0.2, so you will need update
`VERTX_HOME/conf/langs.properties` to use 1.0.5 if you want the latest. Change:

    clojure=io.vertx~lang-clojure~1.0.2:io.vertx.lang.clojure.ClojureVerticleFactory

to

    clojure=io.vertx~lang-clojure~1.0.5:io.vertx.lang.clojure.ClojureVerticleFactory

Every commit triggers a [CI](https://vertx.ci.cloudbees.com/job/vert.x-mod-lang-clojure/)
build, and we publish a SNAPSHOT when that succeeds. So if you would
rather run the latest commit, update your `langs.properties` with:

    clojure=io.vertx~lang-clojure~1.0.6-SNAPSHOT:io.vertx.lang.clojure.ClojureVerticleFactory

**Note that this module only works with Vert.x 2.1RC3 or greater.**

## Documentation

* [User Manual](http://vertx.io/core_manual_clojure.html)
* [1.0.4 API docs](http://vertx.io/mod-lang-clojure/docs/1.0.4/index.html)
* [HEAD API docs](https://vertx.ci.cloudbees.com/job/vert.x-mod-lang-clojure/lastSuccessfulBuild/artifact/api/target/html-docs/index.html)

## Examples

See the offical [examples repo](https://github.com/vert-x/vertx-examples/tree/master/src/raw/clojure#mod-lang-clojure-examples).

## License

mod-lang-clojure is licensed under the Apache License, v2.0. See
[LICENSE.txt](LICENSE.txt) for details.
