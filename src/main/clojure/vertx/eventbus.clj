(ns vertx.eventbus
  (:refer-clojure :exclude [send])
  (:require [vertx.core :as core]
            [vertx.utils :refer :all]))

(defn eventbus []
  (.eventBus core/!vertx))

(defn send
  ([addr content]
     (.send (eventbus) addr (encode content)))
  ([addr content handler]
     (.send (eventbus) addr (encode content) (core/handle* handler))))

(defn publish
  [addr content]
  (.publish (eventbus) addr (encode content)))

(defn reply
  ([m]
     (.reply m))
  ([m content]
     (.reply m (encode content)))
  ([m content handler]
     (.reply m (encode content)) (core/handle* handler)))

(let [registered-handlers (atom {})]

  (defn register-handler
    "Registers a handler fn to receive messages on an address.
Returns an id for the handler that can be passed to
unregister-handler."
    ([addr handler-fn]
       (register-handler addr handler-fn false))
    ([addr handler-fn local-only?]
       (let [h (core/handle* handler-fn)
             id (uuid)]
         (if local-only?
           (.registerLocalHandler (eventbus) addr h)
           (.registerHandler (eventbus) addr h))
         (swap! registered-handlers assoc id [addr h])
         id)))

  (defn unregister-handler
    [id]
    (if-let [[addr h] (@registered-handlers id)]
      (do
        (.unregisterHandler (eventbus) addr h)
        (swap! registered-handlers dissoc id))
      (throw (IllegalArgumentException.
              (format "No handler with id %s found." id))))
    nil))

(defn message-body [m]
  (decode (.body m)))

