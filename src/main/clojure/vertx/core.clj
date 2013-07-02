(ns vertx.core
  (:require [vertx.utils :refer :all])
  (:import [org.vertx.java.core Handler]
           [org.vertx.java.core.json JsonObject]))

(def no-vertx-runtime-error
  (Exception. (str "there are not Vertx instance in this process.")))

(def ^:dynamic *vertx* (atom nil))

(defn get-vertx [] 
  (or @*vertx*
      (throw no-vertx-runtime-error)))

(defmacro start-vertx
  [vertx & body]
  `(do (reset! *vertx* ~vertx) ~@body))

(defn bus-send
  "Send message with ```EventBus``` and it's address"
  ([addr content] (.send (.eventBus (get-vertx)) addr content))
  ([addr content handler] (.send (.eventBus (get-vertx)) addr content handler)))

(defn bus-register
  "Register message handler with address in Eventbus"
  ([addr handler] (.registerHandler (.eventBus (get-vertx)) addr handler))
  ([addr handler rest-handler] (.registerHandler (.eventBus (get-vertx)) addr handler rest-handler)))


(defn periodic [delay handler]
  (.setPeriodic (get-vertx) delay handler))

(defn timer [delay handler]
  (.setTimer (get-vertx) delay handler))

(defn cancel-timer [id]
  (.cancelTimer (get-vertx) id))

(declare !vertx)
(declare !container)

(defn handle* [f]
  (if (instance? Handler f)
    f
    (reify Handler
      (handle [_# event#]
        (f event#)))))

(defmacro handler
  [bindings & body]
  `(handle* (fn ~bindings
              ~@body)))

(defmacro defhandler
  [name & rest]
  `(def ~name (handler ~@rest)))

(defn config []
  (<-json (.config !container)))

(defn deploy-module
  ([module-name conf]
     (fn [vertx# container#]
       (.deployModule container# module-name (->json conf))))
  ([module-name conf instances]
     (fn [vertx# container#]
       (.deployModule container# module-name (->json conf) instances)))
  ([module-name conf instances done-handler]
     (fn [vertx# container#]
       (.deployModule container# module-name (->json conf) instances done-handler)))
  )

(defmacro deploy-module [container & body]
  `(.deployModule ~container ~@body))

(defmacro undeploy-module [container id & body]
  `(.undeployModule ~container ~id ~@body))


(defmacro deploy-verticle [container & body]
  `(.deployVerticle ~container ~@body))

(defmacro undeploy-verticle [container id & body]
  `(.undeployVerticle ~container ~id ~@body))

(defn deploy-verticle* [main {:keys [config instances handler]}]
  (.deployVerticle !container main
                   (->json config)
                   (or instances 1)
                   handler))

(defmacro deploy-worker-verticle [container & body]
  `(.deployWorkerVerticle ~container  ~@body))

;; bound by ClojureVerticle
(def ^:dynamic !vertx-stop-fn nil)

(defmacro on-stop [& body]
  `(reset! !vertx-stop-fn (fn [] ~@body)))
