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

(ns vertx.repl
  "Functions for managing nREPLs within a Vert.x container."
  (:require [vertx.core :as core]
            [vertx.utils :as u]
            [vertx.logging :as log]
            [vertx.eventbus :as eb]
            [clojure.tools.nrepl.server :as nrepl]
            [clojure.tools.nrepl.middleware :as mware]))

(defn ^:private nrepl-init-handler
  "Provides an init point for new nrepl connections."
  [h]
  (fn [{:keys [op transport] :as msg}]
    (when (= op "clone")
      (require 'clj-stacktrace.repl 'complete.core))
    (h msg)))

(mware/set-descriptor! #'nrepl-init-handler {})

(defn ^:private repl-port [server]
  (-> server deref :ss .getLocalPort))

(defn ^:private repl-host [server]
  (-> server deref :ss .getInetAddress .getHostAddress))

(defonce ^:private repls (atom {}))

(defn ^:private -stop-repl
  "Stops the nREPL server with the given id. Should only be used from the worker verticle."
  [{:keys [id]}]
  (when-let [server (get @repls id)]
    (log/info "Stopping nREPL on %s:%s"
              (repl-host server)
              (repl-port server))
    (swap! repls dissoc id)
    (.close server)))


(defn ^:private -start-repl
  "Starts an nREPL server. Should only be used from the worker verticle."
  [{:keys [id port host]}]
  (log/info (format "Starting nREPL at %s:%s" host port))
  (let [server
        (nrepl/start-server
         :handler (nrepl/default-handler #'nrepl-init-handler)
         :port port
         :bind host)]
    (log/info (format "nREPL bound to %s:%s"
                      (repl-host server)
                      (repl-port server)))
    (swap! repls assoc id server)))


(defn ^:internal ^:no-doc dispatch-handler [msg]
  (let [f (resolve (symbol (format "vertx.repl/-%s-repl" (:cmd body))))]
    (f msg)))

(defonce ^:private repl-worker-address (atom nil))

(defn stop-repl
  "Stops the nREPL server with the given id, asynchronously."
  [id]
  (if @repl-worker-address
    (eb/send @repl-worker-address {:cmd "stop" :id id})))

(defn start-repl
  "Starts an nREPL server, asynchrously.
     port defaults to 0, which means \"any available port\". host
     defaults to \"127.0.0.1\". Returns an id for the server that can
     be passed to stop-repl to shut it down. the repl will
     automatically be shutdown when the verticle that started it is
     stopped."
  ([]
     (start-repl 0))
  ([port]
     (start-repl 0 "127.0.0.1"))
  ([port host]
     (let [id (str "repl-" (u/uuid))
           start (bound-fn [& args]
                   (if (first args)
                     (log/error "repl_worker init failed:" (first args))
                     (do
                       (core/on-stop* (partial stop-repl id))
                       (eb/send @repl-worker-address {:cmd "start"
                                                      :id id
                                                      :host host
                                                      :port port}))))]
       (if @repl-worker-address
         (start)
         (do
           (reset! repl-worker-address (u/uuid))
           (core/deploy-worker-verticle "repl_worker.clj"
                                        {:address @repl-worker-address}
                                        1 true start)))
       id)))
