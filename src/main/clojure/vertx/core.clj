(ns vertx.core
  "Vert.x core functionality."
  (:require [vertx.utils :refer :all])
  (:import [org.vertx.java.core Handler VertxException VoidHandler]
           [org.vertx.java.core.json JsonObject]))

(defonce ^{:dynamic true
           :doc "The currently active default vertx instance.
                 The root binding will be set when deployed by vertx."} *vertx*
           nil)

(defonce ^{:dynamic true
           :doc "The currently active default vertx container instance.
                 The root binding will be set when deployed by vertx."} *container*
    nil)

(defn get-vertx
  "Returns the currently active vertx instance (*vertx*), throwing if not set."
  [] 
  (or *vertx*
      (throw (VertxException. "No vertx instance available."))))

(defn get-container
  "Returns the currently active vertx container instance (*container*), throwing if not set."
  [] 
  (or *container*
      (throw (VertxException. "No container instance available."))))

;; TODO: do we need this? if so, rename to with-vertx?
(defmacro start-vertx
  [vertx & body]
  `(binding [*vertx* ~vertx]
     ~@body))

(defn ^:internal ^:no-doc handler?
  "Returns true if h is an instance of org.vertx.java.core.Handler"
  [h]
  (instance? Handler h))

(defn as-handler
  "Wraps the given single-arity f in a org.vertx.java.core.Handler.
  Returns f unmodified if it is nil or already a Handler."
  [f]
  (if (or (nil? f) (handler? f))
    f
    (reify Handler
      (handle [_# event#]
        (f event#)))))

(defmacro handler
  "Wraps the given bindings and body in a org.vertx.java.core.Handler.
   Calling (handler [foo] (bar foo)) is analogous to calling
   (as-handler (fn [foo] (bar foo)))."
  [bindings & body]
  `(as-handler (fn ~bindings
                 ~@body)))

(defmacro defhandler
  "Creates a named (def'ed) handler."
  [name bindings & body]
  `(def ~name (handler ~bindings ~@body)))

(defn as-async-result-handler
  "Wraps the given fn in a org.vertx.java.core.Handler for handling an AsyncResult.
   If include-result? is true (the default), the fn will be passed the
   exception and the result from the AsyncResult, otherwise just the exception.
   Returns f unmodified if it is nil or already a Handler."
  ([f]
     (as-async-result-handler f true))
  ([f include-result?]
     (if (or (nil? f) (handler? f))
       f
       (as-handler
        (fn [r]
          (if include-result?
            (f (.cause r) (.result r))
            (f (.cause r))))))))

(defn as-void-handler
  "Wraps the given fn in a Handler that ignores the event.
   f is expected to be a zero-arity fn or a Handler.
   Returns f unmodified if it is nil or already a Handler."
  [f]
  (if (or (nil? f) (handler? f))
    f
    (as-handler (fn [_#] (f)))))

(defn config
  "Returns the configuration map from the container.
   If no container is provided, the default is used."
  ([]
     (config (get-container)))
  ([container]
     (decode (.config container))))

(defn deploy-module
  "Deploys the module with the given name.
   If container or instances are not provided, they default to the
   default container (*container*) and 1, respectively. handler can
   either be a two-arity fn that will be passed the exception (if any)
   and module id from the result of the deploy call, or a
   org.vertx.java.core.Handler that will be called with the
   AsyncResult object that wraps the exception and id."
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
                    (as-async-result-handler handler))))

(defn undeploy-module
  "Undeploys the module identified by id.
   If container is not provided, it defaults to the default
   container (*container*). handler can either be a single-arity fn
   that will be passed the exception (if any) from the result of the
   deploy call, or a org.vertx.java.core.Handler that will be called
   with the AsyncResult object that wraps the exception."
  ([id]
     (undeploy-module id nil))
  ([id handler]
     (undeploy-module (get-container) id handler))
  ([container id handler]
     (.undeployModule container id (as-async-result-handler handler false))))

(defn deploy-verticle
  "Deploys the verticle with the given main file path.
   If container or instances are not provided, they default to the
   default container (*container*) and 1, respectively. handler can
   either be a two-arity fn that will be passed the exception (if any)
   and verticle id from the result of the deploy call, or a
   org.vertx.java.core.Handler that will be called with the
   AsyncResult object that wraps the exception and id."
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
                      (as-async-result-handler handler))))

(defn deploy-worker-verticle
  "Deploys the worker verticle with the given main file path.
   If container, instances, or multi-threaded? not provided, they
   default to the default container (*container*), 1, and false,
   respectively. handler can either be a two-arity fn that will be
   passed the exception (if any) and verticle id from the result of
   the deploy call, or a org.vertx.java.core.Handler that will be
   called with the AsyncResult object that wraps the exception and
   id."
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
                      (as-async-result-handler handler))))

(defn undeploy-verticle
  "Undeploys the verticle identified by id.
   If container is not provided, it defaults to the default
   container (*container*). handler can either be a single-arity fn
   that will be passed the exception (if any) from the result of the
   deploy call, or a org.vertx.java.core.Handler that will be called
   with the AsyncResult object that wraps the exception."
  ([id]
     (undeploy-verticle id nil))
  ([id handler]
     (undeploy-verticle (get-container) id handler))
  ([container id handler]
     (.undeployVerticle container id (as-async-result-handler handler false))))

;; bound by ClojureVerticle
(def ^:dynamic ^:internal ^:no-doc !vertx-stop-fn nil)

(defn on-stop*
  "Registers a fn to be called when vertx undeploys the verticle.
   Can be called multiple times to register multiple fns."
  [f]
  (swap! !vertx-stop-fn conj f))

(defmacro on-stop
  "Registers code to be called when vertx undeploys the verticle.
   Can be called multiple times to register multiple bodies.
   Calling (on-stop (foo)) is analogous to calling (on-stop* (fn
   [] (foo)))."
  [& body]
  `(on-stop* (fn [] ~@body)))

(defn timer*
  "Registers a handler to be called once in delay ms.
   Returns the id of the timer. If vertx is not provided, it defaults to
   the default vertx (*vertx*). handler can either be a single-arity
   fn or an org.vertx.java.core.Handler that will be passed the id of
   the timer. Returns the id of the timer."
  ([delay handler]
     (timer* (get-vertx) delay handler))
  ([vertx delay handler]
     (.setTimer vertx delay (as-handler handler))))

(defmacro timer
  "Invokes the body in delay ms.
   Returns the id of the timer. Calling (timer 1 (foo)) is analogous
   to calling (timer* 1 (fn [_] (foo)))."
  [delay & body]
  `(timer* ~delay (fn [_#] ~@body)))

(defn periodic*
  "Registers a handler to be called every interval ms until cancelled.
   Returns the id of the timer. If vertx is not provided, it defaults
   to the default vertx (*vertx*). handler can either be a
   single-arity fn or an org.vertx.java.core.Handler that will be
   passed the id of the timer."
  ([interval handler]
     (periodic* (get-vertx) interval handler))
  ([vertx interval handler]
     (.setPeriodic vertx interval (as-handler handler))))

(defmacro periodic
  "Invokes the body every interval ms until cancelled.
   Returns the id of the timer. Calling (periodic 1 (foo)) is
   analogous to calling (periodic* 1 (fn [_] (foo)))."
  [interval & body]
  `(periodic* ~interval (fn [_#] ~@body)))

(defn cancel-timer
  "Cancels the timer specified by id.
   If vertx is not provided, it defaults to the default
   vertx (*vertx*)."
  ([id]
     (cancel-timer (get-vertx) id))
  ([vertx id]
     (.cancelTimer vertx id)))

