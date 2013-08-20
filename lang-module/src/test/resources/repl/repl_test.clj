(ns vertx.context-test
  (:require [vertx.repl :as repl]
            [vertx.testtools :as t]))

(defn test-repl-start []
  (let [id (repl/start-repl)]
    (t/assert-not-nil id)
    (t/test-complete (repl/stop-repl id))))

(t/start-tests)
