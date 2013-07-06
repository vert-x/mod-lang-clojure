(ns vertx.utils-test
  (:require [vertx.utils :refer :all]
            [clojure.test :refer :all])
  (:import io.vertx.test.Biscuit))

(deftest camelize-should-work
  (are [given exp] (= (camelize given) exp)
       :foo-bar  "fooBar"
       'foo-bar  "fooBar"
       "foo-bar" "fooBar"
       :foo_bar  "fooBar"
       :foo-bar-baz "fooBarBaz"
       :foo-BAR "fooBAR"
       :foo-bAR "fooBAR"
       :foo-b "fooB"
       :f "f"))

(deftest set-property-should-work
  (let [b (Biscuit.)]
    (is (nil? (.getJam b)))
    (is (= b (set-property b :jam "grape")))
    (is (= "grape" (.getJam b)))))

(deftest set-properties-should-work
  (let [b (Biscuit.)]
    (is (nil? (.getJam b)))
    (is (= b (set-properties b {:jam "grape"})))
    (is (= "grape" (.getJam b)))))
