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
  "Provides a broad set of functions for creating HTTP servers and
   clients, and handling requests."
  (:refer-clojure :exclude [get])
  (:require [clojure.string :as string]
            [vertx.buffer :as buf]
            [vertx.core :as core]
            [vertx.net :as net]
            [vertx.utils :as u])
  (:import org.vertx.java.core.buffer.Buffer
           org.vertx.java.core.MultiMap
           [org.vertx.java.core.http
            CaseInsensitiveMultiMap
            HttpClient HttpClientRequest HttpClientResponse
            HttpServer HttpServerFileUpload HttpServerRequest HttpServerResponse]))

(defn- parse-multi-map
  "Converts a vertx MultiMap into a clojure map, converting keys to
  keywords and Arrays into vectors."
  [^MultiMap multi-map]
  (into {} (for [^String name (.names multi-map)]
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
      (if (instance? CharSequence v)
        (.set multi-map (name k) ^CharSequence v)
        (.set multi-map (name k) ^Iterable v)))
    multi-map))

(defn server
  "Creates a HTTP or HTTPS server (HttpServer) instance from vertx.core/*vertx*.
   properties is a map of properties to set on the newly created
   server instance. They are translated into .setXXX calls by
   camel-casing the keyword key. Example: {:key-store-path
   \"/some/path\"} will trigger a call to .setKeyStorePath on the
   server object. See the documentation for
   org.vertx.java.core.http.HttpServer for a full list of properties."
  ([]
     (server nil))
  ([properties]
     (u/set-properties (.createHttpServer (core/get-vertx))
                       (net/normalize-ssl-props properties))))

(defn listen
  "Tells the http-server to start listening for connections on port.
   If host is not provided, it defaults to \"0.0.0.0\". handler can
   either be a two-arity fn that will be passed the exception-map (if any)
   and server from the result of the listen call, or a Handler
   instance that will be called with the AsyncResult object that wraps
   the exception and server. Returns the server instance.

   Be aware this is an async operation and the server may not bound on
   return of the function."
  ([server port]
     (listen server port nil nil))
  ([server port host]
     (listen server port host nil))
  ([^HttpServer server port host handler]
     (.listen server port
              (or host "0.0.0.0")
              (core/as-async-result-handler handler))))

(defn on-request
  "Attaches a request handler to the server.
   As HTTP requests are received by the server, instances of
   HttpServerRequest will be created and passed to this
   handler. handler can either be a single-arity fn or a Handler
   instance that will be passed the request object. Returns the server
   instance."
  [^HttpServer server handler]
  (.requestHandler server (core/as-handler handler)))

(defn expect-multi-part
  "Call this if you are expecting a multi-part form to be submitted in
   the request. This must be called before the body of the request has
   been received if you intend to call form-attributes."
  [^HttpServerRequest req]
  (.expectMultiPart req true))

;;public
(defn close
  "Close the server. Any open HTTP connections will be closed."
  ([server]
     (close server nil))
  ([^HttpServer server handler]
     (.close server (core/as-async-result-handler handler false))))

;; HttpServerRequest
(defn version
  "Reads the HTTP version from the request object as a String of the form \"HTTP/1.1\"."
  [^HttpServerRequest req]
  (let [str-vec (string/split (.name (.version req)) #"_")]
    (str (first str-vec) "/" (second str-vec) "." (last str-vec))))

(defn request-method
  "Reads the HTTP request method from the request object as a keyword of the form :GET."
  [^HttpServerRequest req] (keyword (.method req)))

(defn params
  "Returns a map of parameters for the request.
   Keys will be keywords, and keys that have multiple values in the
   params will have those values stored as a vector."
  [^HttpServerRequest req]
  (parse-multi-map (.params req)))

(defn form-attributes
  "Returns a map of form-attributes for the request.
   Keys will be keywords, and keys that have multiple values in the
   attributes will have those values stored as a vector.  You will
   need to call expect-multi-part on the request *before* reading the
   body in order to use form-attributes."
  [^HttpServerRequest req]
  (parse-multi-map (.formAttributes req)))

(defprotocol RequestResponseFunctions
  (-headers [this])
  (-add-header [this key value])
  (-trailers [this])
  (-on-body [this handler]))

(extend-protocol RequestResponseFunctions
  HttpServerRequest
  (-headers [req]
    (parse-multi-map (.headers req)))
  (-on-body [req handler]
    (.bodyHandler req (core/as-handler handler)))

  HttpClientRequest
  (-headers [req]
    (parse-multi-map (.headers req)))
  (-add-header [req ^String key ^String value]
    (.putHeader req key value))

  HttpServerResponse
  (-headers [resp]
    (parse-multi-map (.headers resp)))
  (-add-header [resp ^String key ^String value]
    (.putHeader resp key value))
  (-trailers [resp]
    (parse-multi-map (.trailers resp)))

  HttpClientResponse
  (-headers [resp]
    (parse-multi-map (.headers resp)))
  (-trailers [resp]
    (parse-multi-map (.trailers resp)))
  (-on-body [resp handler]
    (.bodyHandler resp (core/as-handler handler))))

(defn headers
  "Returns a map of headers for the server request, server response, or client response.
   Keys will be keywords, and keys that have multiple values in the
   headers will have those values stored as a vector."
  [req-or-resp]
  (-headers req-or-resp))

(defn trailers
  "Returns a map of trailers for the server response or client response.
   Keys will be keywords, and keys that have multiple values in the
   trailers will have those values stored as a vector."
  [req-or-resp]
  (-trailers req-or-resp))

(defn on-body
  "Attach a handler to receive the entire body in one piece.
   http can either be a server request or client response.  handler
   can either be a single-arity fn or a Handler instance that will be
   passed the body as a buffer. This saves the user having to manually
   set a data and end handler and append the chunks of the body until
   the whole body received.  Don't use this if your request body is
   large - you could potentially run out of RAM. Returns the given
   server request or client response."
  [req-or-resp handler]
  (-on-body req-or-resp handler))

(defn add-header
  "Sets an HTTP header on a client request or server response.
   Key can be a string, keyword, or symbol. The latter two will be
   converted to a string via name. Returns the request or response."
  [req-or-resp key value]
  (-add-header req-or-resp (name key) (name value)))

(defn add-headers
  "Sets a HTTP headers on a client request or server response.
   Keys can be strings, keywords, or symbols. The latter two will be
   converted to strings via name. Returns the request or response."
  [req-or-resp headers]
  (doseq [[k v] headers]
    (add-header req-or-resp k v))
  req-or-resp)

(defn add-trailer
  "Sets an HTTP trailer on a server response.
   Key can be a string, keyword, or symbol. The latter two will be
   converted to a string via name. Returns the request or response."
  [^HttpServerResponse resp key value]
  (.putTrailer resp (name key) (name value)))

(defn add-trailers
  "Sets a HTTP trailers on a client request or server response.
   Keys can be strings, keywords, or symbols. The latter two will be
   converted to strings via name. Returns the request or response."
  [req-or-resp trailers]
  (doseq [[k v] trailers]
    (add-trailer req-or-resp k v))
  req-or-resp)

(defn remote-address
  "Returns the local address for the request as an address-map of the
  form {:address \"127.0.0.1\" :port 8888 :basis inet-socket-address-object}"
  [^HttpServerRequest req]
  (u/inet-socket-address->map (.remoteAddress req)))

(defn server-response
  "Creates a response object for the given request object.
   Properties is a map of options for the server instance. They are
   translated into .setXXX calls by camel-casing the keyword
   key. Example: {:status-code 418} will trigger a call to
   .setStatusCode on the response object. See the documentation for
   org.vertx.java.core.http.HttpServerResponse for a full list of
   properties."
  ([req]
     (server-response req nil))
  ([^HttpServerRequest req properties]
     (u/set-properties (.response req) properties)))

(defn upload-file-info
  "Takes an HttpServerFileUpload object, and returns a map of properties about that object.

   The properties are:
   * :filename     - the name of the file
   * :name         - the name of the upload attribute
   * :content-type - the content-type specified in the upload
   * :encoding     - the content transfer encoding
   * :size         - the size of the file in bytes
   * :charset      - the Charset as a String
   * :basis        - the original file object, which is also a ReadStream
   * :save-fn      - a single-arity fn that can be passed a path to save the file to disk"
  [^HttpServerFileUpload file]
  {:filename (.filename file)
   :name (.name file)
   :content-type (.contentType file)
   :encoding (.contentTransferEncoding file)
   :charset (.name (.charset file))
   :size (.size file)
   :basis file
   :save-fn (fn [path] (.streamToFileSystem file path))})

(defn on-upload
  "Set the upload handler on a server request.
   The handler will get notified once a new file upload was received
   and so allow to get notified by the upload in progress. handler can
   either be a single-arity fn that will be passed a map of properties
   of the uploaded file, or a Handler that will be called with the raw
   HttpServerFileUpload object. See upload-file-info for more
   information on the file properties. Returns the request."
  [^HttpServerRequest req handler]
  (.uploadHandler req (core/as-handler handler upload-file-info)))

(defn send-file
  "Stream a file directly from disk to the outgoing connection.
   This bypasses userspace altogether where supported by
   the underlying operating system. This is a very efficient way to
   serve files. Takes the following keyword arguments:

   :not-found  The path to a resource to serve if filename is
               not found. If not-found is not specified, a standard
               404 responses is generated.
   :handler    can either be a single-arity fn that will be passed the
               exception-map (if any) from the result of the send-file
               call, or a org.vertx.java.core.Handler that will be
               called with the AsyncResult object that wraps the
               exception

   Returns the response object."
  [^HttpServerResponse resp filename & {:keys [not-found handler]}]
  (.sendFile resp filename not-found
    (core/as-async-result-handler handler false)))

(defprotocol End
  (-end [this] [this content] [this content enc]))

(extend-protocol End
  HttpClientRequest
  (-end
    ([this]
       (.end this))
    ([this content]
       (.end this ^Buffer content))
    ([this content enc]
       (.end this content enc)))
  HttpServerResponse
  (-end
    ([this]
       (.end this))
    ([this content]
       (.end this ^Buffer content))
    ([this content enc]
     (.end this content enc))))

(defn end
  "Ends the server request or client response.
  Writes the given content to the request or response before ending,
  using the same rules as vertx.stream/write. If no data has been
  written to the response body, the actual response won't get written
  until this method gets called. Once the response has ended, it
  cannot be used any more."
  ([req-or-resp]
     (-end req-or-resp))
  ([req-or-resp content]
     (-end req-or-resp (buf/as-buffer content)))
  ([req-or-resp content enc]
     (-end req-or-resp content enc)))

;;HttpClient

(defn client
  "Creates a HTTP or HTTPS client (HttpClient) instance from vertx.core/*vertx*.
   properties is a map of properties to set on the newly created
   client instance. They are translated into .setXXX calls by
   camel-casing the keyword key. Example: {:key-store-path
   \"/some/path\"} will trigger a call to .setKeyStorePath on the
   client object. See the documentation for
   org.vertx.java.core.http.HttpClient for a full list of properties."
  ([]
     (client nil))
  ([properties]
     (u/set-properties (.createHttpClient (core/get-vertx))
                       (net/normalize-ssl-props properties))))


(defn request
  "Creates an HttpClientRequest object for the given HTTP method.
   method should be one of: :OPTIONS, :GET, :HEAD, :POST, :PUT,
   :DELETE, :TRACE, :CONNECT, or :PATCH. uri is the relative portion
   of the url to be requested. If a full url is provided, the host and
   port will be ignored, using the host and port set on the client
   object instead. handler can either be a single-arity fn or a
   Handler instance that will be passed the HttpClientResponse.  The
   request won't be issued until ended with a call to the end
   function."  [^HttpClient client method uri handler]
  (.request client (string/upper-case (name method)) uri
            (core/as-handler handler)))

(defn get-now
  "Creates a GET HttpClientRequest object.
   uri is the relative portion of the url to be requested. If a full
   url is provided, the host and port will be ignored, using the host
   and port set on the client object instead. handler can either be a
   single-arity fn or a Handler instance that will be passed the
   HttpClientResponse. This is a quick version of the get function,
   used when you don't need to write a body to the request. This
   request is immediately issued, and the end function need not be
   called."
  ([client uri handler]
     (get-now client uri nil handler))
  ([^HttpClient client uri headers handler]
     (.getNow client uri
              (encode-headers headers)
              (core/as-handler handler))))

(defmacro ^:private def-request-fn [name]
  (let [doc (format "Creates a %s HttpClientRequest object.
    uri is the relative portion of the url to be requested. If a full url is
    provided, the host and port will be ignored, using the host and
    port set on the client object instead. handler can either be a
    single-arity fn or a Handler instance that will be passed the
    HttpClientResponse. The request won't be issued until ended with
    a call to the end function."
                    (string/upper-case name))
        method (symbol (str "." name))
        client (with-meta 'client {:tag 'HttpClient})]
    `(defn ~name ~doc
       [~client ~'uri ~'handler]
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

(defn on-continue
  "Registers a continue handler on a HttpClientRequest.
   handler can either be a zero-arity fn or a Handler instance that
   will be called when the server is ready to continue.  If you send
   an HTTP request with the Expect header set to the value
   100-continue and the server responds with an interim HTTP response
   with a status code of 100, then the handler will be called.  You
   can then continue to write data to the request body and later end
   it."
  [^HttpClientRequest req handler]
  (.continueHandler req (core/as-void-handler handler)))
