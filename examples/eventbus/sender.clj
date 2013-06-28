(ns example.sender
  (:use [vertx.core])
  )


(def address "example.address")
(def msg-count (atom 0))

(start-vertx vertx
             (periodic 1000
                       (handler [_]
                                (let [msg (str "some-message-" (swap! msg-count inc))]
                                  (bus-send address msg)
                                  (println "sent message" msg)))))
