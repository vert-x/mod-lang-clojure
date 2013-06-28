(ns example.handler
  (:use [vertx.core]))


(def address "example.address")

(start-vertx vertx
             (bus-register address
                           (handler [^String msg]
                                    (println "Received message" (.body msg)))))
