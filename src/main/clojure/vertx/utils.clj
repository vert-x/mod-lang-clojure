(ns vertx.utils
  (:import [org.vertx.java.core.json JsonObject])
  (:require [clojure.string :as s]
            [clojure.data.json :as json]))

(defn ^JsonObject map->json
  "Convert a map to ```JsonObject```."
  [clj-map]
  (JsonObject. (json/write-str clj-map)))
