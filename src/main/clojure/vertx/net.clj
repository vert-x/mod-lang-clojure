(ns vertx.net
  (:import (org.vertx.java.core.streams Pump))
  (:use [vertx.core]))

(defn pump
  ([sock1 sock2]
     (Pump/createPump sock1 sock2))
  ([sock1 sock2 start]
     (let [p (pump sock1 sock2)]
       (if start
         (.start p)
         p))))

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
