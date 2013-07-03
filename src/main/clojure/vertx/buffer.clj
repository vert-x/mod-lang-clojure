(ns vertx.buffer
  "Functions for operating on Vert.x Buffers"
  (:require [vertx.core :as core])
  (:import [org.vertx.java.core.buffer Buffer]
           [org.vertx.java.core.parsetools RecordParser]))

;; TODO: better docs
(defn buffer
  "Creates a new Buffer object."
  ([arg1]
     (Buffer. arg1))
  ([str enc]
     (Buffer. str enc)))

(defn fixed-parser [size handler] 
  (RecordParser/newFixed size (core/as-handler handler)))

(defn delimited-parser [delim handler]
  (RecordParser/newDelimited (.getBytes delim)
                             (core/as-handler handler)))

(defn parse-buffer [parser in]
  (.handle parser in))

(defn parse-fixed [buff size handler]
  (-> (fixed-parser size handler) (parse-buffer buff)))

(defn parse-delimited
  "Parse ```Buffer``` with specific limiter then invoke handler"
  [buff delim handler]
  (-> (delimited-parser delim handler) (.handle buff)))
