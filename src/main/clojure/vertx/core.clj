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

(defn handler? [h]
  (instance? Handler h))

(defn handle* [f]
  (if (or (nil? f) (handler? f))
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
  (decode (.config !container)))

(defn async-result-handler
  ([f]
     (async-result-handler f true))
  ([f include-result?]
     (if (or (nil? f) (handler? f))
       f
       (handle*
        (fn [r]
          (if include-result?
            (f (.cause r) (.result r))
            (f (.cause r))))))))

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

(defn simple-handler [f]
  (if (or (nil? f) (handler? f))
    f
    (handle*
     (fn [& _]
       (f)))))

(defn deploy-verticle
  ([main]
     (deploy-verticle main nil nil nil))
  ([main config]
     (deploy-verticle main config nil nil))
  ([main config instances]
     (deploy-verticle main config instances nil))
  ([main config instances handler]
     (deploy-verticle !container main config instances handler))
  ([container main config instances handler]
     (.deployVerticle container main
                      (encode config)
                      (or instances 1)
                      (async-result-handler handler))))

(defn undeploy-verticle
  ([id]
     (undeploy-verticle id nil))
  ([id handler]
     (undeploy-verticle !container id handler))
  ([container id handler]
     (.undeployVerticle container id (async-result-handler handler false))))

;; bound by ClojureVerticle
(def ^:dynamic !vertx-stop-fn nil)

(defmacro on-stop [& body]
  `(reset! !vertx-stop-fn (fn [] ~@body)))

(defn set-timer*
  ([t h]
     (set-timer* !vertx t h))
  ([vertx t h]
     (.setTimer vertx t (handle* h))))

(defmacro set-timer [t & body]
  `(set-timer* ~t (fn [_#] ~@body)))

(defn set-periodic*
  ([t h]
     (set-periodic* !vertx t h))
  ([vertx t h]
     (.setPeriodic vertx t (handle* h))))

(defmacro set-periodic [t & body]
  `(set-periodic* ~t (fn [_#] ~@body)))

(defn cancel-timer
  ([id]
     (cancel-timer !vertx id))
  ([vertx id]
     (.cancelTimer vertx id)))

(defni deploy-worker-verticle)
(defni deploy-module)
