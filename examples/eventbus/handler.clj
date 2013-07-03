(ns example.handler
  (:require [vertx.eventbus :as eb]))

(def address "example.address")

(eb/register-handler
 address
 (fn [msg]
   (println "Received message" (eb/message-body msg))))
