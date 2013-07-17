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
            [vertx.testtools :as t]))

(defn test-map []
  (let [name "my-map"
        shared-map (shared/get-map name)]

    (t/test-complete
     (.put shared-map "key" "value")
     (t/assert= (get (shared/get-map name) "key") "value")

     (.remove shared-map "key")
     (t/assert-nil (get (shared/get-map name) "key"))
     
     (t/assert (shared/remove-map name)))))


(defn test-set []
  (let [name "my-set"
        shared-set (shared/get-set name)]

    (t/test-complete
     (.add shared-set "value")
     (t/assert (contains? (shared/get-set name) "value"))

     (.remove shared-set "value")
     (t/assert (not (contains? (shared/get-set name) "value")))
     
     (t/assert (shared/remove-set name)))))

(defn test-set-add []
  (let [set (shared/get-set "foo")]
    (shared/add! set "a")
    (shared/add! set "b" "c")
    (t/test-complete
     (t/assert (contains? set "a"))
     (t/assert (contains? set "b"))
     (t/assert (contains? set "c")))))

(defn test-set-remove []
  (let [set (shared/get-set "foo")]
    (shared/add! set "a" "b" "c")
    (shared/remove! set "a")
    (t/test-complete
     (t/assert (not (contains? set "a")))
     (t/assert (contains? set "b"))
     (t/assert (contains? set "c"))
     (shared/remove! set "b" "c")
     (t/assert (not (contains? set "b")))
     (t/assert (not (contains? set "c"))))))

(defn test-map-put []
  (let [m (shared/get-map "foo")]
    (shared/put! m "a" "b")
    (shared/put! m "c" "d" "e" "f")
    (t/test-complete
     (t/assert= "b" (get m "a"))
     (t/assert= "d" (get m "c"))
     (t/assert= "f" (get m "e")))))

(defn test-map-put-invalid-args []
  (try
    (shared/put! (shared/get-map "blah") :a)
    (catch IllegalArgumentException _
      (t/test-complete))))

(t/start-tests)

