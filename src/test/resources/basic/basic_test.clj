(ns vertx.basic-test
  (:require [vertx.testtools :refer :all]
            [vertx.core :as core]))

(defn test-a-simple-deployment-should-work []
  (core/deploy-verticle*
   "basic/app.clj"
   {:handler
    (completing-handler [deploy-id]
                        (assert (not (nil? deploy-id)))
                        (assert (resolve 'basic.app/value)))}))

(start-tests)

