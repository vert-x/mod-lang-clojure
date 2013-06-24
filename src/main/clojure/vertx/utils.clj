(ns vertx.utils
  (:import [org.vertx.java.core.json JsonObject])
  (:require [clojure.string :as s]))

(defn ^JsonObject json
  "Convert a map to ```JsonObject```."
  [clj-map]
  (let [ret (JsonObject.)]
    (doseq [[k v] clj-map]
      (.putString ret (name k) (str v)))
    ret))


(defmacro webroot
  "Get the absolute path of the current verticle definition."
  [resource]
  `(str user-dir "src/" (-> (s/join "/" (-> ~*ns* str (s/split #"\.") drop-last)) (s/replace #"-" "_")) "/" ~resource))
