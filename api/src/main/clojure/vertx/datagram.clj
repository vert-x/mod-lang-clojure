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

(ns vertx.datagram
  "Provides a broad set of functions for UDP servers and clients."
  (:refer-clojure :exclude [send])
  (:require [vertx.core :as core :exclude [send]]
            [vertx.common :as common]
            [vertx.utils :as u])
  (:import [java.net InetSocketAddress]
           [ org.vertx.java.core.datagram InternetProtocolFamily]))


(defn- inet-protocol-family
  "convert keyword of protocol to enum, default value is InternetProtocolFamily/IPv4"
  [protocol] (if (keyword? protocol)
               (condp = protocol
                 :ipv4 InternetProtocolFamily/IPv4
                 :ipv6 InternetProtocolFamily/IPv6)
               InternetProtocolFamily/IPv4))

;;TODO: setting properties
(defn socket
  "TODO:comments"
  ([]
     (socket InternetProtocolFamily/IPv4 nil))
  ([protocol-family properties]
     (u/set-properties
      (.createDatagramSocket (core/get-vertx) (inet-protocol-family protocol-family))
      properties)))

(defn send
  "TODO:comments"
  ([socket content host port handler]
     (.send socket content host port (core/as-async-result-handler handler)))

  ([socket content enc host port handler]
     (.send socket content enc host port (core/as-async-result-handler handler))))

(defn listen
  "TODO:comments"
  ([socket port handler]
     (listen socket "0.0.0.0", port handler))
  ([socket host port handler]
     (.listen socket host port (core/as-async-result-handler handler))))

(defn listen-multicast-group
  "TODO:comments"
  ([socket multi-addr handler]
     (.listenMulticastGroup socket multi-addr (core/as-async-result-handler handler)))
  ([socket multi-addr addr source handler]
     (.listenMulticastGroup socket multi-addr addr source (core/as-async-result-handler handler))))

(defn unlisten-multicast-group
  "TODO:comments"
  ([socket multi-addr handler]
     (.unlistenMulticastGroup socket multi-addr (core/as-async-result-handler handler)))
  ([socket multi-addr addr source handler]
     (.unlistenMulticastGroup socket multi-addr addr source (core/as-async-result-handler handler))))

(defn block-multicast-group
  "TODO:comments"
  ([socket multi-addr source handler]
     (.blockMulticastGroup socket multi-addr source (core/as-async-result-handler handler)))
  ([socket multi-addr addr source handler]
     (.blockMulticastGroup socket multi-addr addr source (core/as-async-result-handler handler))))

(defn on-close
  "TODO:comments"
  ([socket]
     (.close socket))
  ([socket handler]
     (.close socket (core/as-async-result-handler handler))))


(defn data
  "Return the data from DatagramPacket in buffer"
  [packet]
  (.data packet))

(defn sender
  "Return a map which contain port and host that sending this packet"
  [packet]
  (let [inet (.sender packet)]
    {:port (.getPort inet)
     :host (.getHostString inet)}))
