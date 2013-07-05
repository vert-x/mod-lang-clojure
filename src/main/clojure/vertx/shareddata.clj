(ns vertx.shareddata
  "Functions for operating on Vert.x Shareddata.
   Though shareddata isn't that useful for Clojure,
   since it already has immutable data structures.
   We may want to implement them for completeness though"
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
  "Remove the map with the specific name"
  ([name]
     (remove-set (core/get-vertx) name))
  ([vertx name]
     (-> (shared-data vertx) (.removeSet name))))

