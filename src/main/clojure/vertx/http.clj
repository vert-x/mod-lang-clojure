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
  "TODO: docs"
  (:refer-clojure :exclude [get])
  (:require [clojure.string :as string]
            [vertx.buffer :as buf]
            [vertx.common :as common]
            [vertx.core :as core]
            [vertx.net :as net]
            [vertx.utils :as u])
  (:import [org.vertx.java.core.impl CaseInsensitiveMultiMap]))

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

(defn ^:internal ^:no-doc encode-headers
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

(defn request-method
  "Return method of HTTP in keyword e.g GET -> :GET"
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

(defn upload-file-info
  "Takes an HttpServerFileUpload object, and returns a map of properties about that object.

   The properties are:
   * :filename     - the name of the file
   * :name         - the name of the upload attribute
   * :content-type - the content-type specified in the upload
   * :encoding     - the content transfer encoding
   * :size         - the size of the file in bytes
   * :charset      - the Charset as a String
   * :stream       - the original file object, which is also a ReadStream
   * :save-fn      - a single-arity fn that can be passed a path to save the file to disk"
  [file]
  {:filename (.filename file)
   :name (.name file)
   :content-type (.contentType file)
   :encoding (.contentTransferEncoding file)
   :charset (.name (.charset file))
   :size (.size file)
   :stream file
   :save-fn (fn [path] (.streamToFileSystem file path))})

(defn on-upload
  "Set the upload handler. The handler will get notified once a new
   file upload was received and so allow to get notified by the upload
   in progress. handler can either be a single-arity fn that will be
   passed a map of properties of the uploaded file, or a Handler that
   will be called with the raw HttpServerFileUpload object. See
   upload-file-info for more information on the file properties."
  [req handler]
  (.uploadHandler req (core/as-handler handler upload-file-info)))


(defn add-header
  "Sets an HTTP header on a request or response object.
   Key can be a string, keyword, or symbol. The latter two will be
   converted to a string via name."
  [req-or-resp key value]
  (.putHeader req-or-resp (name key) value))

(defn add-headers
  "Sets a HTTP headers on a request or response object.
   Keys can be strings, keywords, or symbols. The latter two will be
   converted to strings via name."
  [req-or-resp headers]
  (doseq [[k v] headers]
    (add-header req-or-resp k v)))

(defn add-trailer
  "Sets an HTTP trailer on a request or response object.
   Key can be a string, keyword, or symbol. The latter two will be
   converted to a string via name."
  [req-or-resp key value]
  (.putTrailer req-or-resp (name key) value))

(defn add-trailers
  "Sets a HTTP trailers on a request or response object.
   Keys can be strings, keywords, or symbols. The latter two will be
   converted to strings via name."
  [req-or-resp trailers]
  (doseq [[k v] trailers]
    (add-trailer req-or-resp k v)))

(defn send-file
  "TODO: docs"
  ([resp filename]
     (.sendFile resp filename))
  ([resp filename not-found]
     (.sendFile resp filename not-found)))

(defn end
  "TODO: docs"
  ([http]
     (.end http))
  ([http content]
     (.end http (buf/as-buffer content)))
  ([http content enc]
     (.end http content enc)))

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


(defn request
  "The specific HTTP method (e.g. GET, POST, PUT etc),
   in clojure we use key word instead of string (e.g. :GET :POST)
   the methods including
   :OPTIONS :GET :HEAD :POST :PUT :DELETE :TRACE :CONNECT :PATCH"
  [http-client method uri resp-h]
  (.request http-client (string/upper-case (name method)) uri
            (core/as-handler resp-h)))

(defn get-now
  "TODO: docs"
  ([http-client uri resp-h]
     (get-now http-client uri nil resp-h))
  ([http-client uri headers resp-h]
     (.getNow http-client uri
              (encode-headers headers)
              (core/as-handler resp-h))))

(defmacro ^:private def-request-fn [name]
  (let [doc (format "Makes a %s request to uri.
   handler is a single-arity fn that will be called with the HTTP
   response object. Returns the request object."
                    (string/upper-case name))
        method (symbol (str "." name))]
    `(defn ~name ~doc
       [~'client ~'uri ~'handler]
       (~method ~'client ~'uri (core/as-handler ~'handler)))))

(def-request-fn get)
(def-request-fn put)
(def-request-fn post)
(def-request-fn delete)
(def-request-fn head)
(def-request-fn options)
(def-request-fn connect)
(def-request-fn trace)
(def-request-fn patch)

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
