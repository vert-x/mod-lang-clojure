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

(ns vertx.http.websocket
  "Provides a set of functions for using http websockets."
  (:require [vertx.buffer :as buf]
            [vertx.core :as core]
            [vertx.http :as http])
  (:import [org.vertx.java.core.http WebSocketVersion]))


(defn on-websocket
  "Sets the websocket handler for the HTTP server.
   handler can either be a single-arity fn or a Handler instance that
   will be passed the ServerWebSocket when a successful connection is
   made."
  [server handler]
  (.websocketHandler server (core/as-handler handler)))

(defn- ws-version
  "convert websocket version to Enum, or vice versa
   :RFC6455->RFC6455 :HYBI-00->HYBI_00 :HYBI-08->HYBI_08"
  [version] (if (keyword? version)
              (condp = version
                :RFC6455 WebSocketVersion/RFC6455
                :HYBI-00 WebSocketVersion/HYBI_00
                :HYBI-08 WebSocketVersion/HYBI_08)
              (condp = version
                WebSocketVersion/RFC6455 :RFC6455
                WebSocketVersion/HYBI_00 :HYBI-00
                WebSocketVersion/HYBI_08 :HYBI-08)))

(defn connect
  "Connect the HTTP client to a websocket at the specified URI.
   version allows you to force the protocol version to one
   of: :RFC6455, :HYBI-00, or :HYBI-08.  handler can either be a
   single-arity fn or a Handler instance that will be passed the
   WebSocket when a successful connection is made. Returns the
   client."
  ([client uri handler]
     (connect client uri nil nil handler))
  ([client uri version handler]
     (connect client uri version nil handler))
  ([client uri version header handler]
     (.connectWebsocket client uri (ws-version version)
                        (http/encode-headers header)
                        (core/as-handler handler))))

(defn write-binary-frame
  "Write data Buffer to websocket as a binary frame.
   Returns the websocket."
  [ws data]
  (.writeBinaryFrame ws (buf/as-buffer data)))

(defn write-text-frame
  "Write data String to websocket as a text frame.
   Returns the websocket."
  [ws data]
  (.writeTextFrame ws (str data)))
