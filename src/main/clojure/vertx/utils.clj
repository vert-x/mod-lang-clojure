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
           [clojure.lang BigInt Ratio Seqable]
           [java.util Map UUID]
           java.math.BigDecimal))


;; TODO: implement as protocols?
(defn encode
  [data]
  (condp instance? data
    BigDecimal (double data)
    BigInt     (long data)
    Map        (JsonObject. (json/write-str data))
    Ratio      (double data)
    Seqable    (JsonArray. (json/write-str data))
    data))

(defn decode
  [j]
  (condp instance? j
    JsonObject (json/read-str (.encode j) :key-fn keyword)
    JsonArray  (json/read-str (.encode j) :key-fn keyword)
    j))

(defn uuid
  "Generates a uuid."
  []
  (.toString (UUID/randomUUID)))

(defn camelize [input] 
  (let [words (s/split (name input) #"[\s_-]+")] 
    (s/join (cons (.toLowerCase (first words))
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

