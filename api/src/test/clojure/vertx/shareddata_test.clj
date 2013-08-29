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

(ns vertx.shareddata-test
  (:require [vertx.shareddata :as shared]
            [vertx.testtools :as t]
            [clojure.test :refer [deftest is use-fixtures]]))

(use-fixtures :each t/as-embedded)

(deftest shared-map
  (let [name "my-map"
        shared-map (shared/get-map name)]

    (t/test-complete
     (.put shared-map "key" "value")
     (is (= (get (shared/get-map name) "key") "value"))

     (.remove shared-map "key")
     (is (nil? (get (shared/get-map name) "key")))

     (is (true? (shared/remove-map name))))))


(deftest shared-set
  (let [name "my-set"
        shared-set (shared/get-set name)]

    (t/test-complete
     (.add shared-set "value")
     (is (true? (contains? (shared/get-set name) "value")))

     (.remove shared-set "value")
     (is (false? (contains? (shared/get-set name) "value")))

     (is (true? (shared/remove-set name))))))


(deftest set-add
  (let [set (shared/get-set "foo")]
    (shared/add! set "a")
    (shared/add! set "b" "c")
    (shared/add! "foo" "d")
    (shared/add! :foo "e")
    (t/test-complete
     (is (true? (contains? set "a")))
     (is (true? (contains? set "b")))
     (is (true? (contains? set "c")))
     (is (true? (contains? set "d")))
     (is (true? (contains? set "e"))))))

(deftest set-remove
  (let [set (shared/get-set "foo")]
    (shared/add! set "a" "b" "c" "d" "e")
    (shared/remove! "foo" "a")
    (shared/remove! :foo "d" "e")
    (t/test-complete
     (is (false? (contains? set "a")))
     (is (false? (contains? set "d")))
     (is (false? (contains? set "e")))
     (is (true? (contains? set "b")))
     (is (true? (contains? set "c")))
     (shared/remove! set "b" "c")
     (is (false? (contains? set "b")))
     (is (false? (contains? set "c"))))))

(deftest map-put
  (let [m (shared/get-map "foo")]
    (shared/put! m "a" "b")
    (shared/put! "foo" "c" "d")
    (shared/put! :foo "e" "f" "g" "h")
    (t/test-complete
     (is (= "b" (get m "a")))
     (is (= "d" (get m "c")))
     (is (= "f" (get m "e")))
     (is (= "h" (get m "g"))))))

(deftest map-remove
  (let [map (shared/get-map "map-remove")]
    (shared/put! map "a" "a" "b" "b" "c" "c" "d" "d" "e" "e")
    (shared/remove! "map-remove" "a")
    (shared/remove! :map-remove "d" "e")
    (t/test-complete
     (is (nil? (get map "a")))
     (is (nil? (get map "d")))
     (is (nil? (get map "e")))
     (is (= "b" (get map "b")))
     (is (= "c" (get map "c")))
     (shared/remove! map "b" "c")
     (is (nil? (get map "b")))
     (is (nil? (get map "c"))))))

(deftest map-put-invalid-args
  (t/test-complete
   (is (thrown? IllegalArgumentException
                (shared/put! (shared/get-map "blah") :a)))))

(deftest string-ambiguous
  (shared/put! "same" "somekey" "somevalue")
  (shared/add! "same" "somevalue")
  (t/test-complete
   (is (thrown? IllegalArgumentException
                (shared/clear! "same")))))
