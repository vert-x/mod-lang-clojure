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
  (:import (org.vertx.java.core.sockjs EventBusBridgeHook SockJSServer SockJSSocket)
           org.vertx.java.core.impl.DefaultFutureResult
           (org.vertx.java.core.json JsonArray JsonObject)))

(defn sockjs-server
  "Create a SockJS server that wraps an HTTP server."
  [http-server]
  (-> (core/get-vertx) (.createSockJSServer http-server)))

(defn remote-address
  "Returns the remote address for the socket as an address-map of the
  form {:address \"127.0.0.1\" :port 8888 :basis inet-socket-address-object}"
  [^SockJSSocket socket]
  (u/inet-socket-address->map (.remoteAddress socket)))

(defn install-app
  "Installs a SockJS application.
   When the server receives a SockJS request that matches the
   configured :prefix, it creates an instance of SockJSSocket and
   passes it to the connect handler. handler can either be a
   single-arity fn or a Handler instance that will be passed the
   socket. Returns the server instance.

   config can contain the following values:

   * :prefix - A url prefix for the application. All http requests whose
     paths begins with selected prefix will be handled by the
     application. This property is mandatory.
   * :insert_JSESSIONID - Some hosting providers enable sticky sessions
     only to requests that have JSESSIONID cookie set. This setting
     controls if the server should set this cookie to a dummy value. By
     default setting JSESSIONID cookie is enabled. More sophisticated
     behaviour can be achieved by supplying a function.
   * :session_timeout - The server sends a close event when a client
     receiving connection have not been seen for a while. This delay is
     configured by this setting. By default the close event will be
     emitted when a receiving connection wasn't seen for 5 seconds.
   * :heartbeat_period - In order to keep proxies and load balancers
     from closing long running http requests we need to pretend that the
     connecion is active and send a heartbeat packet once in a
     while. This setting controlls how often this is done. By default a
     heartbeat packet is sent every 5 seconds.
   * :max_bytes_streaming - Most streaming transports save responses on
     the client side and don't free memory used by delivered
     messages. Such transports need to be garbage-collected once in a
     while. :max_bytes_streaming sets a minimum number of bytes that can
     be send over a single http streaming request before it will be
     closed. After that client needs to open new request. Setting this
     value to one effectively disables streaming and will make streaming
     transports to behave like polling transports. The default value is
     128K.
   * :library_url - Transports which don't support cross-domain
     communication natively ('eventsource' to name one) use an iframe
     trick. A simple page is served from the SockJS server (using its
     foreign domain) and is placed in an invisible iframe. Code run from
     this iframe doesn't need to worry about cross-domain issues, as it's
     being run from domain local to the SockJS server. This iframe also
     does need to load SockJS javascript client library, and this option
     lets you specify its url (if you're unsure, point it to the latest
     minified SockJS client release, this is the default). The default
     value is http://cdn.sockjs.org/sockjs-0.3.4.min.js"
  [^SockJSServer server config handler]
  (.installApp server (u/encode config) (core/as-handler handler)))

(defn bridge
  "Install an app which bridges the SockJS server to the event bus.
   app-config is a map of configuration options (see install-app).
   inbound-permitted and outbound-permitted are lists of JSON objects
   which define permitted matches for inbound (client->server) and
   outbound (server->client) traffic, respectively. See the \"Securing
   the Bridge\" section of the manual for the proper usage of these
   options. bridge-config is a map of bridge specific configuration
   options [default]:

   * :auth_address             the address of the auth manager
                               [vertx.basicauthmanager.authorise]
   * :auth_timeout             the amount of time (in ms) an
                               authorisation will be cached in the
                               bridge [5 minutes]
   * :max_address_length       the maximum length (in characters) an
                               address a client attempts to register
                               a handler against can be [200]
   * :max_handlers_per_socket  the maximum number of handlers a single
                               client can register [1000]
   * :ping_interval            the interval (in ms) between pings to 
                               confirm the client is still available
                               [10000]"
  ([server app-config inbound-permitted outbound-permitted]
     (bridge server app-config inbound-permitted outbound-permitted {}))
  ([^SockJSServer server app-config inbound-permitted outbound-permitted bridge-config]
     (.bridge server
       ^JsonObject (u/encode app-config)
       ^JsonArray (u/encode inbound-permitted)
       ^JsonArray (u/encode outbound-permitted)
       ^JsonObject (u/encode bridge-config))))


(defn- eb-bridge-hook
  "Make a implememtion of EventBusBridgeHook, take a kv pair as handlers."
  [hooks]
  (letfn [(call-hook [key-fn & args]
            (if-let [f (key-fn hooks)]
              (boolean (apply f args))
              true))]
    (reify EventBusBridgeHook
      (handleSocketCreated [_ sock]
        (call-hook :created sock))

      (handleSocketClosed [_ sock]
        (call-hook :closed sock))

      (handleSendOrPub [_ sock is-send msg address]
        (call-hook
          (if is-send :send :publish)
          sock msg address))

      (handleAuthorise [_ message session-id handler]
        (call-hook #(:authorise % (:authorize %))
          (u/decode message)
          session-id
          (fn [pass]
            (.setHandler (DefaultFutureResult. ^Boolean (boolean pass))
              handler))))

      (handlePreRegister [_ sock address]
        (call-hook :pre-register sock address))

      (handlePostRegister [_ sock address]
        (call-hook :post-register sock address))

      (handleUnregister [_ sock address]
        (call-hook :unregister sock address)))))

(defn set-hooks
  "Registers functions to be called when certain events occur on an event bus bridge.
   Takes the following keyword arguments:

   :created       Called when the socket is created. The fn will be
                  passed SockJSSocket. The fn must return truthy for the creation
                  to be allowed.
   :closed        Called when the socket has been closed. The fn will be
                  passed the SockJSSocket
   :send          Called when the clent is sends data. The fn will be
                  passed SockJSSocket, the message, and the eventbus
                  address. The fn must return truthy for the send to be
                  allowed.
   :publish       Called when the clent is publishes data. The fn will be
                  passed SockJSSocket, the message, and the eventbus
                  address. The fn must return truthy for the publish to be
                  allowed.
   :authorise     Called when an authorisation message is received. Can be
                  used to override the default mechanism. The fn will
                  be passed the auth request as a map, the session id,
                  and a single-arity fn that should be called with the
                  truthy/falsey auth result (allowing you to do the
                  auth asynchronously). This function must return
                  truthy if it intends to handle the auth request.
   :pre-register  Called before a client handler registration is processed.
                  The fn will be passed the SockJSSocket and the address.
                  The fn must return truthy for the registration to be
                  allowed.
   :post-register Called after a client handler registration is processed.
                  The fn will be passed the SockJSSocket and the address.
   :unregister    Called before a client handler unregistration is processed.
                  The fn will be passed the SockJSSocket and the address.
                  The fn must return truthy for the unregistration to be
                  allowed.

  Returns the server. Calling set-hooks more than once will overwrite
  the hooks set previously."
  [^SockJSServer server & {:as hooks}]
  (.setHook server (eb-bridge-hook hooks)))
