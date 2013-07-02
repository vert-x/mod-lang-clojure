(ns vertx.timer-test
  (:require [vertx.testtools :as t]
            [vertx.core :as core]))

(defn test-timer-macro []
  (core/set-timer
   10
   (t/test-complete)))

(defn test-timer-fn []
  (core/set-timer*
   10
   (fn [id]
     (t/test-complete
      (t/assert-not-nil id)))))

(defn test-periodic-fn []
  (let [count (atom 1)
        total 10]
    (core/set-periodic*
     10
     (fn [id]
       (swap! count inc)
       (when (= @count total)
         (core/cancel-timer id)
         ;; end test in another timer to catch if this timer keeps running
         (core/set-timer 100 (t/test-complete)))
       (when (> @count total)
         (throw (IllegalStateException. "fired too many times")))))))

(t/start-tests)
