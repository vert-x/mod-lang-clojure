# mod-lang-clojure

The Clojure language implementation for Vert.x

## Usage

First, you'll need to [install Vert.x](http://vertx.io/install.html).

The latest stable release is *1.0.2*. Until this module becomes part
of Vert.x proper, you will need to make Vert.x aware of it by editing
`VERTX_HOME/conf/langs.properties` and adding:

    clojure=io.vertx~lang-clojure~1.0.2:io.vertx.lang.clojure.ClojureVerticleFactory
    .clj=clojure


Every commit triggers a [CI](https://vertx.ci.cloudbees.com/job/vert.x-mod-lang-clojure/)
build, and we publish a SNAPSHOT when that succeeds. So if you would
rather run the latest commit, update your `langs.properties` with:

    clojure=io.vertx~lang-clojure~1.0.3-SNAPSHOT:io.vertx.lang.clojure.ClojureVerticleFactory
    .clj=clojure

**Note that this module only works with Vert.x 2.1RC3 or greater.**

## Documentation

* [User Manual](http://vertx.io/core_manual_clojure.html)
* [1.0.2 API docs](http://vertx.io/mod-lang-clojure/docs/1.0.2/index.html)
* [HEAD API docs](https://vertx.ci.cloudbees.com/job/vert.x-mod-lang-clojure/lastSuccessfulBuild/artifact/api/target/html-docs/index.html)

## Examples

See the offical [examples repo](https://github.com/vert-x/vertx-examples/tree/master/src/raw/clojure#mod-lang-clojure-examples).

## License

mod-lang-clojure is licensed under the Apache License, v2.0. See
[LICENSE.txt](LICENSE.txt) for details.
