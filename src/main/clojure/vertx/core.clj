(ns vertx.core
  (:require [vertx.utils :refer :all])
  (:import [org.vertx.java.core Handler VertxException]
           [org.vertx.java.core.json JsonObject]))

(defonce ^:dynamic *vertx* nil)
(defonce ^:dynamic *container* nil)

(defn get-vertx [] 
  (or *vertx*
      (throw (VertxException. "No vertx instance available."))))

(defn get-container [] 
  (or *container*
      (throw (VertxException. "No container instance available."))))

(defmacro start-vertx
  [vertx & body]
  `(binding [*vertx* ~vertx]
     ~@body))

(defn handler? [h]
  (instance? Handler h))

(defn handler* [f]
  (if (or (nil? f) (handler? f))
    f
    (reify Handler
      (handle [_# event#]
        (f event#)))))

(defmacro handler
  [bindings & body]
  `(handler* (fn ~bindings
              ~@body)))

(defmacro defhandler
  [name & rest]
  `(def ~name (handler ~@rest)))

(defn config []
  (decode (.config (get-container))))

(defn async-result-handler
  ([f]
     (async-result-handler f true))
  ([f include-result?]
     (if (or (nil? f) (handler? f))
       f
       (handler*
        (fn [r]
          (if include-result?
            (f (.cause r) (.result r))
            (f (.cause r))))))))

(defn deploy-module
  ([module-name]
     (deploy-module module-name nil nil nil))
  ([module-name config]
     (deploy-module module-name config nil nil))
  ([module-name config instances]
     (deploy-module module-name config instances nil))
  ([module-name config instances handler]
     (deploy-module (get-container) module-name config instances handler))
  ([container module-name config instances handler]
     (.deployModule container module-name
                    (encode config)
                    (or instances 1)
                    (async-result-handler handler))))

(defn undeploy-module
  ([id]
     (undeploy-module id nil))
  ([id handler]
     (undeploy-module (get-container) id handler))
  ([container id handler]
     (.undeployModule container id (async-result-handler handler false))))

(defn deploy-verticle
  ([main]
     (deploy-verticle main nil nil nil))
  ([main config]
     (deploy-verticle main config nil nil))
  ([main config instances]
     (deploy-verticle main config instances nil))
  ([main config instances handler]
     (deploy-verticle (get-container) main config instances handler))
  ([container main config instances handler]
     (.deployVerticle container main
                      (encode config)
                      (or instances 1)
                      (async-result-handler handler))))

(defn deploy-worker-verticle
  ([main]
     (deploy-worker-verticle main nil nil nil nil))
  ([main config]
     (deploy-worker-verticle main config nil nil nil))
  ([main config instances]
     (deploy-worker-verticle main config instances nil nil))
  ([main config instances multi-threaded?]
     (deploy-worker-verticle main config instances multi-threaded? nil))
  ([main config instances multi-threaded? handler]
     (deploy-worker-verticle (get-container) main config instances multi-threaded? handler))
  ([container main config instances multi-threaded? handler]
     (.deployWorkerVerticle container main
                      (encode config)
                      (or instances 1)
                      multi-threaded?
                      (async-result-handler handler))))

(defn undeploy-verticle
  ([id]
     (undeploy-verticle id nil))
  ([id handler]
     (undeploy-verticle (get-container) id handler))
  ([container id handler]
     (.undeployVerticle container id (async-result-handler handler false))))

(defn simple-handler [f]
  (if (or (nil? f) (handler? f))
    f
    (handler*
     (fn [& _]
       (f)))))

(defn config []
  (decode (.config (get-container))))

;; bound by ClojureVerticle
(def ^:dynamic !vertx-stop-fn nil)

(defmacro on-stop [& body]
  `(reset! !vertx-stop-fn (fn [] ~@body)))

(defn timer*
  ([t h]
     (timer* (get-vertx) t h))
  ([vertx t h]
     (.setTimer vertx t (handler* h))))

(defmacro timer [t & body]
  `(timer* ~t (fn [_#] ~@body)))

(defn periodic*
  ([t h]
     (periodic* (get-vertx) t h))
  ([vertx t h]
     (.setPeriodic vertx t (handler* h))))

(defmacro periodic [t & body]
  `(periodic* ~t (fn [_#] ~@body)))

(defn cancel-timer
  ([id]
     (cancel-timer (get-vertx) id))
  ([vertx id]
     (.cancelTimer vertx id)))

