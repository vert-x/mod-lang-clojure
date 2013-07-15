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
  (:import [org.vertx.java.core.http WebSocketVersion]
           [org.vertx.java.core.impl CaseInsensitiveMultiMap])
  (:require [vertx.core :as core]
            [vertx.common :as common]
            [vertx.net :as net]
            [clojure.string :as string]
            [vertx.utils :as u]))

(defn- parse-multi-map
  "Converts a vertx MultiMap into a clojure map, converting keys to keywords and Arrays into vectors."
  [multi-map]
  (into {} (for [name (.names multi-map)]
             {(keyword (string/replace name #"\s" "-"))
              (let [vals (.getAll multi-map name)]
                (if (empty? vals)
                  (.get multi-map name)
                  (if (= 1 (count vals))
                    (first vals)
                    (into [] vals))))})))

(defn- encode-headers
  "Encode a clojure map of headers to a vertx MultiMap"
  [header]
  (let [multi-map (CaseInsensitiveMultiMap.)]
    (doseq [[k v] header]
      (.set multi-map (name k) v))
    multi-map))


;;TODO: document properties
(defn server
  "Creates a HTTP or HTTPS server (HttpServer) instance from vertx.core/*vertx*."
  ([]
     (server nil))
  ([properties]
     (u/set-properties (.createHttpServer (core/get-vertx)) properties)))

(defn listen
  "Tells the http-server to start listening for connections on port.
   If host is not provided, it defaults to \"0.0.0.0\". handler can
   either be a two-arity fn that will be passed the exception (if any)
   and server from the result of the listen call, or a Handler
   instance that will be called with the AsyncResult object that wraps
   the exception and server. Returns the server instance.

   Be aware this is an async operation and the server may not bound on
   return of the method."
  ([server port]
     (listen server nil nil))
  ([server port host]
     (listen server port host nil))
  ([server port host handler]
     (.listen server port
              (or host "0.0.0.0")
              (core/as-async-result-handler handler))))

(defn on-request
  "Set the request handler for the server to requestHandler.
   As HTTP requests are received by the server, instances of HttpServerRequest
   will be created and passed to this handler."
  [server handler]
  (.requestHandler server (core/as-handler handler)))

(defn on-websocket
  "Set the websocket handler for the server to wsHandler.
   If a websocket connect handshake is successful a
   new ServerWebSocket instance will be created and passed to the handler."
  [server handler]
  (.websocketHandler server (core/as-handler handler)))

;;public
(defn close
  "Close the server. Any open HTTP connections will be closed."
  ([server]
     (close server nil))
  ([server handler]
     (common/internal-close server handler)))

;; HttpServerRequest
(defn version
  "Convert HTTP Enum to String e.g:HTTP_1_1 -> HTTP/1.1"
  [req]
  (let [str-vec (string/split (.name (.version req)) #"_")]
    (str (first str-vec) "/" (second str-vec) "." (last str-vec))))

(defn method
  "Return method of HTTP in keywoed e.g GET -> :GET"
  [req] (keyword (.method req)))

(defn uri [req] (.uri req))

(defn path [http] (.path http))

(defn query [http] (.query http))

(defn params
  "Returns a map of all the parameters in the request of uri, suit to GET"
  [req] (parse-multi-map (.params req)))

(defn form-attributes
  "Returns a map of all the paramemters in the request of body, suit to form-POST"
  [req] (parse-multi-map (.formAttributes req)))

(defn headers
  "Returns a map of header from request or response,
   the value is Vector if key have value more than one"
  [http] (parse-multi-map (.headers http)))

(defn trailers
  "Returns HTTP trailers in map from request or response
   the value is Vector if key have value more than one"
  [http] (parse-multi-map (.trailers http)))

(defn remote-address
  "Return a map which contains {:host 127.0.0.1 :port 5566}"
  [req]
  (let [addr (.remoteAddress req)]
    {:host (.getHostName addr) :port (.getPort addr)}))

(defn certs
  "Return an array of the peer certificates. nil if connection is not SSL."
  [req] (.peerCertificateChain req))

(defn absolute-uri
  "Get the absolute URI corresponding to the the HTTP request"
  [req] (.absoluteURI req))

(defn server-response
  "Represents a server-side HTTP response.
   properties include
   :status-code :status-message :chunked"
  ([req] (server-response req nil))
  ([req properties]
     (u/set-properties (.response req) properties)))

(defn on-body
  "Convenience method for receiving the entire request body in one piece.
   This saves the user having to manually
   set a data and end handler and append the chunks of the body until the whole body received.
   Don't use this if your request body is large - you could potentially run out of RAM."
  [http handler] (.bodyHandler http (core/as-handler handler)))

(defn on-upload
  "Set the upload handler. The handler will get notified once a
   new file upload was received and so allow to
   get notified by the upload in progress."
  [req handler]
  (.uploadHandler req (core/as-handler handler)))


;;netSocket? do we need this method?

(defn put-header
  "name of header is keyword"
  [http key value] (.putHeader http (name key) value))

(defn put-trailer
  [http key value] (.putTrailer http (name key) value))

(defn write
  ([http content]
     (common/internal-write http content))
  ([http content enc]
     (common/internal-write http content enc)))

(defn send-file
  ([resp filename]
     (.sendFile resp filename))
  ([resp filename not-found]
     (.sendFile resp filename not-found)))

(defn end
  ([http]
     (.end http))
  ([http content]
     (.end http content))
  ([http content enc]
     (.end http content enc)))



;;HttpServerFileUpload

(defn upload-file-info
  "Return information that have uploaded the file in map
   :filename :name :content-type :encoding :size :charset
  "
  [file]
  {:filename (.filename file)
   :name (.name file)
   :content-type (.contentType file)
   :encoding (.contentTransferEncoding file)
   :charset (.name (.charset file))
   :size (.size file)})

(defn save-upload
  "Stream the content of this upload to the given filename"
  [file path] (.streamToFileSystem file path))


;;ServerWebSocket
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

(defn reject
  "Reject the WebSocket
   Calling this method from the websocketHandler gives you the opportunity to reject
   the websocket, which will cause the websocket handshake to fail by returning
   a 404 response code.
   You might use this method, if for example you only want to accept websockets
   with a particular path."
  [ws]
  (.reject ws))


;;HttpClient

;;exception handler could use it with stream/on-exception

;;TODO: document properties
(defn client
  "Creates a HTTP or HTTPS client (HttpClient) instance.
   If vertx is not provided, it defaults to the default
   vertx (vertx.core/*vertx*)."
  ([]
     (client nil))
  ([properties]
     (u/set-properties (.createHttpClient (core/get-vertx)) properties)))

(defn connect-ws
  ([http-client uri h]
     (connect-ws http-client uri nil nil h))
  ([http-client uri version h]
     (connect-ws http-client uri version nil h))
  ([http-client uri version header h]
     (.connectWebsocket http-client uri (ws-version version)
                        (encode-headers header) (core/as-handler h))))

(defn write-binary-frame
  "write data to websocket as a binary frame"
  [ws data]
  (.writeBinaryFrame ws data))

(defn write-text-frame
  "write data to websocket as a text frame"
  [ws data]
  (.writeTextFrame ws data))

(defn request
  "The specific HTTP method (e.g. GET, POST, PUT etc),
   in clojure we use key word instead of string (e.g. :GET :POST)
   the methods including
   :OPTIONS :GET :HEAD :POST :PUT :DELETE :TRACE :CONNECT :PATCH"
  [http-client method uri resp-h]
  (.request http-client (string/upper-case (name method)) uri
            (core/as-handler resp-h)))

(defn get-now
  ([http-client uri resp-h]
     (get-now http-client uri nil resp-h))
  ([http-client uri header resp-h]
     (.getNow http-client uri
              (encode-headers header)
              (core/as-handler resp-h))))


;;HttpClientRequest

;;TODO: properties documents
(defn request-prop
  "Setting properties for http client request."
  [client-request prop]
  (u/set-properties client-request prop))


(defn on-continue
  " If you send an HTTP request with the header set to the value 100-continue
    and the server responds with an interim HTTP response with a status code of 100
    and a continue handler has been set using this method, then the handler will be called.
    You can then continue to write data to the request body and later end it. "
  [client-req handler]
  (.continueHandler client-req (core/as-handler handler)))

(defn send-head
  " Forces the head of the request to be written before end is called on the request or
    any data is written to it. This is normally used to implement HTTP 100-continue handling."
  [client-req] (.sendHead client-req))

;;HttpClientResponse

(defn cookies
  "return The Set-Cookie headers (including trailers)"
  [client-resp] (.cookies client-resp))

(defn status-code [resp] (.statusCode resp))
(defn status-msg [resp] (.statusMessage resp))
