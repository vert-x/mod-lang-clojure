# ChangeLog

## v0.3.0 - unreleased

* BREAKING CHANGE: Pass the eventbus to on-open callbacks in the ClojureScript client [#53](/../../issues/53)
* nil message handlers are now properly handled [#56](/../../issues/56)
* Maps were sometimes encoded as JsonArrays [#57](/../../issues/57)
* vertx.logging/get-logger now always returns a logger, even when embedded [#59](/../../issues/59)
* start-repl now properly honors a port argument [#60](/../../issues/60)
* start-repl now writes out the actual bound port to .nrepl-port [#58](/../../issues/58)

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
