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
  "Functions for operating on Vert.x Buffers.
   A Buffer represents a sequence of zero or more bytes that can be
   written to or read from, and which expands as necessary to
   accommodate any bytes written to it.

   append!, set! and as-buffer take several different types of data
   that can be written to a buffer these types are referred to
   collectively as \"bufferable\", and are:

   * Buffer
   * Byte
   * byte[]
   * Double
   * BigDecimal (coerced to a Double)
   * Ratio (coerced to a Double)
   * Float
   * Integer
   * Long
   * BigInt (coerced to a Long)
   * Short
   * String"
  (:require [vertx.core :as core])
  (:import [org.vertx.java.core.buffer Buffer]
           [org.vertx.java.core.parsetools RecordParser]
           [clojure.lang BigInt Ratio]
           java.math.BigDecimal
           java.nio.ByteBuffer))

(defn buffer
  "Creates a new Buffer instance.

   arg can be:
   * a String, which will be written to the buffer as UTF-8
   * a byte[], which will be written to the buffer
   * an int, which specifies an initial size hint

   You can also provide a String along with a second argument
   specifying the encoding."
  ([]
     (Buffer.))
  ([arg]
     (Buffer. arg))
  ([str enc]
     (Buffer. str enc)))

(let [byte-arr (Class/forName "[B")]
  (defn append!
    "Appends bufferable data to the end of a buffer.
     Returns the mutated buffer instance."
    ([buf data]
       (condp instance? data
         Buffer     (.appendBuffer buf data)
         Byte       (.appendByte buf data)
         byte-arr   (.appendBytes buf data)
         Double     (.appendDouble buf data)
         BigDecimal (.appendDouble buf (double data))
         Ratio      (.appendDouble buf (double data))
         Float      (.appendFloat buf data)
         Integer    (.appendInt buf data)    
         Long       (.appendLong buf data)
         BigInt     (.appendLong buf (long data))
         Short      (.appendShort buf data)
         String     (.appendString buf data)
         (throw (IllegalArgumentException.
                 (str "Can't append data of class " (class data))))))
    ([buf data-string encoding]
       (.append data-string encoding)))

  (defn set!
    "Sets bufferable data in a buffer.
     The data is set at the offset specified by loc."
    ([buf loc data]
       (condp instance? data
         Buffer     (.setBuffer buf loc data)
         Byte       (.setByte buf loc data)
         byte-arr   (.setBytes buf loc data)
         ByteBuffer (.setBytes buf loc data)
         Double     (.setDouble buf loc data)
         BigDecimal (.setDouble buf loc (double data))
         Ratio      (.setDouble buf loc (double data))
         Float      (.setFloat buf loc data)
         Integer    (.setInt buf loc data)    
         Long       (.setLong buf loc data)
         BigInt     (.setLong buf loc (long data))
         Short      (.setShort buf loc data)
         String     (.setString buf loc data)
         (throw (IllegalArgumentException.
                 (str "Can't set data of class " (class data))))))
    ([buf loc data-string encoding]
       (.setString buf loc data-string encoding))))

(defn as-buffer
  "Wraps bufferable data in a buffer unless it is already one."
  [data]
  (if (or (nil? data) (instance? Buffer data))
    data
    (doto (buffer)
      (append! data))))

(defn get-buffer
  "Returns a copy of a sub-sequence of buf as a Buffer starting
   at start and ending at end - 1."
  [buf start end]
  (.getBuffer buf start end))

(defmacro ^:private def-get [name len]
  (let [fname (symbol (str "get-" name))
        doc (format "Returns the %s at position pos in buf.\n  Throws IndexOutOfBoundsException if the specified pos is less than 0\n  or pos + %s is greater than the length of buf." name len)
        method (symbol (str ".get" (clojure.string/capitalize name)))]
    `(defn ~fname ~doc [~'buf ~'pos] (~method ~'buf ~'pos))))

(def-get byte 1) 
(def-get int 4)
(def-get long 8)
(def-get double 8)
(def-get float 4)
(def-get short 2)

(defn get-bytes
  "Returns a copy of all or a portion of buf as a java byte array.
   If start and end are provided, it returns a copy of a sub-sequnce
   starting at start and ending at end - 1, otherwise it returns a
   copy of the entire buf."
  ([buf]
     (.getBytes buf))
  ([buf start end]
     (.getBytes buf start end)))

(defn get-string
  "Returns a copy of a sub-sequence the buf as a String starting at
   start and ending at end - 1 in the given encoding (defaulting to
   UTF-8)."
  ([buf start end]
     (.getString buf start end))
  ([buf start end encoding]
     (.getString buf start end encoding)))

(defn fixed-parser
  "Creates a fixed-size RecordParser.
   A fixed-size parser can be used to parse protocol data that may be
   delivered across many buffers. For example, a fixed-size parser
   with a size of 4 would take:

   buffer1:1234567
   buffer2:8
   buffer3:90123456

   and invoke the handler four times with:

   buffer1:1234
   buffer2:5678
   buffer3:9012
   buffer4:3456

   handler can either be a single-arity fn or a Handler instance that
   will be passed the Buffer for each parsed fragment. See
   org.vertx.java.core.parsetools.RecordParser for more details."
  [size handler]
  (RecordParser/newFixed size (core/as-handler handler)))

(defn delimited-parser
  "Creates a delimited RecordParser.
   A delimited parser can be used to parse protocol data that may be
   delivered across many buffers. For example, a delimited parser
   with a delimiter of \"\\n\" would take:

   buffer1:Hello\\nHow are y
   buffer2:ou?\\nI am
   buffer3:fine.
   buffer4:\\n

   and invoke the handler three times with:

   buffer1:Hello
   buffer2:How are you?
   buffer3:I am fine.
   
   handler can either be a single-arity fn or a Handler instance that
   will be passed the Buffer for each parsed fragment. See
   org.vertx.java.core.parsetools.RecordParser for more details."
  [delim handler]
  (RecordParser/newDelimited (.getBytes delim)
                             (core/as-handler handler)))

(defn parse-buffer
  "Parses buf with parser."
  [parser buf]
  (.handle parser buf))

(defn parse-fixed
  "Convience function that creates a fixed-size parser and uses it to parse buf."
  [buf size handler]
  (-> (fixed-parser size handler) (parse-buffer buf)))

(defn parse-delimited
  "Convience function that creates a delimited parser and uses it to parse buf."
  [buf delim handler]
  (-> (delimited-parser delim handler) (.handle buf)))
