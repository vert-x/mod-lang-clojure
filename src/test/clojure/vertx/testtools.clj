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

(ns vertx.testtools
  (:refer-clojure :exclude [assert])
  (:require [vertx.core :as core]
            [vertx.utils :as utils]
            [vertx.buffer :as buf]
            [clojure.java.io :as io])
  (:import [org.vertx.testtools VertxAssert]))

(defn start-tests
  ([]
     (start-tests (fn [f] (f))))
  ([fixture]
     (VertxAssert/initialize (core/get-vertx))
     (fixture (ns-resolve *ns*
                          (symbol
                           ((core/config) :methodName))))))

(def teardown (atom []))

(defn on-complete [f]
  (swap! teardown conj f))

(defn test-complete*
  ([]
     (test-complete* #()))
  ([f]
     (try
       (f)
       (doseq [td @teardown]
         (td))
       (reset! teardown [])
       (finally (VertxAssert/testComplete)))))

(defmacro test-complete [& body]
  `(test-complete* (fn [] ~@body)))

(defn assert [cond]
  (VertxAssert/assertTrue (boolean cond)))

(defn assert= [exp actual]
  (VertxAssert/assertEquals exp actual))

(defn assert-nil [given]
  (VertxAssert/assertNull given))

(defn assert-not-nil [given]
  (VertxAssert/assertNotNull given))

(defn random-byte []
  (byte (- (int (* (rand) 255)) 128)))

(defn random-byte-array [length]
  (let [arr (byte-array length)]
    (dotimes [n length]
      (aset-byte arr n (random-byte)))
    arr))

(defn random-buffer [length]
  (buf/buffer (random-byte-array length)))

(defn a=
  "Compares java arrays for equality."
  [& args]
  (apply = (map (partial into '()) args)))

(defn resource-path [name]
  (.getCanonicalPath (io/file (io/resource name))))
