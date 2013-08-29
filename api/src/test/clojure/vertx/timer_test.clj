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

(ns vertx.timer-test
  (:require [vertx.testtools :as t]
            [vertx.core :as core]
            [clojure.test :refer [deftest is use-fixtures]]))

(use-fixtures :each t/as-embedded)

(deftest timer-macro
  (core/timer
   10
   (t/test-complete)))

(deftest timer-fn
  (core/timer*
   10
   (fn [id]
     (t/test-complete
      (is (not (nil? id)))))))

(deftest periodic-fn
  (let [count (atom 1)
        total 10]
    (core/periodic*
     10
     (fn [id]
       (swap! count inc)
       (when (= @count total)
         (core/cancel-timer id)
         ;; end test in another timer to catch if this timer keeps running
         (core/timer 100 (t/test-complete)))
       (when (> @count total)
         (throw (IllegalStateException. "fired too many times")))))))
