(ns ^:internal ^:no-doc vertx.utils
    "Internal utility functions."
  (:require [clojure.string :as s]
            [clojure.data.json :as json])
  (:import [org.vertx.java.core.json JsonArray JsonObject]
           java.util.UUID))

(defn encode
  [data]
  (condp instance? data
    java.util.Map (JsonObject. (json/write-str data))
    java.util.List (JsonArray. (json/write-str data))
    data))

(defn decode
  [j]
  (condp instance? j
    JsonObject (json/read-str (.encode j) :key-fn keyword)
    JsonArray (json/read-str (.encode j) :key-fn keyword)
    j))

(defn uuid
  "Generates a uuid."
  []
  (.toString (UUID/randomUUID)))

