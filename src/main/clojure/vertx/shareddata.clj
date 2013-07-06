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


(defn- shared-data [vertx]
  (.sharedData vertx))

(defn get-map
  "Return a map with specific name.
   If vertx is not provided, it defaults to the default
   vertx (vertx.core/*vertx*)."
  ([name]
     (get-map (core/get-vertx) name))
  ([vertx name]
     (-> (shared-data vertx) (.getMap name))))

(defn get-set
  "Return a Set with specific name"
  ([name]
     (get-set (core/get-vertx) name))
  ([vertx name]
     (-> (shared-data vertx) (.getSet name))))

(defn remove-map
  "Remove the Map with the specific name"
  ([name]
     (remove-map (core/get-vertx) name))
  ([vertx name]
     (-> (shared-data vertx) (.removeMap name))))

(defn remove-set
  "Remove the Set with the specific name"
  ([name]
     (remove-set (core/get-vertx) name))
  ([vertx name]
     (-> (shared-data vertx) (.removeSet name))))

