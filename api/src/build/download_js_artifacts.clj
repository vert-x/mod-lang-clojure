(ns download-js-artifacts
  (:require [clojure.java.io :as io]))

;; download vertxbus and sockjs, so they can be available to the
;; ClojureScript compiler for downstream projects via deps.cljs.

(let [artifacts [["${version.sockjs}"
                  "http://cdn.sockjs.org/sockjs-${version.sockjs}.js"]
                 ["${version.vertx}"
                  "https://raw.github.com/eclipse/vert.x/v${version.vertx}/src/dist/client/vertxbus.js"
                  "https://raw.github.com/eclipse/vert.x/master/src/dist/client/vertxbus.js"]]
      file (io/file (doto (io/file "target/classes/js")
                      .mkdirs)
             "vertxbus-sockjs.js")]
  (if (.exists file)
    (println (.getPath file) "already exists, skipping.")
    (spit file
      (reduce (fn [acc [version release-url snapshot-url]]
                (let [url (if (re-find #"SNAPSHOT" version) snapshot-url release-url)]
                  (println "Downloading" url)
                  (str acc (slurp url))))
        "" artifacts))))
