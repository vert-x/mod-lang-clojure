(ns vertx.testtools
  (:require [vertx.core :as core]
            [vertx.utils :as utils])
  (:import [org.vertx.testtools VertxAssert]))

(defn init [vertx]
  (VertxAssert/initialize vertx))

(defn start-tests []
  (init core/vertx)
  ((ns-resolve *ns*
               (symbol
                (-> (.config core/container)
                    utils/json->map
                    (get "methodName"))))))

(defn test-complete []
  (VertxAssert/testComplete))

(defn assert [cond]
  (VertxAssert/assertTrue (boolean cond)))

(defmacro completing-handler [bindings & body]
  `(core/handle
    ~bindings
    (try
      ~@body
      (finally
        (test-complete)))))
