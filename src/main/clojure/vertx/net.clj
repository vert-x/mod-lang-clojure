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

(ns vertx.net
  "TODO: docs"
  (:require [vertx.core :as core]
            [vertx.common :as common]
            [vertx.utils :as u]))

;;TODO: document properties
(defn server
  "Creates a TCP or SSL server (NetServer) instance using vertx.core/*vertx*."
  ([]
     (server nil))
  ([properties]
     (u/set-properties (.createNetServer (core/get-vertx)) properties)))

;;TODO: document properties
(defn client
  "Creates a TCP or SSL client (NetClient) instance using vertx.core/*vertx*.

   Multiple connections to different servers can be made using the
   same client instance."
  ([]
     (client nil))
  ([properties]
     (u/set-properties (.createNetClient (core/get-vertx)) properties)))

(defn listen
  "Tells the server to start listening for connections on port.
   If host is not provided, it defaults to \"0.0.0.0\". handler can
   either be a two-arity fn that will be passed the exception (if any)
   and server from the result of the listen call, or a Handler
   instance that will be called with the AsyncResult object that wraps
   the exception and server. Returns the server instance.

   Be aware this is an async operation and the server may not bound on
   return of the method."
  ([server port]
     (listen server port nil))
  ([server port host]
     (listen server port host nil))
  ([server port host handler]
     (.listen server port
              (or host "0.0.0.0")
              (core/as-async-result-handler handler))))

(defn on-connect 
  "Attaches a connect handler to the server.
   As the server accepts TCP or SSL connections it creates an instance
   of NetSocket and passes it to the connect handler.  handler can
   either be a single-arity fn or a Handler instance that will be
   passed the socket. Returns the server instance.

   The server can only have at most one connect handler at any one
   time. " [server handler]
  (.connectHandler server (core/as-handler handler)))

(defn on-close
  "Attaches a handler to the socket that will be called when the socket is closed.
   handler can either be a zero-arity fn or a Handler instance.
   Returns the socket."
  [socket handler]
  (.closeHandler socket (core/as-void-handler handler)))

(defn connect
  "Attempts to open a connection to a server.
   If host is not provided, it defaults to \"localhost\". If no client
   is provided, one is created by calling the vertx.net/client fn.
   handler can either be a two-arity fn that will be passed the
   exception (if any) and socket from the result of the connect call,
   or a Handler instance that will be called with the AsyncResult
   object that wraps the exception and socket. Returns the client
   instance."
  ([port handler]
     (connect port nil handler))
  ([port host handler]
     (connect (client) port host handler))
  ([client port host handler]
     (.connect client port
               (or host "localhost")
               (core/as-async-result-handler handler))))

(defn close
  "Close the server. Any open connections will be closed."
  ([server]
     (close server nil))
  ([server handler]
     (common/internal-close server handler)))
