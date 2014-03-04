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

(defn- assign-values-to-jsonobject [^JsonObject jobj k v]
  (.putValue jobj k v)
  jobj)

(defn- assign-entry-to-jsonobject [^JsonObject jobj ^Map$Entry e]
  (assign-values-to-jsonobject jobj (encode (.getKey e)) (encode (.getValue e))))

(defn- encode-map [data]
  (reduce #(assign-values-to-jsonobject
             %1 (name (first %2)) (encode (second %2)))
    (JsonObject.)
    (seq data)))

(defn- encode-java-map [^Map data]
  (reduce #(assign-entry-to-jsonobject %1 %2)
    (JsonObject.)
    (seq data)))

(defn- add-item-to-json-array [^JsonArray ja item]
  (.add ja item) ja)

(defn- encode-seq [data]
  (reduce #(add-item-to-json-array %1 (encode %2))
    (JsonArray.)
    data))

(defn- encode-collection [data]
  (condp instance? data
    IPersistentMap (encode-map data)
    IPersistentVector (encode-seq data)
    IPersistentList (encode-seq data)
    IPersistentSet (encode-seq data)
    ISeq (encode-seq data)
    Associative (encode-map data)))

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
    (encode-java-map data))
  Ratio
  (encode [data] (double data))
  Seqable
  (encode [data]
    (encode-seq data))
  List
  (encode [data]
    (encode-seq data))
  Keyword
  (encode [data]
    (name data)))

(defprotocol Decodeable
  (decode [data]))

(defn- assoc-java-entry-to-map [newmap ^Map$Entry entry]
  (let [k (keyword (.getKey entry))
        v (decode (.getValue entry))]
    (assoc newmap k v)))

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
    (reduce assoc-java-entry-to-map {} (seq (.toMap data))))
  Map
  (decode [data]
    (reduce assoc-java-entry-to-map {} (seq data))))

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
