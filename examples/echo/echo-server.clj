(ns example.echo.echo-server
  (:import (org.vertx.java.core.buffer Buffer))
  (:use [vertx.core]
        [vertx.net]))



(defn parse-buf [buf sock]
  (parse-delimited buf "\n"
                   (handler [res]
                            (let [s (.toString res)]
                              (println "net server receive:" s)
                              (->> (str s " from server \n") (Buffer.) (.write sock)))
                            )))

(sock-listen 1234 "localhost"
             (connect-handler sock-server [sock]
                              (pump sock sock)
                              (data-handler sock [buf]
                                            (parse-buf buf sock))
                              (close-handler sock [_]
                                             (println "close handler be invoked"))
                              ))
