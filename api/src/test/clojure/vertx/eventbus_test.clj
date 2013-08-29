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

(ns vertx.eventbus-test
  (:require [vertx.testtools :as t]
            [vertx.core :as core]
            [vertx.eventbus :as eb]
            [clojure.test :refer [deftest is use-fixtures]]))

(use-fixtures :each t/as-embedded)

(deftest eb-send
  (let [msg {:ham "biscuit"}
        addr "eb.test"
        id (atom nil)]
    (reset! id
            (eb/on-message
             addr
             (fn [m]
               (t/test-complete
                (is (= msg m))
                (eb/unregister-handler @id)))))
    (is (not (nil? @id)))
    (eb/send addr msg)))

(deftest reply
  (let [msg {:ham "biscuit"}
        addr "eb.test"
        reply {:biscuit "gravy"}
        id (atom nil)]
    (reset! id
            (eb/on-message
             addr
             (fn [m]
               (t/test-complete
                (is (= msg m))
                (eb/reply reply)))))
    (is (not (nil? @id)))
    (eb/send addr msg
             (fn [m]
               (t/test-complete
                (is (= reply m))
                (eb/unregister-handler @id))))))

(deftest send-unregister-send
  (let [msg {:ham "biscuit"}
        addr "eb.test"
        id (atom nil)
        rcvd (atom false)]
    (reset! id
            (eb/on-message
             addr
             (fn [m]
               (if @rcvd
                 (throw (IllegalStateException. "Handler already called")))
               (is (= msg m))
               (eb/unregister-handler @id)
               (reset! rcvd true)
               (core/timer 100 (t/test-complete)))))

    (t/assert-not-nil @id)
    (dotimes [_ 2]
      (eb/send addr msg))))

(deftest publish-multiple-matching-handlers
  (let [msg {:ham "biscuit"}
        addr "eb.test"
        total 10
        count (atom 1)]
    (dotimes [_ total]
      (let [id (atom nil)]
        (reset! id
                (eb/on-message
                 addr
                 (fn [m]
                   (t/assert= msg m)
                   (eb/unregister-handler @id)
                   (swap! count inc)
                   (if (= @count total)
                     (t/test-complete)))))))

    (eb/publish addr msg)))

(deftest reply-of-reply-of-reply
  (let [addr "eb.test"
        id (atom nil)]
    (reset! id
            (eb/on-message
             addr
             (fn [m]
               (t/test-complete
                (is (= "message" m))
                (eb/reply "reply"
                          (fn [m]
                            (is (= "reply-of-reply" m))
                            (eb/reply m "reply-of-reply-of-reply")))))))

    (eb/send addr "message"
             (fn [m]
               (is (= "reply" m))
               (eb/reply "reply-of-reply"
                         (fn [m]
                           (t/test-complete
                            (is (= "reply-of-reply-of-reply" m))
                            (eb/unregister-handler @id))))))))

(deftest message-types-roundtrip
  (let [addr "eb.test"
        tfn
        (fn [msg]
          (let [id (atom nil)]
            (reset! id
                    (eb/on-message
                     addr
                     (fn [m]
                       (eb/unregister-handler @id)
                       (eb/reply m))))

            (eb/send addr msg
                     (fn [m]
                       (t/test-complete
                        (is (= msg m)))))))]
    (doseq [m ["ham"
               nil
               true
               false
               1
               1.1
               [1 2 3]
               [{:a "b"} 2]
               {:a "biscuit" :b nil :c true :d false :e 1 :f 1.1 :g [1 2 3]}]]
      (tfn m))))
