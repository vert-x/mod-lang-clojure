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
            [vertx.utils :as u]
            [clojure.test :refer [deftest is use-fixtures]]))

(use-fixtures :each t/as-embedded)

(defn ^{:test t/test-complete*} fail-on-exception [e]
  (is false (str "Unexpected exception: " e)))

(deftest echo
  (let [peer1 (udp/socket)
        peer2 (udp/socket)]
    
    (-> peer1
        (udp/on-data 
         (fn [packet]
           (is (= "127.0.0.1" (-> packet :sender :host)))
           (is (= 1234 (-> packet :sender :port)))
           (is (= "data" (str (:data packet))))
           (udp/send peer2 "data" "127.0.0.1" 1235 (fn [err _] (is (nil? err))))))
        (stream/on-exception fail-on-exception)
        (udp/listen 1234 "127.0.0.1" (fn [err _] (is (nil? err)))))

    (-> peer2
        (udp/on-data 
         (fn [packet]
           (is (= "127.0.0.1" (-> packet :sender :host)))
           (is (= 1235 (-> packet :sender :port)))
           (t/test-complete (is (= "data" (str (:data packet)))))))
        (stream/on-exception fail-on-exception)
        (udp/listen 1235 "127.0.0.1"
                    (fn [err socket]
                      (is (nil? err))
                      (udp/send peer1 "data" "127.0.0.1" 1234 (fn [err _] (is (nil? err)))))))))


(deftest multicast-join-leave
  (let [iface (u/interface-name "127.0.0.1")
        peer1 (-> (udp/socket :ipv4 {:multicast-network-interface iface}))
        peer2 (-> (udp/socket :ipv4 {:multicast-network-interface iface}))
        group "230.0.0.1"
        received* (atom 0)
        
        peer1-leave-handler (fn [err socket]
                                 (is (nil? err))
                                 (udp/send peer2 "data" group 1234
                                           (fn [err socket]
                                             (is (nil? err))
                                             (core/timer 1000 (t/test-complete (is (= 1 @received*)))))))

        peer2-send-handler (fn [err socket]
                             (is (nil? err))
                             ;;leave group
                             (core/timer 1000
                               (is (= 1 @received*))
                               (udp/leave-multicast-group peer1 group iface peer1-leave-handler)))
        
        listen-join-handler (fn [err socket]
                              (is (nil? err))
                              (udp/send peer2 "data" group 1234 peer2-send-handler))

        listen-peer1-handler (fn [err socket]
                               (is (nil? err))
                               (udp/join-multicast-group peer1 group iface listen-join-handler))]
    
    (-> peer1
        (stream/on-exception fail-on-exception)
        (udp/on-data (fn [packet]
                       (is (= "data" (str (:data packet))))
                       (swap! received* inc)))
        (udp/listen 1234 nil listen-peer1-handler))
    
    (stream/on-exception peer2 fail-on-exception)))


