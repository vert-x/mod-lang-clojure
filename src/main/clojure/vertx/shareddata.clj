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
  "Functions for operating on Vert.x Shareddata.
   Though shareddata isn't that useful for Clojure,
   since it already has immutable data structures."
  (:require [vertx.core :as core])
  (:import [org.vertx.java.core.shareddata SharedData]))


(def ^{:dynamic true
       :doc "The currently active SharedData instance.
             If not bound, the eventbus from vertx.core/*vertx* will be used.
             You should only need to bind this for advanced usage."}
  *shared-data*) nil

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

