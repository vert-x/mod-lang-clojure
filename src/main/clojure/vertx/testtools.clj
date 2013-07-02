(ns vertx.testtools
  (:refer-clojure :exclude [assert])
  (:require [vertx.core :as core]
            [vertx.utils :as utils])
  (:import [org.vertx.testtools VertxAssert]))

(defn start-tests []
  (VertxAssert/initialize core/!vertx)
  ((ns-resolve *ns*
               (symbol
                (get (core/config) :methodName)))))

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
