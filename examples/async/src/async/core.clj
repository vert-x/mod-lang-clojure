(ns async.core
  (:import java.util.concurrent.CountDownLatch)
  (:require [vertx.embed :as vertx]
            [vertx.eventbus :as eb]
            [clojure.core.async :as async :refer [<! >! chan alt! go]]))

(def address "example.address")

(defn- reply-processor [chan]
  (go (loop [[msg] (alts! [chan])]
        (println "sender received:" msg)
        (recur (<! chan)))))

(defn bus-sender []
  (let [reply-chan (chan)]
    (eb/send address "ping"
             (fn [reply]
               (go (>! reply-chan reply))
               (reply-processor reply-chan)
               ))))


(defn- message-processor [chan]
  (go (loop [[msg] (alts! [chan])]
        (println "listener received:" msg)
        (eb/reply "pong")
        (recur (<! chan)))))

(defn bus-listener []
  (let [chan (chan)]
    (eb/on-message
     address
     (fn [msg]
       (go (>! chan msg))
       (message-processor chan)
       ))))

(defn -main
  [& args]
  (vertx/set-vertx! (vertx/vertx))
  (let [latch (CountDownLatch. 1)]
    (bus-listener)
    (bus-sender)
    (.await latch)))
