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

(ns vertx.datagram-test
  (:require [vertx.datagram :as udp]
            [vertx.buffer :as buf]
            [vertx.stream :as stream]
            [vertx.core :as core]
            [vertx.testtools :as t]
            [clojure.test :refer [deftest is use-fixtures]]))

(use-fixtures :each t/as-embedded)

(deftest echo
  (letfn [(listen-peer2-handler [peer1 peer2 err socket]
            (is (nil? err))
            (stream/on-data peer2
                            (fn [packet]
                              (let [data (udp/data packet)
                                    sender (udp/sender packet)]
                                (is (= "127.0.0.1" (:host sender)))
                                (is (= 1235 (:port sender)))
                                (t/test-complete (is (= "data" (.toString data))))
                                )))
            (udp/send peer1 "data" "127.0.0.1" 1234 (fn [err] (is (nil? err)))))

          (listen-peer1-handler [peer1 peer2 err socket]
            (is (nil? err))
            (stream/on-data peer1
                            (fn [packet]
                              (let [data (udp/data packet)
                                    sender (udp/sender packet)]
                                (is (= "127.0.0.1" (:host sender)))
                                (is (= 1234 (:port sender)))
                                (is (= "data" (.toString data)))
                                (udp/send peer2 "data" "127.0.0.1" 1235 (fn [err] (is (nil? err))))
                                )))

            (udp/listen peer2 "127.0.0.1" 1235 (partial listen-peer2-handler peer1 peer2)))]

    (let [peer1 (udp/socket)
          peer2 (udp/socket)]
      (stream/on-exception peer1 (fn [e] (is (nil? e))))
      (stream/on-exception peer2 (fn [e] (is (nil? e))))
      (udp/listen peer1 "127.0.0.1" 1234 (partial listen-peer1-handler peer1 peer2)))))


(deftest multicast-join-leave
  (letfn [(peer1-unlisten-handler [peer1 peer2 err socket]
            (is (nil? err))
            (let [received* (atom false)]
              (stream/on-data peer1
                              (fn [packet]
                                ;;should not receive any more event as it left the group
                                (println "should not receive any more event as it left the group")
                                (t/assert false)
                                (swap! received* not)))

              (udp/send peer2 "data" "230.0.0.1" 1234
                        (fn [err socket]
                          (is (nil? err))
                          (core/timer 1000 (t/test-complete (is (false? @received*))))
                          ))))

          (peer2-send-handler [peer1 peer2 err socket]
            (is (nil? err))
            ;;leave group
            (udp/unlisten-multicast-group peer1 "230.0.0.1" (partial peer1-unlisten-handler peer1 peer2)))

          (listen-group-handler [peer1 peer2 err socket]
            (is (nil? err))
            (udp/send peer2 "data" "127.0.0.1" 1234 (partial peer2-send-handler peer1 peer2)))

          (listen-peer1-handler [peer1 peer2 err socket]
            (is (nil? err))
            (udp/listen-multicast-group peer1 "230.0.0.1" (partial listen-group-handler peer1 peer2))
            )]

    (let [peer1 (udp/socket)
          peer2 (udp/socket)]
      (stream/on-exception peer1 (fn [e] (is (nil? e))))
      (stream/on-exception peer2 (fn [e] (is (nil? e))))

      (stream/on-data peer1 (fn [packet] (is (= "data" (.toString (udp/data packet))))))
      (udp/listen peer1 "127.0.0.1" 1234 (partial listen-peer1-handler peer1 peer2)))))
