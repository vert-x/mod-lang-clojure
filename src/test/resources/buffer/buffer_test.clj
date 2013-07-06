;; Copyright 2013 the original author or authors.
;; 
;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at
;; 
;;      http://www.apache.org/licenses/LICENSE-2.0
;; 
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.

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

