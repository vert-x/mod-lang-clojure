(ns example.echo.echo-client
  (:require [vertx.core :as vertx]
            [vertx.net :as net]
            [vertx.buffer :as buff]))

(defn parse-buf [buf]
  (buff/parse-delimited
   buf "\n"
   (fn [res]
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
                      (->> s buff/buffer (.write sock)))))))
