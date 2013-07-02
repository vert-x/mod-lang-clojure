(ns vertx.eventbus
  (:refer-clojure :exclude [send])
  (:require [vertx.core :as core]
            [vertx.utils :refer :all]))

(defn send* [bus addr content]
  (.send bus addr (->json content)))

(defn eventbus []
  (.eventBus core/!vertx))

;; TODO: accept reply handler
;; TODO: don't assume all content is a map
(defn send [addr content]
  (send* (eventbus) addr content))

(defn register-handler
  ([addr handler-fn]
     (register-handler addr handler-fn false))
  ([addr handler-fn local-only?]
     (let [h (core/handle* handler-fn)]
       (if local-only?
         (.registerLocalHandler (eventbus) addr h)
         (.registerHandler (eventbus) addr h)))))
