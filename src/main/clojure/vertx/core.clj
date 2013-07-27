;; Copyright 2013 the original author or authors.
;; 
;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at
;; 
;;      http://www.apache.org/licenses/LICENSE-2.0
;; 
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.

(ns vertx.core
  "Vert.x core functionality."
  (:require [vertx.utils :refer :all])
  (:import [org.vertx.java.core Handler VertxException VoidHandler]
           [org.vertx.java.core.json JsonObject]))

(defonce ^{:dynamic true
           :doc "The currently active default vertx instance.
                 The root binding will be set on verticle deploymeny by Vert.x.
                 You should only need to bind this for advanced usage."}
  *vertx* nil)

(defonce ^{:dynamic true
           :doc "The currently active default vertx container instance.
                 The root binding will be set on verticle deploymeny by Vert.x.
                 You should only need to bind this for advanced usage."}
  *container* nil)

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
  Returns f unmodified if it is nil or already a Handler. If provided,
  result-fn will be applied to the event before passing it to f."
  ([f]
     (as-handler f identity))
  ([f result-fn]
      (if (or (nil? f) (handler? f))
        f
        (reify Handler
          (handle [_# event#]
            (f (result-fn event#)))))))

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
   If include-result-or-result-fn is true (the default), the fn will
   be passed the exception and the result from the AsyncResult,
   otherwise just the exception. If include-result-or-result-fn is a
   fn, it will be passed the result before passing it to the handler
   fn. Returns f unmodified if it is nil or already a Handler."
  ([f]
     (as-async-result-handler f true))
  ([f include-result-or-result-fn]
     (if (or (nil? f) (handler? f))
       f
       (as-handler
        (fn [r]
          (if include-result-or-result-fn
            (f (.cause r)
               (if (fn? include-result-or-result-fn)
                 (include-result-or-result-fn (.result r))
                 (.result r)))
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
  "Returns the configuration map from the current *container*, with the keys converted to keywords."
  []
  (decode (.config (get-container))))

(defn deploy-module
  "Deploys the module with the given name to *container*.
   If instances is not provided, it defaults to 1. handler can either
   be a two-arity fn that will be passed the exception (if any) and
   module id from the result of the deploy call, or a
   org.vertx.java.core.Handler that will be called with the
   AsyncResult object that wraps the exception and id."
  ([module-name]
     (deploy-module module-name nil nil nil))
  ([module-name config]
     (deploy-module module-name config nil nil))
  ([module-name config instances]
     (deploy-module module-name config instances nil))
  ([module-name config instances handler]
     (.deployModule (get-container)
                    module-name
                    (encode config)
                    (or instances 1)
                    (as-async-result-handler handler))))

(defn undeploy-module
  "Undeploys the module identified by id from *container*.
   handler can either be a single-arity fn that will be passed the
   exception (if any) from the result of the deploy call, or a
   org.vertx.java.core.Handler that will be called with the
   AsyncResult object that wraps the exception."
  ([id]
     (undeploy-module id nil))
  ([id handler]
     (.undeployModule (get-container) id
                      (as-async-result-handler handler false))))

(defn deploy-verticle
  "Deploys the verticle with the given main file path to *container*.
   If instances is not provided, it defaults to 1. handler can either
   be a two-arity fn that will be passed the exception (if any) and
   verticle id from the result of the deploy call, or a
   org.vertx.java.core.Handler that will be called with the
   AsyncResult object that wraps the exception and id."
  ([main]
     (deploy-verticle main nil nil nil))
  ([main config]
     (deploy-verticle main config nil nil))
  ([main config instances]
     (deploy-verticle main config instances nil))
  ([main config instances handler]
     (.deployVerticle (get-container)
                      main
                      (encode config)
                      (or instances 1)
                      (as-async-result-handler handler))))

(defn deploy-worker-verticle
  "Deploys the worker verticle with the given main file path to *container*.
   If instances or multi-threaded? not provided, they default to 1 and
   false, respectively. handler can either be a two-arity fn that will
   be passed the exception (if any) and verticle id from the result of
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
     (.deployWorkerVerticle (get-container)
                            main
                      (encode config)
                      (or instances 1)
                      multi-threaded?
                      (as-async-result-handler handler))))

(defn undeploy-verticle
  "Undeploys the verticle identified by id from *container*.
   handler can either be a single-arity fn that will be passed the
   exception (if any) from the result of the deploy call, or a
   org.vertx.java.core.Handler that will be called with the
   AsyncResult object that wraps the exception."
  ([id]
     (undeploy-verticle id nil))
  ([id handler]
     (.undeployVerticle (get-container) id
                        (as-async-result-handler handler false))))

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
  "Registers a handler with *vertx* to be called once in delay ms.
   handler can either be a single-arity fn or an
   org.vertx.java.core.Handler that will be passed the id of the
   timer. Returns the id of the timer."
  [delay handler]
  (.setTimer (get-vertx) delay (as-handler handler)))

(defmacro timer
  "Invokes the body in delay ms.
   Returns the id of the timer. Calling (timer 1 (foo)) is analogous
   to calling (timer* 1 (fn [_] (foo)))."
  [delay & body]
  `(timer* ~delay (fn [_#] ~@body)))

(defn periodic*
  "Registers a handler with *vertx* to be called every interval ms until cancelled.
   handler can either be a single-arity fn or an
   org.vertx.java.core.Handler that will be passed the id of the
   timer. Returns the id of the timer."
  [interval handler]
  (.setPeriodic (get-vertx) interval (as-handler handler)))

(defmacro periodic
  "Invokes the body every interval ms until cancelled.
   Returns the id of the timer. Calling (periodic 1 (foo)) is
   analogous to calling (periodic* 1 (fn [_] (foo)))."
  [interval & body]
  `(periodic* ~interval (fn [_#] ~@body)))

(defn cancel-timer
  "Cancels the timer specified by id using *vertx*."
  [id]
  (.cancelTimer (get-vertx) id))

(defn current-context
  "Returns the current Context for *vertx*."
  []
  (.currentContext (get-vertx)))

(defn run-on-context
  "Put the zero-arity fn on the event queue for the context so it will
   be run asynchronously ASAP after this event has been processed.
   If no context is provided, the context from *vertx* is used."
  ([f]
     (run-on-context (get-vertx) f))
  ([context f]
     (.runOnContext context (as-void-handler
                             (bound-fn [] (f))))))

(defn event-loop?
  "Is the current thread an event loop thread?"
  []
  (.isEventLoop (get-vertx)))

(defn worker? []
  "Is the current thread an worker thread?"
  (.isWorker (get-vertx)))

(defn env
  "Returns a map of environment variables."
  []
  (.env (get-container)))

(defn exit
  "Causes the Vert.x instance to shutdown."
  []
  (.exit (get-container)))
