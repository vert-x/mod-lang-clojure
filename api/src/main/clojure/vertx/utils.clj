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

(ns ^:internal ^:no-doc vertx.utils
    "Internal utility functions."
    (:require [clojure.string :as s])
    (:import [org.vertx.java.core.json JsonArray JsonObject]
             [clojure.lang BigInt IPersistentMap Ratio Seqable IPersistentVector
              IPersistentList IPersistentSet IPersistentCollection Associative Keyword ISeq]
             [java.util Map UUID List Map$Entry]
             [java.net NetworkInterface InetAddress InetSocketAddress]
             java.math.BigDecimal))


(defprotocol Encodeable
  (encode [data]))

(defn- put
  ([^JsonObject jobj ^Map$Entry e]
     (put jobj (.getKey e) (.getValue e)))
  ([^JsonObject jobj k v]
     (doto jobj (.putValue (name k) (encode v)))))

(defn- map->JsonObject [data]
  (reduce #(put %1 (first %2) (second %2))
    (JsonObject.)
    (seq data)))

(defn- java-map->JsonObject [^Map data]
  (reduce #(put %1 %2)
    (JsonObject.)
    (seq data)))

(defn- seq->JsonArray [data]
  (reduce #(doto ^JsonArray %1 (.add (encode %2)))
    (JsonArray.)
    data))

(defn- encode-collection [data]
  ((condp instance? data
     IPersistentMap    map->JsonObject
     IPersistentVector seq->JsonArray
     IPersistentList   seq->JsonArray
     IPersistentSet    seq->JsonArray
     ISeq              seq->JsonArray
     Associative       map->JsonObject)
   data))

(extend-protocol Encodeable
  Object
  (encode [data] data)
  nil
  (encode [data] nil)
  BigDecimal
  (encode [data] (double data))
  BigInt
  (encode [data] (long data))
  ;; clojure maps are Maps and Seqables, and sometimes the Seqable
  ;; version gets called for a them. Let's explicitly handle IPersistentCollections.
  IPersistentCollection
  (encode [data]
    (encode-collection data))
  Map
  (encode [data]
    (java-map->JsonObject data))
  Ratio
  (encode [data] (double data))
  Seqable
  (encode [data]
    (seq->JsonArray data))
  List
  (encode [data]
    (seq->JsonArray data))
  Keyword
  (encode [data]
    (name data)))

(defprotocol Decodeable
  (decode [data]))

(extend-protocol Decodeable
  Object
  (decode [data] data)
  nil
  (decode [data] nil)
  JsonArray
  (decode [data]
    (map decode data))
  JsonObject
  (decode [data]
    (decode (.toMap data)))
  Map
  (decode [data]
    (reduce (fn [m ^Map$Entry e]
              (assoc m
                (keyword (.getKey e))
                (decode (.getValue e))))
      {} (seq data)))
  List
  (decode [data]
    (vec (map decode data))))

(defn uuid
  "Generates a uuid."
  []
  (.toString (UUID/randomUUID)))

(defn camelize [input]
  (let [words (s/split (name input) #"[\s_-]+")]
    (s/join (cons (.toLowerCase ^String (first words))
              (map #(str (.toUpperCase (subs % 0 1)) (subs % 1))
                (rest words))))))

(defn set-property [obj prop value]
  (clojure.lang.Reflector/invokeInstanceMethod
    obj
    (->> prop name (str "set-") camelize)
    (into-array Object [value]))
  obj)

(defn set-properties [obj props]
  (mapv (fn [[prop value]]
          (set-property obj prop value)) props)
  obj)

(defn inet-socket-address->map [^InetSocketAddress addr]
  (if addr
    {:host (.getHostString addr)
     :port (.getPort addr)
     :basis addr}))

(defn inet-address->map [^InetAddress addr]
  (if addr
    {:address (.getHostAddress addr)
     :basis addr}))

(defprotocol ExceptionAsMap
  (exception->map [this]))

(extend-protocol ExceptionAsMap
  nil
  (exception->map [_] nil)
  Throwable
  (exception->map [e]
    {:message (.getMessage e)
     :cause (.getCause e)
     :basis e}))

(defn interface-name
  "Looks up the name of the interface for the given address String."
  [address]
  (-> address InetAddress/getByName NetworkInterface/getByInetAddress .getName))

(def byte-arr-class (Class/forName "[B"))

(defn require-resolve
  "Requires the ns for sym, then resolves sym."
  [sym]
  (require (symbol (namespace sym)))
  (resolve sym))
