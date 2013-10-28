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
            [clojure.java.io :as io]
            [clojure.tools.nrepl.server :as nrepl]))

(defn ^:private repl-port [server]
  (-> server deref :ss .getLocalPort))

(defn ^:private repl-host [server]
  (-> server deref :ss .getInetAddress .getHostAddress))

(defonce ^:private repls (atom {}))

(defn ^:private -stop [id]
  (when-let [server (get @repls id)]
    (log/info
     (format "Stopping nREPL at %s:%s"
             (repl-host @server)
             (repl-port @server)))
    (swap! repls dissoc id)
    (.close @server)))

(defn ^:private write-port-file [port]
  (let [f (doto (io/file ".nrepl-port")
            .deleteOnExit)]
    (spit f port)
    (log/debug "Wrote nrepl port to" (.getAbsolutePath f))))

(defn ^:private -start [port host]
  (log/info (format "Starting nREPL at %s:%s" host port))
  (let [server
        (nrepl/start-server
         :port port
         :bind host)
        actual-port (repl-port server)]
    (write-port-file actual-port)
    (require 'clj-stacktrace.repl 'complete.core)
    (log/info (format "nREPL bound to %s:%s"
                      (repl-host server)
                      actual-port))
    server))

(defn stop
  "Stops the nREPL server with the given id, asynchronously."
  [id]
  (future-call (bound-fn []
                 (-stop id))))

(defn start
  "Starts an nREPL server, asynchrously.
   port defaults to 0, which means \"any available port\". host
   defaults to \"127.0.0.1\". Returns an id for the server that can be
   passed to stop to shut it down. the repl will automatically be
   shutdown when the verticle that started it is stopped."
  ([]
     (start 0))
  ([port]
     (start port "127.0.0.1"))
  ([port host]
     (let [id (str "repl-" (u/uuid))]
       (swap! repls assoc id
              (future-call (bound-fn []
                             (-start port host))))
       id)))
