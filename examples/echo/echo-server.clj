(ns example.echo.echo-server
  (:require [vertx.net :as net]
            [vertx.buffer :as buf]
            [vertx.stream :as stream]))

(defn echo-data [socket]
  (stream/pump socket socket true)
  (-> socket
      (stream/on-data #(println "echo server received:" %))
      (net/on-close #(println "Socket closed"))))

(-> (net/server)
    (net/on-connect echo-data)
    (net/listen 1234))
