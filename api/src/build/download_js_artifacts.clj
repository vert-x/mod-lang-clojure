(ns download-js-artifacts
  (:require [clojure.java.io :as io]))

;; download vertxbus and sockjs, so they can be available to the
;; ClojureScript compiler for downstream projects via deps.cljs.

(let [artifacts {"vertxbus.js" ["${version.vertx}"
                                "https://raw.github.com/eclipse/vert.x/v${version.vertx}/src/dist/client/vertxbus.js"
                                "https://raw.github.com/eclipse/vert.x/master/src/dist/client/vertxbus.js"]
                 "sockjs.js" ["${version.sockjs}"
                              "http://cdn.sockjs.org/sockjs-${version.sockjs}.js"]}
      dest-dir (doto (io/file "target/classes/js")
                 .mkdirs)]
  (doseq [[name info] artifacts]
    (let [file (io/file dest-dir name)]
      (if (.exists file)
        (println (.getPath file) "already exists, skipping.")
        (let [[version release-url snapshot-url] info
              url (if (re-find #"SNAPSHOT" version) snapshot-url release-url)]
          (println "Downloading" url "to" (.getPath file))
          (io/copy (io/reader url) file))))))
