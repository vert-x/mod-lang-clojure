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
  (:require [clojure.string :as s]
            [clojure.data.json :as json])
  (:import [org.vertx.java.core.json JsonArray JsonObject]
           [clojure.lang BigInt IPersistentMap Ratio Seqable]
           [java.util Map UUID]
           [java.net NetworkInterface InetAddress InetSocketAddress]
           java.math.BigDecimal))


(defprotocol Encodeable
  (encode [data]))

(defn- encode-pmap[data]
  (reduce #(do (.putValue %1 
                          (name (first %2)) 
                          (encode (second %2))) %1) 
          (JsonObject.) 
          (seq data)))

(defn- encode-map[data]
  (reduce #(do (.putValue %1 
                          (.getKey %2)
                          (encode (.getValue %2))) %1) 
          (JsonObject.) 
          (seq data)))

(defn- encode-seq[data]
  (reduce #(do (.add %1 (encode %2)) %1) 
          (JsonArray.) 
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
  ;; version gets called for a them. Let's explicitly extend
  ;; the clojure map interface to prevent that.
  IPersistentMap
  (encode [data]
          (encode-pmap data))
    ;(JsonObject. ^String (json/write-str data)))
  Map
  (encode [data]
          (encode-map data))
    ;(JsonObject. ^String (json/write-str data)))
  Ratio
  (encode [data] (double data))
  Seqable
  (encode [data]
          (encode-seq data)))
    ;(JsonArray. ^String (json/write-str data))))

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
    ;(json/read-str (.encode data) :key-fn keyword))
  JsonObject
  (decode [data]
          (reduce #(assoc %1 (keyword (.getKey %2)) (decode (.getValue %2))) 
                  {} 
                  (seq (.toMap data)))))
    ;(json/read-str (.encode data) :key-fn keyword)))

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
