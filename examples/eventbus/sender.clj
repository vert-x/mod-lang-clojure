(ns example.sender
  (:require [vertx.core :as vertx]
            [vertx.eventbus :as eb]))

(def address "example.address")
(def msg-count (atom 0))

(vertx/periodic
 1000
 (let [msg (str "some-message-" (swap! msg-count inc))]
   (eb/send address msg)
   (println "sent message" msg)))
