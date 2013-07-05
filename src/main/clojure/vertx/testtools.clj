(ns vertx.testtools
  (:refer-clojure :exclude [assert])
  (:require [vertx.core :as core]
            [vertx.utils :as utils]
            [vertx.buffer :as buf])
  (:import [org.vertx.testtools VertxAssert]))

(defn start-tests []
  (VertxAssert/initialize (core/get-vertx))
  ((ns-resolve *ns*
               (symbol
                ((core/config) :methodName)))))

(defn test-complete* [f]
  (try
    (f)
    (finally (VertxAssert/testComplete))))

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
