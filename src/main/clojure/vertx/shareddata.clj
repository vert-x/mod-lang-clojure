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

(ns vertx.shareddata
  "Functions for sharing data between verticles on the same Vert.x instance."
  (:require [vertx.core :as core])
  (:import org.vertx.java.core.shareddata.SharedData))


(def ^{:dynamic true
       :doc "The currently active SharedData instance.
             If not bound, the eventbus from vertx.core/*vertx* will be used.
             You should only need to bind this for advanced usage."}
  *shared-data* nil)

(defn get-shared-data
  "Returns the currently active SharedData instance."
  []
  (or *shared-data* (.sharedData (core/get-vertx))))

(defn get-map
  "Return a map with specific name, using the SharedData instance from vertx.core/*vertx*."
  [name]
  (-> (get-shared-data) (.getMap name)))

(defn get-set
  "Return a Set with specific name, using the SharedData instance from vertx.core/*vertx*."
  [name]
  (-> (get-shared-data) (.getSet name)))

(defn remove-map
  "Remove the Map with the specific name, using the SharedData instance from vertx.core/*vertx*."
  [name]
  (-> (get-shared-data) (.removeMap name)))

(defn remove-set
  "Remove the Set with the specific name, using the SharedData instance from vertx.core/*vertx*."
  [name]
  (-> (get-shared-data) (.removeSet name)))

(defn- resolve-collection [col]
  (condp instance? col
    java.util.Set col
    org.vertx.java.core.shareddata.ConcurrentSharedMap col
    ;;default return shared-map, if both empty.
    java.lang.String (or (not-empty (get-map col)) (not-empty (get-set col))
                         (throw (IllegalArgumentException. 
                                 (str "the name of collection " col " is ambiguous."))))
    clojure.lang.Keyword (resolve-collection (name col))
    (throw (IllegalArgumentException. (str "unsupported type to this shraedData")))))

(defn add!
  "Adds values to the SharedData set.
   This mutates the set in place, returning the set."
  [s & vals]
  (let [s (resolve-collection s)]
    (.addAll s vals) s))

(defn put!
  "Adds values to the SharedData map.
   This mutates the map in place, returning the map."
  [m & kvs]
  (let [m (resolve-collection m)]
    (if (odd? (count kvs))
      (throw (IllegalArgumentException. (str "No value for key " (last kvs)))))
    (loop [[k v] (take 2 kvs)
           rest (nnext kvs)]
      (.put m k v)
      (if (seq rest)
        (recur (take 2 rest) (nnext rest))
        m))))

(defn remove!
  "Removes values from a SharedData set or map.
   This mutates the hash or set in place, returning the hash or set."
  [col & vals]
  (let [col (resolve-collection col)]
    (doseq [v vals]
      (.remove col v)) col))

(defn clear!
  "Clears all values from a SharedData set or map.
   This mutates the hash or set in place, returning the hash or set."
  [col]
  (let [col (resolve-collection col)]
    (.clear col) col))
