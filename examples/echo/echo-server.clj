(ns example.echo.echo-server
  (:import (org.vertx.java.core.buffer Buffer))
  (:require [vertx.core :as vertx]
            [vertx.net :as net]))


(defn parse-buf [buf sock]
  (net/parse-delimited
   buf "\n"
   (vertx/handler [res]
                  (let [s (.toString res)]
                    (println "net server receive:" s)
                    (->> (str s " from server \n") (Buffer.) (.write sock))))))

(net/sock-listen
 1234 "localhost"
 (net/connect-handler sock-server [sock]
                      (net/pump sock sock)
                      (net/data-handler sock [buf]
                                        (parse-buf buf sock))
                      (net/close-handler sock [_]
                                         (println "close handler be invoked"))))
