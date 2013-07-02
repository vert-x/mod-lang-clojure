(ns deploy.child
  (:require [vertx.core :as core]
            [vertx.eventbus :as eb]
            [vertx.testtools :as tt]))

(core/on-stop
  (eb/send "test.data" "stopped"))

(tt/assert= {:ham "biscuit"} (core/config))

(eb/send "test.data" "started")


