(ns vertx.buffer-test
  (:require [vertx.buffer :as b]
            [vertx.testtools :as t]
            [clojure.string :as str]))

(defn test-delimited-parsing []
  (let [lines (atom [])]
    (b/parse-delimited
     (->> (range 100)
          (map #(str "line " %))
          (str/join "\n")
          b/buffer)
     "\n" #(swap! lines conj (str %)))

    (doall
     (map-indexed (fn [idx line]
                    (t/assert= (str "line " idx) line))
                  @lines)))
  (t/test-complete))


(defn test-fixed-parsing []
  (let [lines (atom [])]
    (b/parse-fixed
     (->> (range 100)
          (map #(format "line %03d" %))
          str/join
          b/buffer)
     8 #(swap! lines conj (str %)))

    (doall
     (map-indexed (fn [idx line]
                    (t/assert= (format "line %03d" idx) line))
                  @lines)))
  (t/test-complete))

(t/start-tests)

