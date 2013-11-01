;; Copyright 2013 the original author or authors.
;;
;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at
;;
;;      http://www.apache.org/licenses/LICENSE-2.0
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.

(ns vertx.net-test
  (:require [vertx.net :as net]
            [vertx.buffer :as buf]
            [vertx.stream :as stream]
            [vertx.testtools :as t]
            [clojure.test :refer [deftest is use-fixtures]]))

(use-fixtures :each t/as-embedded)

(defn assert-socket-addresses [socket]
  (is (not (nil? (-> socket .localAddress .getAddress))))
  (is (> (-> socket .localAddress .getPort) -1))
  (is (not (nil? (-> socket .remoteAddress .getAddress))))
  (is (> (-> socket .remoteAddress .getPort) -1))
  socket)

(defn echo-handler [socket]
  (stream/on-data socket
                  (fn [data]
                    (stream/write socket data))))

(defn exercise-handlers [socket]
  (-> socket
      (stream/on-drain #(println "drain"))
      (stream/on-end #(println "end"))
      (net/on-close #(println "close"))))

(deftest echo
  (letfn [(client-data-handler [sent-buf! rcv-buf! send-count send-size data]
            (buf/append! rcv-buf! data)
            (when (= (.length rcv-buf!) (* send-count send-size))
              (t/test-complete
               (is (= sent-buf! rcv-buf!)))))

          (client-connect-handler [err socket]
            (assert-socket-addresses socket)
            (let [sent-buf! (buf/buffer)
                  rcv-buf! (buf/buffer)
                  send-count 10
                  send-size 100]
              (stream/on-data socket (partial client-data-handler
                                              sent-buf! rcv-buf!
                                              send-count send-size))
              (dotimes [_ send-count]
                (let [data (t/random-buffer send-size)]
                  (buf/append! sent-buf! data)
                  (stream/write socket data)))))

          (server-listen-handler [orig-server port err server]
            (is (nil? err))
            (is (= orig-server server))
            (net/connect port client-connect-handler))]

    (let [server (net/server)
          port 8111]
      (-> server
          (net/on-connect (comp assert-socket-addresses
                                echo-handler
                                exercise-handlers))
          (net/listen port "localhost"
                      (partial server-listen-handler server port))))))

(deftest pump
  (letfn [(client-data-handler [sent-buf! rcv-buf! send-count send-size data]
            (buf/append! rcv-buf! data)
            (when (= (.length rcv-buf!) (* send-count send-size))
              (t/test-complete
               (is (= sent-buf! rcv-buf!)))))

          (client-connect-handler [err socket]
            (let [sent-buf! (buf/buffer)
                  rcv-buf! (buf/buffer)
                  send-count 10
                  send-size 100]
              (stream/on-data socket (partial client-data-handler
                                              sent-buf! rcv-buf!
                                              send-count send-size))
              (dotimes [_ send-count]
                (let [data (t/random-buffer send-size)]
                  (buf/append! sent-buf! data)
                  (stream/write socket data)))))]

    (let [server (net/server)
          port 8111]
      (-> server
          (net/on-connect #(stream/pump % %))
          (net/listen port "localhost"
                      (fn [_ _]
                        (net/connect port client-connect-handler)))))))

(deftest echo-ssl
  (letfn [(client-data-handler [sent-buf! rcv-buf! send-count send-size data]
            (buf/append! rcv-buf! data)
            (when (= (.length rcv-buf!) (* send-count send-size))
              (t/test-complete
               (is (= sent-buf! rcv-buf!)))))

          (client-connect-handler [err socket]
            (is (nil? err))
            (assert-socket-addresses socket)
            (let [sent-buf! (buf/buffer)
                  rcv-buf! (buf/buffer)
                  send-count 10
                  send-size 100]
              (stream/on-data socket (partial client-data-handler
                                              sent-buf! rcv-buf!
                                              send-count send-size))
              (dotimes [_ send-count]
                (let [data (t/random-buffer send-size)]
                  (buf/append! sent-buf! data)
                  (stream/write socket data)))))

          (server-listen-handler [orig-server port err server]
            (is (nil?  err))
            (is (= orig-server server))
            (-> (net/client {:SSL true
                             :key-store-path (t/resource-path "keystores/server-keystore.jks")
                             :key-store-password "wibble"
                             :trust-store-path (t/resource-path "keystores/server-truststore.jks")
                             :trust-store-password "wibble"})
                (net/connect port "localhost" client-connect-handler)))]

    (let [server (net/server
                  {:SSL true
                   :key-store-path (t/resource-path "keystores/client-keystore.jks")
                   :key-store-password "wibble"
                   :trust-store-path (t/resource-path "keystores/client-truststore.jks")
                   :trust-store-password "wibble"
                   :client-auth-required true})
          port 8111]
      (-> server
          (net/on-connect (comp assert-socket-addresses
                                echo-handler
                                exercise-handlers))
          (net/listen port "localhost"
                      (partial server-listen-handler server port))))))

(deftest write-str
  (letfn [(client-data-handler [rcv-buf! expected data]
            (buf/append! rcv-buf! data)
            (when (= (.length rcv-buf!) (.length expected))
              (is (= expected (str rcv-buf!)))
              (t/test-complete)))

          (client-connect-handler [err socket]
            (assert-socket-addresses socket)
            (let [rcv-buf! (buf/buffer)
                  msg "ham-biscuit"]
              (stream/on-data socket (partial client-data-handler
                                              rcv-buf! msg))
              (stream/write socket msg)))

          (server-listen-handler [orig-server port err server]
            (is (nil? err))
            (is (= orig-server server))
            (net/connect port client-connect-handler))]
    (let [server (net/server)
          port 8111]
      (-> server
          (net/on-connect (comp assert-socket-addresses
                                echo-handler))
          (net/listen port "localhost"
                      (partial server-listen-handler server port))))))
