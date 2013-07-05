(ns vertx.shareddata-test
  (:require [vertx.shareddata :as shared]
            [vertx.testtools :as t]))

(defn test-map []
  (let [name "my-map"
        shared-map (shared/get-map name)]
    (do
     (.put shared-map "key" "value")
     (t/assert= (.get (shared/get-map name) "key") "value")

     (.remove shared-map "key")
     (t/assert-nil (.get (shared/get-map name) "key"))
     
     (t/assert (shared/remove-map name))))
  (t/test-complete))


(defn test-set []
  (let [name "my-set"
        shared-set (shared/get-set name)]
    (do
     (.add shared-set "value")
     (t/assert (.contains (shared/get-set name) "value"))

     (.remove shared-set "value")
     (t/assert (not (.contains (shared/get-set name) "value")))
     
     (t/assert (shared/remove-set name))))
  (t/test-complete))

(t/start-tests)

