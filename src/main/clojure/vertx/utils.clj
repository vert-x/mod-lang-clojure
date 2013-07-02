(ns vertx.utils
  (:require [clojure.string :as s]
            [clojure.data.json :as json])
  (:import [org.vertx.java.core.json JsonObject]))

;; TODO: we may need to handle other types here
(defn ->json
  [data]
  (condp instance? data
    java.util.Map (JsonObject. (json/write-str data))
    data))

(defn <-json [^JsonObject j]
  (json/read-str (.encode j) :key-fn keyword))

(defmacro webroot
  "Get the absolute path of the current verticle definition."
  [resource]
  `(str user-dir "src/" (-> (s/join "/" (-> ~*ns* str (s/split #"\.") drop-last)) (s/replace #"-" "_")) "/" ~resource))
