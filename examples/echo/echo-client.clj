(ns example.echo.echo-client
  (:import (org.vertx.java.core.buffer Buffer))
  (:require [vertx.core :as vertx]
            [vertx.net :as net]))


(defn parse-buf [buf]
  (net/parse-delimited
   buf "\n"
   (vertx/handler [res]
                  (let [s (.toString res)]
                    (println "net client receive:" s)))))

(net/sock-connect
 1234 "localhost"
 (vertx/handler [async-sock]
                (when-let [sock (.result async-sock)]
                  (net/pump sock sock)
                  (net/data-handler sock [buf]
                                    (parse-buf buf))
                  (doseq [i (range 10)]
                    (let [s (str "hello" i "\n")]
                      (->> s (Buffer.) (.write sock)))))))
