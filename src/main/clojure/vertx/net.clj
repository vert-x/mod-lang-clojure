(ns vertx.net
  (:import (org.vertx.java.core.streams Pump)
           (org.vertx.java.core Vertx Handler AsyncResultHandler))
  (:use vertx.core)
)

(defn pump
  ([sock1 sock2]
     (Pump/createPump sock1 sock2))
  ([sock1 sock2 start]
     (if start
       (.start (pump sock1 sock2))
       (pump sock1 sock2))))


(defmacro sock-listen
  "Create a ```NetServer``` and listen on specified port and host,
   vertx and sock-server is available in the body."
  [port host & body]
  `(fn [vertx# _#]
     (let [sock-server# (.createNetServer vertx#)]
       ((fn [~'vertx ~'sock-server] ~@body) vertx# sock-server#)
       (.listen sock-server# ~port ~host))))

(defmacro sock-connect
  "Create a ```NetClient```, and connect specified port and host."
  [port host body]
  `(fn [vertx# container#]
     (let [client# (.createNetClient vertx#)]
       (.connect client# ~port ~host ~body))))

(defmacro connect-handler
  "Used by a ```NetServer```, handle new connection."
  [sock-server expr & body]
  `(.connectHandler ~sock-server
                    (handler ~expr ~@body)))

(defmacro data-handler
"``` dataHandler```"
  [sock expr & body]
  `(.dataHandler ~sock
                 (handler ~expr ~@body)))

(defmacro closed-handler
  "Used by a ```NetServer```, handle connection close."
  [sock expr & body]
  `(.closedHandler ~sock
                  (handler ~expr ~@body)))

;;TODO exception Handler





