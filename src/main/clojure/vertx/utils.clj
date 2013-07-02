(ns vertx.utils
  (:require [clojure.string :as s]
            [clojure.data.json :as json])
  (:import [org.vertx.java.core.json JsonObject]))

;; TODO: we may need to handle other types here. what about arrays?
(defn ->json
  "Converts a map to a JsonObject, else returns the data"
  [data]
  (condp instance? data
    java.util.Map (JsonObject. (json/write-str data))
    data))

(defn <-json
  "Converts a JsonObject to clojure data, converting keys to keywords."
  [^JsonObject j]
  (json/read-str (.encode j) :key-fn keyword))

(defmacro webroot
  "Get the absolute path of the current verticle definition."
  [resource]
  `(str user-dir "src/" (-> (s/join "/" (-> ~*ns* str (s/split #"\.") drop-last)) (s/replace #"-" "_")) "/" ~resource))

(defmacro defni [n]
  `(do
     (let [fqn# (format "%s/%s" *ns* ~(name n))]
       (println "NOT IMPLEMENTED:" fqn#)
       (defn ~n [& args#]
         (throw (RuntimeException.
                 (format "%s not yet implemented" fqn#)))))))
