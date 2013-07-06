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

(ns vertx.http
  (:import (org.vertx.java.core.streams Pump)
           (org.vertx.java.core Vertx Handler AsyncResultHandler)
           (org.vertx.java.core.http HttpServerRequest)
           (org.vertx.java.core.net.impl SocketDefaults))
  (:use (vertx core))
  )

(defonce sock-default SocketDefaults/instance)

(defmacro http-listen
  "Create a HttpServer instance that listens on the specified port and host.
   instance of Vertx and HttpServer is available in the body as vertx
   and http-server."
  [vertx port host & body]
  `(let [http-server# (.createHttpServer ~vertx)]
     ((fn [~'vertx ~'http-server] ~@body) ~vertx http-server#)
     (.listen http-server# ~port ~host)))

(defmacro http-route
  "Sinatra like route matching by ```RouterMatcher```, a ```RouterMatcher```
  instance is available for the body."
  [vertx port host & body]
  `(let [http# (.createHttpServer ~vertx)
         router# (RouteMatcher.)]
     ((fn [~'router] ~@body) router#)
     (-> http#
         (.requestHandler router#)
         (.listen ~port ~host))))

(defmacro ws-handler
  "Create a handler for websocket request."
  [http-server expr & body]
  `(.websocketHandler ~http-server
                      (handler ~expr ~@body)))

(defmacro req-handler
  "Create a handler for http request."
  [http-server expr & body]
  `(.requestHandler ~http-server
                    (handler ~expr ~@body)))

(defn params
  "Retrieve the value associated with the key from request parameters."
  [^HttpServerRequest req key]
  (-> req .params (.get key)))

(defn headers
  "Get the headers of the request."
  [^HttpServerRequest req]
  (.headers req))


(defmacro http-connect
  "Create a ```HttpClient```
   the first parameter of vertx is verticle of runtime, you can pass it directly
   second parameter of http-client is callback name after macron has been invoked which
   you can use it in body.
   opts is configuration of parameter for the http client connection"
  [vertx http-client opts & body]
  (let [{:keys [port host timeout max-conns]
         :or  {ssl false
               verify-host true
               key-store-path nil
               key-store-password nil
               trust-store-path nil
               trust-store-password nil
               trust-all false
               accept-backlog 1024
               timeout 60000
               max-conns 1
               }} opts]
    `(let [~http-client (.createHttpClient ~vertx)]
       (doto ~http-client (.setPort ~port) (.setHost ~host)
             (.setMaxPoolSize ~max-conns) (.setConnectTimeout ~timeout))
       ((fn [~'vertx ~'http-client] ~@body) ~vertx ~http-client))

    ))


(defmacro get-now
  "Send a get request and block before the body returned."
  [http-client path content & body]
  `(.getNow ~http-client ~path
            (proxy [Handler] []
              (handle [resp#]
                (.bodyHandler resp#
                              (proxy [Handler] []
                                (handle [data#]
                                  (let [~content data#] ~@body))))))))

(defn end-req
  "Syntax sugar for ending request."
  ([req buf]
     (.end (.response req) buf))
  ([req]
     (.end (.response req))))

(defn end-handler
  "Handler for request end event."
  [req callback]
  (.endHandler req callback))

(defn send-file
  "Http SendFile."
  [req & paths]
  (.sendFile (.response req) (apply str paths)))
