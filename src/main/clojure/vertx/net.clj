(ns vertx.net
  (:import (org.vertx.java.core.streams Pump)
           (org.vertx.java.core.buffer Buffer)
           (org.vertx.java.core.parsetools RecordParser)
           (org.vertx.java.core Vertx Handler AsyncResultHandler))
  (:use [vertx.core]))

(defn pump
  ([sock1 sock2]
     (Pump/createPump sock1 sock2))
  ([sock1 sock2 start]
     (if start
       (.start (pump sock1 sock2))
       (pump sock1 sock2))))


(defn parse-fixed [in size h] 
  (-> (RecordParser/newFixed size h) (.handle in)))

(defn parse-delimited
  "Parse ```Buffer``` with specific limiter then invoke handler"
  [in ^String limite h]
  (-> (RecordParser/newDelimited (.getBytes limite) h) (.handle in)))


(defmacro sock-listen
  "Create a ```NetServer``` and listen on specified port and host"
  [port host & body]
  `(let [vertx# (get-vertx)
         sock-server# (.createNetServer vertx#)]
     ((fn [~'sock-server] ~@body) sock-server#)
     (.listen sock-server# ~port ~host)))

(defmacro connect-handler
  "Used by a ```NetServer```, handle new connection."
  [sock-server expr & body]
  `(.connectHandler ~sock-server (handler ~expr ~@body)))

(defmacro close-handler
  "Used by a ```NetServer```, handle connection close."
  [sock expr & body]
  `(.closeHandler ~sock (handler ~expr ~@body)))

(defmacro data-handler
  "Create a handler and attach it the ```.dataHandler``` callback of the first argument,
   usually a socket"
  [sock expr & body]
  `(.dataHandler ~sock (handler ~expr ~@body)))

(defmacro sock-connect
  "Create a ```NetClient```, and connect specified port and host."
  [port host & body]
  `(let [vertx# (get-vertx)
         client# (.createNetClient vertx#)]
     (.connect client# ~port ~host ~@body)))

(defmacro exception-handler
  "Catch exception from socket"
  [sock e & body]
  `(.exceptionHandler ~sock (handler ~e ~@body)))
