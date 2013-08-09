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

(ns vertx.isolation-test
  (:require [vertx.core :as vertx]
            [vertx.eventbus :as eb]
            [vertx.testtools :as t]))


(defn test-differently-named-verticles-are-isolated []
  (let [msg-count (atom 0)]
    (eb/on-message "isolation"
                         (fn [m]
                           (t/assert= 1 m)
                           (if (= 2 (swap! msg-count inc))
                             (t/test-complete)))))
  
  (vertx/deploy-verticle "v1.clj" {} 1
                        (fn [& _]
                          (vertx/deploy-verticle "v2.clj"))))

(defn test-samely-named-verticles-are-not-isolated []
  (let [received-messages (atom #{})]
    (eb/on-message "isolation"
                         (fn [m]
                           (swap! received-messages conj m)
                           (if (= 2 (count @received-messages))
                             (t/test-complete
                              (t/assert= #{1 2} @received-messages))))))
  
  (vertx/deploy-verticle "v1.clj" {} 2))

(t/start-tests)
