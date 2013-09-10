(ns download-js-artifacts
  (:require [clojure.java.io :as io]))

;; download vertxbus and sockjs, so they can be available to the
;; ClojureScript compiler for downstream projects via deps.cljs.

(let [artifacts {"vertxbus.js" "https://raw.github.com/eclipse/vert.x/v${version.vertx}/src/dist/client/vertxbus.js"
                 "sockjs.js" "http://cdn.sockjs.org/sockjs-${version.sockjs}.js"}
      dest-dir (doto (io/file "target/classes/js")
                 .mkdirs)]
  (doseq [[name url] artifacts]
    (let [file (io/file dest-dir name)]
      (if (.exists file)
        (println (.getPath file) "already exists, skipping.")
        (do
          (println "Downloading" url "to" (.getPath file))
          (io/copy (io/reader url) file))))))
