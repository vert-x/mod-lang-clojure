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

(ns vertx.http.sockjs
  "This is an implementation of the server side part of https://github.com/sockjs.

  SockJS enables browsers to communicate with the server using a
  simple WebSocket-like api for sending and receiving messages. Under
  the bonnet SockJS chooses to use one of several protocols depending
  on browser capabilities and what appears to be working across the
  network.

  Available protocols include:

  * WebSockets
  * xhr-polling
  * xhr-streaming
  * json-polling
  * event-source
  * html-file

  This means it should just work irrespective of what browser
  is being used, and whether there are nasty things like proxies and
  load balancers between the client and the server.

  For more detailed information on SockJS, see their website.

  On the server side, you interact using instances of SockJSSocket -
  this allows you to send data to the client or receive data via
  vertx.stream/on-data.

  You can register multiple applications with the same SockJSServer,
  each using different path prefixes, each application will have its
  own handler, and configuration."
  (:require [clojure.string :as string]
            [vertx.utils :as u]
            [vertx.core :as core])
  (:import [org.vertx.java.core.sockjs EventBusBridgeHook]))

(defn sockjs-server
  "Create a SockJS server that wraps an HTTP server."
  [http-server]
  (-> (core/get-vertx) (.createSockJSServer http-server)))

(defn install-app
  "Install an application with a handler that will be called when new SockJS sockets are created
   TODO: better doc"
  [server config handler]
  (.installApp server (u/encode config) (core/as-handler handler)))

(def default-auth-timeout ^:const (* 5 60 1000))
(def default-auth-address ^:const (str "vertx.basicauthmanager.authorise"))

(defn bridge
  "Install an app which bridges the SockJS server to the event bus
   app-config The config for the app
   inboundPermitted A list of JSON objects which define permitted matches for inbound (client->server) traffic
   outboundPermitted A list of JSON objects which define permitted matches for outbound (server->client) traffic
   authTimeout Default time an authorisation will be cached for in the bridge (defaults to 5 minutes)
   authAddress Address of auth manager. Defaults to 'vertx.basicauthmanager.authorise'
   TODO: better doc"
  ([server app-config inbound-permitted outbound-permitted]
     (bridge server app-config inbound-permitted outbound-permitted
             default-auth-timeout default-auth-address))

  ([server app-config inbound-permitted outbound-permitted auth-timeout]
     (bridge server app-config inbound-permitted outbound-permitted auth-timeout
             default-auth-address))

  ([server app-config inbound-permitted outbound-permitted auth-timeout auth-address]
     (.bridge server (u/encode app-config) (u/encode inbound-permitted)
              (u/encode outbound-permitted) auth-timeout auth-address)))

(defn- eb-bridge-hook
  "Make a implememtion of EventBusBridgeHook, take a kv pair as handlers."
  [n-h]
  (reify EventBusBridgeHook
    (handleSocketClosed [_# sock#]
      ((:closed n-h) sock#))

    (handleSendOrPub [_# sock# is-send# msg# address#]
      (if is-send#
        ((:send n-h) sock# msg# address#)
        ((:publish n-h) sock# msg# address#)))

    (handlePreRegister [_# sock# address#]
      ((:pre-register n-h) sock# address#))

    (handlePostRegister [_# sock# address#]
      ((:post-register n-h) sock# address#))

    (handleUnregister [_# sock# address#]
      ((:unregister n-h) sock# address#))))

(defn set-hooks
  "Set a ```EventBusBridgeHook``` to the server.
   name-handlers are six pair of kv, v is implementation of EventBusBridgeHook,
   k is keyworkd and meaning is:
   :closed        The socket has been closed
   :send          Clent is sending
   :publish       Client is publishing
   :pre-register  Called before client registers a handler
   :post-register Called after client registers a handler
   :unregister    Client is unregistering a handler
   TODO: better doc"
  [server & {:as name-handlers}]
  (.setHook server (eb-bridge-hook name-handlers)))
