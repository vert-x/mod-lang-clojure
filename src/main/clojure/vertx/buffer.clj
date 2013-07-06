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

(ns vertx.buffer
  "Functions for operating on Vert.x Buffers"
  (:require [vertx.core :as core])
  (:import [org.vertx.java.core.buffer Buffer]
           [org.vertx.java.core.parsetools RecordParser]
           [clojure.lang BigInt Ratio]
           java.math.BigDecimal))

;; TODO: better docs
(defn buffer
  "Creates a new Buffer instance."
  ([]
     (Buffer.))
  ([arg]
     (Buffer. arg))
  ([str enc]
     (Buffer. str enc)))

(let [byte-array (Class/forName "[B")]
  (defn append!
    "Appends data to a buffer.
     Returns the mutated buffer instance."
    ([buf data]
       (condp instance? data
         Buffer     (.appendBuffer buf data)
         Byte       (.appendByte buf data)
         byte-array (.appendBytes buf data)
         Double     (.appendDouble buf data)
         BigDecimal (.appendDouble buf (double data))
         Ratio      (.appendDouble buf (double data))
         Float      (.appendFloat buf data)
         Integer    (.appendInt buf data)    
         Long       (.appendLong buf data)
         BigInt     (.appendLong buf (long data))
         String     (.appendString buf data)
         (throw (IllegalArgumentException.
                 (str "Can't append data of class " (class data))))))
    ([buf data-string encoding]
       (.append data-string encoding))))

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
