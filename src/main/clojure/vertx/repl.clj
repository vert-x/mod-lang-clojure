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

(let [repls (atom {})]

  ;; TODO: take a handler fn?
  (defn stop-repl
    "Stops the nREPL server with the given id, asynchronously."
    [id]
    (core/run-on-context
     (fn []
       (when-let [server (get @repls id)]
         (log/info "Stopping nREPL on %s:%s"
                   (repl-host server)
                   (repl-port server))
         (swap! repls dissoc id)
         (.close server)))))

  ;; TODO: take a handler fn?
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
       (let [id (str "repl-" (u/uuid))]
         (core/run-on-context
          (fn []
            (log/info (format "Starting nREPL at %s:%s" host port))
            (let [server
                  (nrepl/start-server
                   :handler (nrepl/default-handler #'nrepl-init-handler)
                   :port port
                   :bind host)]
              (log/info (format "nREPL bound to %s:%s"
                                (repl-host server)
                                (repl-port server)))
              ;; FIXME: need to check to see if stop was called before
              ;; we got this far
              (core/on-stop* (partial stop-repl id))
              (swap! repls assoc id server))))
         id))))

