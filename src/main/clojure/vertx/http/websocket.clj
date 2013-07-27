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
  "TODO: docs"
  (:require [vertx.buffer :as buf]
            [vertx.core :as core]
            [vertx.http :as http])
  (:import [org.vertx.java.core.http WebSocketVersion]))


(defn on-websocket
  "Set the websocket handler for the server to wsHandler.
   If a websocket connect handshake is successful a
   new ServerWebSocket instance will be created and passed to the handler."
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
  "TODO: docs"
  ([http-client uri h]
     (connect http-client uri nil nil h))
  ([http-client uri version h]
     (connect http-client uri version nil h))
  ([http-client uri version header h]
     (.connectWebsocket http-client uri (ws-version version)
                        (http/encode-headers header)
                        (core/as-handler h))))

(defn write-binary-frame
  "Write data Buffer to websocket as a binary frame."
  [ws data]
  (.writeBinaryFrame ws (buf/as-buffer data)))

(defn write-text-frame
  "Write data String to websocket as a text frame."
  [ws data]
  (.writeTextFrame ws data))
