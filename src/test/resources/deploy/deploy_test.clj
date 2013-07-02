(ns vertx.deploy-test
  (:require [vertx.testtools :refer :all]
            [vertx.core :as core]
            [vertx.eventbus :as eb]))

(defn test-deploy []
  (eb/register-handler "test.data"
                       (completing-handler
                        [m]
                        (assert= "started" (.body m))))
  
  (core/deploy-verticle* "deploy/child.clj" {:config {:ham "biscuit"}}))

(start-tests)

