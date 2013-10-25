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

(ns ^:no-doc vertx.testtools
  (:refer-clojure :exclude [assert])
  (:require [vertx.core :as core]
            [vertx.utils :as utils]
            [vertx.buffer :as buf]
            [vertx.embed :as embed]
            [clojure.java.io :as io])
  (:import org.vertx.testtools.VertxAssert
           [java.util.concurrent CountDownLatch TimeUnit]))

(defn start-tests
  ([]
     (start-tests (fn [f] (f))))
  ([fixture]
     (VertxAssert/initialize (core/get-vertx))
     (fixture (ns-resolve *ns*
                          (symbol
                           ((core/config) :methodName))))))

(def ^:private ^:dynamic *embedded-latch*
  "Used when doing embedded testing to signify test completion."
  nil)

(def test-timeout
  "The timeout to use when waiting for embedded tests to complete, in seconds.
   Override with -Dtest.timeout"
  (if-let [timeout-prop (System/getProperty "test.timeout")]
    (Integer/parseInt timeout-prop)
    10))

(defn as-embedded
  "Run tests with an embedded vertx.
   Useful as a fixture: (clojure.test/use-fixtures vertx.testtools/as-embedded).
   Call test-complete to signal the end of the test."
  [f]
  (binding [*embedded-latch* (CountDownLatch. 1)
            core/*vertx* (embed/vertx)]
    (try
      (f)
      (finally
        (if (.await *embedded-latch* test-timeout TimeUnit/SECONDS)
          (.stop core/*vertx*)
          (throw (Exception. "Timed out waiting for test to complete")))))))

(def ^:private teardown
  "Teardown functions to be called when the test is complete."
  (atom []))

(defn on-complete
  "Add a teardown function to be called when the test completes."
  [f]
  (swap! teardown conj f))

(defn test-complete*
  "Signals that a test is complete.
   If given a function, will signal after calling f. Calls any
   teardown functions specified by on-complete, clearing the teardown
   list when done."
  ([]
     (test-complete* #()))
  ([f]
     (try
       (f)
       (doseq [td @teardown]
         (td))
       (reset! teardown [])
       (finally
         (if *embedded-latch*
           (.countDown *embedded-latch*)
           (VertxAssert/testComplete))))))

(defmacro test-complete
  "A convenience macro wrapping a body in a fn and passing it to test-complete*."
  [& body]
  `(test-complete* (fn [] ~@body)))

(defn assert [cond]
  "Assert using VertxAssert. Don't use when running an embedded test."
  (VertxAssert/assertTrue (boolean cond)))

(defn assert=
  "Assert equals using VertxAssert. Don't use when running an embedded test."
  [exp actual]
  (VertxAssert/assertEquals exp actual))

(defn assert-nil
  "Assert nil using VertxAssert. Don't use when running an embedded test."
  [given]
  (VertxAssert/assertNull given))

(defn assert-not-nil
  "Assert not nil using VertxAssert. Don't use when running an embedded test."
  [given]
  (VertxAssert/assertNotNull given))

(defn random-byte
  "Creates a random byte."
  []
  (byte (- (int (* (rand) 255)) 128)))

(defn random-byte-array
  "Creates a randomly-filled byte array of the given length."
  [length]
  (let [arr (byte-array length)]
    (dotimes [n length]
      (aset-byte arr n (random-byte)))
    arr))

(defn random-buffer
  "Creates a randomly-filled buffer of the given length."
  [length]
  (buf/buffer (random-byte-array length)))

(defn a=
  "Compares java arrays for equality."
  [& args]
  (apply = (map (partial into '()) args)))

(defn resource-path
  "Looks up the given name as a resource, returning its canonical path as a String."
  [name]
  (.getCanonicalPath (io/file (io/resource name))))

(defn wait-for
  "Waits for (t) to be true before invoking f, if passed. Evaluates
   test every 100 ms attempts times before giving up. Attempts
   defaults to 300. Passing :forever for attempts will loop until the
   end of time or (t) is true, whichever comes first."
  ([t]
     (wait-for t (constantly true)))
  ([t f]
     (wait-for t f 300))
  ([t f attempts]
     (let [wait #(Thread/sleep 100)]
       (cond
        (t)                   (f)
        (= :forever attempts) (do
                                (wait)
                                (recur t f :forever))
        (< attempts 0)        (throw (IllegalStateException.
                                      (str "Gave up waiting for " t)))
        :default              (do
                                (wait)
                                (recur t f (dec attempts)))))))
