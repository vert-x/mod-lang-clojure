(ns example.echo.echo-client
  (:require [vertx.core :as vertx]
            [vertx.net :as net]
            [vertx.stream :as stream]
            [vertx.buffer :as buf]))

(defn send-data [socket]
  (stream/on-data socket #(println "echo client received:" %))
  (doseq [i (range 10)]
    (let [s (format "hello %s\n" i)]
      (println "echo client sending:" s)
      (->> s buf/buffer (.write socket)))))

(net/connect
 1234 "localhost"
 (fn [err socket]
   (if err
     (throw err)
     (send-data socket))))
