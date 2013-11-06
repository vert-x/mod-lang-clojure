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
  "Provides a broad set of functions for UDP servers and clients.

   Usually you use a Datragram Client to send UDP over the wire. UDP
   is connection-less which means you are not connected to the remote
   peer in a persistent way. Because of this you have to supply the
   address and port of the remote peer when sending data.
 
   You can send data to ipv4 or ipv6 addresses, which also include
   multicast addresses."
  (:refer-clojure :exclude [send])
  (:require [vertx.core :as core :exclude [send]]
            [vertx.buffer :as buf]
            [vertx.common :as common]
            [vertx.utils :as u])
  (:import [java.net InetSocketAddress]
           [ org.vertx.java.core.datagram InternetProtocolFamily]))


(defn- inet-protocol-family
  "convert keyword of protocol to enum, default value is nil."
  [protocol]
  (case protocol
    :ipv4 InternetProtocolFamily/IPv4
    :ipv6 InternetProtocolFamily/IPv6
    nil))

;;TODO: setting properties
(defn socket
  "Creates a datagram socket (DatagramSocket) instance using vertx.core/*vertx*.
   protocol-family can be one of :ipv4, ipv6. properties is a map of
   properties to set on the newly created socket instance. They are
   translated into .setXXX calls by camel-casing the keyword
   key. Example: {:send-buffer-size 1024} will trigger a call
   to .setSendBufferSize on the socket object. See the docuementation
   for org.vertx.java.core.datagram.DatagramSocket for a full list of
   properties."
  ([]
     (socket nil nil))
  ([protocol-family]
     (socket protocol-family nil))
  ([protocol-family properties]
     (u/set-properties
      (.createDatagramSocket (core/get-vertx)
                             (inet-protocol-family protocol-family))
      properties)))

(defn local-address
  "Returns the local address for the socket as an address-map.
   This will be nil if listen hasn't been called for the socket."
  [socket]
  (u/inet-socket-address->map (.localAddress socket)))

(defn send
  "Writes the given bufferable content to the given host & port.
   handler can either be a two-arity fn that will be passed the
   exception-map (if any) and socket from the result of the send
   call, or a Handler instance that will be called with the
   AsyncResult object that wraps the exception and socket. Returns the
   socket instance."
  ([socket content host port]
     (send socket content host port nil))
  ([socket content host port handler]
     (.send socket (buf/as-buffer content) host port
            (core/as-async-result-handler handler))))

(defn listen
  "Makes socket listen to the given port and (optional) host.
   handler can either be a two-arity fn that will be passed the
   exception-map (if any) and socket from the result of the listen
   call, or a Handler instance that will be called with the
   AsyncResult object that wraps the exception and socket. Returns the
   socket instance."
  ([socket port]
     (listen socket port nil nil))
  ([socket port host]
     (listen socket port host nil))
  ([socket port host handler]
     (if host
       (.listen socket host port (core/as-async-result-handler handler))
       (.listen socket port (core/as-async-result-handler handler)))))

(defn join-multicast-group
  "Joins the socket to a multicast group.
   interface and source-address can be used to limit the packets
   received to a particular interface and source address,
   respectively.

   handler can either be a two-arity fn that will be passed the
   exception-map (if any) and socket from the result of the join
   call, or a Handler instance that will be called with the
   AsyncResult object that wraps the exception and socket. Returns the
   socket instance."
  ([socket group-address]
     (join-multicast-group socket group-address nil))
  ([socket group-address handler]
     (.listenMulticastGroup socket group-address
                            (core/as-async-result-handler handler)))
  ([socket group-address interface source-address handler]
     (.listenMulticastGroup socket group-address interface
                            source-address
                            (core/as-async-result-handler handler))))

(defn leave-multicast-group
  "Removes the socket from a multicast group.
   interface and source-address must be used if they were used as part
   of the join.

   handler can either be a two-arity fn that will be passed the
   exception-map (if any) and socket from the result of the leave
   call, or a Handler instance that will be called with the
   AsyncResult object that wraps the exception and socket. Returns the
   socket instance."
  ([socket group-address]
     (leave-multicast-group socket group-address nil))
  ([socket group-address handler]
     (.unlistenMulticastGroup socket group-address
                              (core/as-async-result-handler handler)))
  ([socket group-address addr source handler]
     (.unlistenMulticastGroup socket group-address addr source
                              (core/as-async-result-handler handler))))

(defn block-multicast-sender
  "Blocks packets from the given source-address for the given group.
   interface can be used to limit the block to a specific interface.

   handler can either be a two-arity fn that will be passed the
   exception-map (if any) and socket from the result of the block
   call, or a Handler instance that will be called with the
   AsyncResult object that wraps the exception and socket. Returns the
   socket instance."
  ([socket group-address source-address]
     (block-multicast-sender socket group-address source-address nil))
  ([socket group-address source-address handler]
     (.blockMulticastGroup socket group-address source-address
                           (core/as-async-result-handler handler)))
  ([socket group-address source-address interface handler]
     (.blockMulticastGroup socket group-address interface source-address
                           (core/as-async-result-handler handler))))

(defn on-data
  "Set a data handler on a socket.
   As packets are read, the handler will be called with each packet of
   the form:

     {:sender address-map
      :data data-as-a-buffer
      :basis the-DatagramPacket-object}

   handler can either be a Handler or a single-arity fn.
   Returns the socket."
  [socket handler]
  (letfn [(->map [packet]
            {:sender (u/inet-socket-address->map (.sender packet))
             :data (.data packet)
             :basis packet})]
    (.dataHandler socket
                  (core/as-handler handler ->map))))

(defn close
  "Closes the socket.
   handler can either be a single-arity fn that will be passed the
   exception-map (if any) from the result of the close call, or a
   Handler instance that will be called with the AsyncResult object
   that wraps the exception."
  ([socket]
     (.close socket))
  ([socket handler]
     (common/internal-close socket handler)))
