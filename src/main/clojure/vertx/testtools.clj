(ns vertx.testtools
  (:require [vertx.core :as core]
            [vertx.utils :as utils])
  (:import [org.vertx.testtools VertxAssert]))

(defn start-tests []
  (VertxAssert/initialize core/!vertx)
  ((ns-resolve *ns*
               (symbol
                (get (core/config) :methodName)))))

(defn test-complete []
  (VertxAssert/testComplete))

(defn assert [cond]
  (VertxAssert/assertTrue (boolean cond)))

(defn assert= [exp actual]
  (VertxAssert/assertEquals exp actual))

(defmacro completing-handler [bindings & body]
  `(core/handle
    ~bindings
    (try
      ~@body
      (finally
        (test-complete)))))
