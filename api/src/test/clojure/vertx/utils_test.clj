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

(ns vertx.utils-test
  (:require [vertx.utils :refer :all]
            [clojure.test :refer :all])
  (:import io.vertx.test.Biscuit
           [org.vertx.java.core.json JsonArray JsonObject]))


(deftest camelize-should-work
  (are [given exp] (= (camelize given) exp)
       :foo-bar  "fooBar"
       'foo-bar  "fooBar"
       "foo-bar" "fooBar"
       :foo_bar  "fooBar"
       :foo-bar-baz "fooBarBaz"
       :foo-BAR "fooBAR"
       :foo-bAR "fooBAR"
       :foo-b "fooB"
       :f "f"))

(deftest set-property-should-work
  (let [b (Biscuit.)]
    (is (nil? (.getJam b)))
    (is (= b (set-property b :jam "grape")))
    (is (= "grape" (.getJam b)))))

(deftest set-properties-should-work
  (let [b (Biscuit.)]
    (is (nil? (.getJam b)))
    (is (= b (set-properties b {:jam "grape"})))
    (is (= "grape" (.getJam b)))))

(deftest persistentmap-should-encode ;IPersistentMap
  (is (= (encode {:a "b"}) (JsonObject. "{\"a\":\"b\"}"))))

(deftest map-should-encode ;Map
  (is (= (encode (let [m (java.util.HashMap.)] (.put m "a" "b") m)) (JsonObject. "{\"a\":\"b\"}"))))

(deftest seq-should-encode ;Seqable
  (is (= (encode '(1000000000000 2000000000000)) (JsonArray. "[1000000000000,2000000000000]"))))

(deftest jsonarray-should-decode ;JsonArray
    (is (= [1,2] (decode (JsonArray. "[1,2]")))))

(deftest jsonobject-should-decode ;JsonObject
  (is (= {:a "b"} (decode (JsonObject. "{\"a\":\"b\"}")))))

