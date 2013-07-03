(ns example.echo.echo-client
  (:import (org.vertx.java.core.buffer Buffer))
  (:use [vertx.core]
        [vertx.net]))


(defn parse-buf [buf]
  (parse-delimited buf "\n"
                   (handler [res]
                            (let [s (.toString res)]
                             (println "net client receive:" s)))))

(sock-connect 1234 "localhost"
              (handler [async-sock]
                       (when-let [sock (.result async-sock)]
                         (pump sock sock)
                         (data-handler sock [buf]
                                       (parse-buf buf))
                         (doseq [i (range 10)]
                           (let [s (str "hello" i "\n")]
                             (->> s (Buffer.) (.write sock))))
                         ))
              )
