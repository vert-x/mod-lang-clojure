(ns example.echo.echo-server
  (:require [vertx.net :as net]
            [vertx.buffer :as buff]))

(defn parse-buf [buf sock]
  (buff/parse-delimited
   buf "\n"
   (fn [res]
     (let [s (.toString res)]
       (println "net server receive:" s)
       (->> (str s " from server \n") buff/buffer (.write sock))))))

(net/sock-listen
 1234 "localhost"
 (net/connect-handler sock-server [sock]
                      (net/pump sock sock)
                      (net/data-handler sock [buf]
                                        (parse-buf buf sock))
                      (net/close-handler sock [_]
                                         (println "close handler invoked"))))
