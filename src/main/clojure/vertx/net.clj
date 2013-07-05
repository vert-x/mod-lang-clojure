(ns vertx.net
  (:require [vertx.core :as core]))

(defn server
  "Creates a TCP or SSL server (NetServer) instance.
   If vertx is not provided, it defaults to the default
   vertx (vertx.core/*vertx*)."
  ([]
     (server (core/get-vertx)))
  ([vertx]
     (.createNetServer vertx)))

(defn client
  "Creates a TCP or SSL client (NetClient) instance.
   If vertx is not provided, it defaults to the default
   vertx (vertx.core/*vertx*).

   Multiple connections to different servers can be made using the
   same client instance."
  ([]
     (client (core/get-vertx)))
  ([vertx]
     (.createNetClient vertx)))

(defn listen
  "Tells the server to start listening for connections on port.
   If host is not provided, it defaults to \"0.0.0.0\". handler can
   either be a two-arity fn that will be passed the exception (if any)
   and server from the result of the listen call, or a Handler
   instance that will be called with the AsyncResult object that wraps
   the exception and server. Returns the server instance.

   Be aware this is an async operation and the server may not bound on
   return of the method."
  ([server port]
     (listen server port nil))
  ([server port host]
     (listen server port host nil))
  ([server port host handler]
     (.listen server port
              (or host "0.0.0.0")
              (core/as-async-result-handler handler))))

(defn on-connect 
  "Attaches a connect handler to the server.
   As the server accepts TCP or SSL connections it creates an instance
   of NetSocket and passes it to the connect handler.  handler can
   either be a single-arity fn or a Handler instance that will be
   passed the socket. Returns the server instance.

   The server can only have at most one connect handler at any one
   time. " [server handler]
  (.connectHandler server (core/as-handler handler)))

(defn on-close
  "Attaches a handler to the socket that will be called when the socket is closed.
   handler can either be a zero-arity fn or a Handler instance.
   Returns the socket."
  [socket handler]
  (.closeHandler socket (core/as-void-handler handler)))

(defn connect
  "Attempts to open a connection to a server.
   If host is not provided, it defaults to \"localhost\". If no client
   is provided, one is created by calling the vertx.net/client fn.
   handler can either be a two-arity fn that will be passed the
   exception (if any) and socket from the result of the connect call,
   or a Handler instance that will be called with the AsyncResult
   object that wraps the exception and socket. Returns the client
   instance."
  ([port handler]
     (connect port nil handler))
  ([port host handler]
     (connect (client) port host handler))
  ([client port host handler]
     (.connect client port
               (or host "localhost")
               (core/as-async-result-handler handler))))
