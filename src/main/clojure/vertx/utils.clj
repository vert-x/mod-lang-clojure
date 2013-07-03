(ns vertx.utils
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

(defn uuid []
  (.toString (UUID/randomUUID)))

(defmacro defni [n]
  `(do
     (let [fqn# (format "%s/%s" *ns* ~(name n))]
       (println "NOT IMPLEMENTED:" fqn#)
       (defn ~n [& args#]
         (throw (RuntimeException.
                 (format "%s not yet implemented" fqn#)))))))
