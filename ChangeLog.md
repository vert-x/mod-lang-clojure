# ChangeLog

## 0.4.0 - unreleased

* `vertx.client.eventbus/on-open` & `vertx.client.eventbus/on-close` now return the eventbus to ease chaining [#81](/../../issues/81)
* nrepl worker threads now have the context for \*vertx\* conveyed [#84](/../../issues/84)

## [v0.3.0](/../../tree/0.3.0) - 2013-11-08

Breaking changes:

* Pass the eventbus to on-open callbacks in the ClojureScript client [#53](/../../issues/53)
* The APIs for the `deploy-*` functions in `vertx.core` have switched to kwarg args [#61](/../../issues/61)
* `vertx.repl/start-repl` and `vertx.repl/stop-repl` have been renamed to `vertx.repl/start` and `vertx.repl/stop` [#62](/../../issues/62)
* The `:stream` key in the response from `vertx.http/upload-file-info` has been renamed `:basis`
* `vertx.http/remote-address` now returns the raw host string as `:host`, instead of the possibly DNS-resolved value [#69](/../../issues/69)
* Exceptions passed to callbacks are now decomposed into maps [#74](/../../issues/74)

Other changes:

* nil message handlers are now properly handled [#56](/../../issues/56)
* Maps were sometimes encoded as JsonArrays [#57](/../../issues/57)
* vertx.logging/get-logger now always returns a logger, even when embedded [#59](/../../issues/59)
* repl/start now properly honors a port argument [#60](/../../issues/60)
* repl/start now writes out the actual bound port to .nrepl-port [#58](/../../issues/58)
* The module can now run under Vert.x 2.1M1 in addition to 2.0.x [#66](/../../issues/66)
* `vertx.http/remote-address` now includes the source InetSocketAddress object as `:basis`
* The module now uses Clojure 1.6.0-alpha2 instead of a custom Clojure build that prevented memory leaks
* (vertx.buffer/append! buf string encoding) now works [#71](/../../issues/71)
* An async DNS client is now provided in `vertx.dns` [#65](/../../issues/65)
* UDP sockets are now supported via `vertx.datagram` [#64](/../../issues/64)
* The new EventBus timeout features from Vert.x core are now supported [#54](/../../issues/54)

## [v0.2.0](/../../tree/0.2.0) - 2013-09-17

* add `with-vertx` convenience macro to embed ns
* Add ClojureScript wrapper around vertxbus.js [#50](/../../issues/50)
* Coerce appropriate sockjs hooks to booleans [#48](/../../issues/48)
* BREAKING CHANGE: Rename event-bus -> eventbus [#49](/../../issues/49)
* Don't require all sockjs hooks to be set [#52](/../../issues/52)

## [v0.1.2](/../../tree/0.1.2) - 2013-08-26
 
* Add additional metadata required by the Vert.x module registry

## [v0.1.1](/../../tree/0.1.1) - 2013-08-26

* Convey bindings for handler functions [#43](/../../issues/43)
* Change the lang-module artifact name (lang-clojure.zip ->
  lang-clojure-mod.zip) to follow Vert.x module registry guidelines

## [v0.1.0](/../../tree/0.1.0) - 2013-08-12 

Initial release
