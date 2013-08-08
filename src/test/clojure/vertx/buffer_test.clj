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
            [clojure.string :as str]
            [clojure.test :refer :all]))

(deftest test-buffer-create
  (is (= 0 (.length (b/buffer))))
  (is (= 0 (.length (b/buffer 100))))
  (is (= "abcdefg" (str (b/buffer "abcdefg")))))

(deftest test-append-buffer
  (let [b1! (t/random-buffer 100)
        b2! (b/buffer)]
    (is (identical? b2! (b/append! b2! b1!)))
    (is (= 100 (.length b2!)))
    (is (= b1! (b/get-buffer b2! 0 100)))))

(deftest test-append-byte
  (let [b! (b/buffer)
        data (byte 1)]
    (b/append! b! data)
    (is (= data (b/get-byte b! 0)))))

(deftest test-append-int
  (let [b! (b/buffer)
        data (int 1)]
    (b/append! b! data)
    (is (= data (b/get-int b! 0)))))

(deftest test-append-long
  (let [b! (b/buffer)
        data (long 1)]
    (b/append! b! data)
    (is (= data (b/get-long b! 0)))))

(deftest test-append-double
  (let [b! (b/buffer)
        data (double 1.0)]
    (b/append! b! data)
    (is (= data (b/get-double b! 0)))))

(deftest test-append-float
  (let [b! (b/buffer)
        data (float 1.0)]
    (b/append! b! data)
    (is (= data (b/get-float b! 0)))))

(deftest test-append-short
  (let [b! (b/buffer)
        data (short 1)]
    (b/append! b! data)
    (is (= data (b/get-short b! 0)))))

(deftest test-append-bytes
  (let [b! (b/buffer)
        data (.getBytes "ham")]
    (b/append! b! data)
    (is (t/a= data (b/get-bytes b!)))
    (is (t/a= data (b/get-bytes b! 0 (alength data))))))

(deftest test-append-big-decimal
  (let [b! (b/buffer)
        data 4.2M]
    (b/append! b! data)
    (is (= (double data) (b/get-double b! 0)))))

(deftest test-append-ratio
  (let [b! (b/buffer)
        data 22/7]
    (b/append! b! data)
    (is (= (double data) (b/get-double b! 0)))))

(deftest test-append-big-int
  (let [b! (b/buffer)
        data 42N]
    (b/append! b! data)
    (is (= (long data) (b/get-long b! 0)))))

(deftest test-append-string-default-encoding
  (let [b! (b/buffer)
        data "ham-biscuit"]
    (b/append! b! data)
    (is (= data (b/get-string b! 0 (.length data))))))

;; TODO: implement
(deftest test-append-string-other-encoding
  )

(deftest test-set-buffer
  (let [b1! (t/random-buffer 100)
        b2! (b/buffer)]
    (is (identical? b2! (b/set! b2! 1 b1!)))
    (is (= 101 (.length b2!)))
    (is (= b1! (b/get-buffer b2! 1 101)))))

(deftest test-set-byte
  (let [b! (b/buffer)
        data (byte 1)]
    (b/set! b! 1 data)
    (is (= data (b/get-byte b! 1)))))

(deftest test-set-int
  (let [b! (b/buffer)
        data (int 1)]
    (b/set! b! 1 data)
    (is (= data (b/get-int b! 1)))))

(deftest test-set-long
  (let [b! (b/buffer)
        data (long 1)]
    (b/set! b! 1 data)
    (is (= data (b/get-long b! 1)))))

(deftest test-set-double
  (let [b! (b/buffer)
        data (double 1.1)]
    (b/set! b! 1 data)
    (is (= data (b/get-double b! 1)))))

(deftest test-set-float
  (let [b! (b/buffer)
        data (float 1.1)]
    (b/set! b! 1 data)
    (is (= data (b/get-float b! 1)))))

(deftest test-set-short
  (let [b! (b/buffer)
        data (short 1)]
    (b/set! b! 1 data)
    (is (= data (b/get-short b! 1)))))

(deftest test-set-bytes
  (let [b! (b/buffer)
        data (.getBytes "ham")]
    (b/set! b! 1 data)
    (is (t/a= data (b/get-bytes b! 1 (+ 1 (alength data)))))))

(deftest test-set-big-decimal
  (let [b! (b/buffer)
        data 4.2M]
    (b/set! b! 1 data)
    (is (= (double data) (b/get-double b! 1)))))

(deftest test-set-ratio
  (let [b! (b/buffer)
        data 22/7]
    (b/set! b! 1 data)
    (is (= (double data) (b/get-double b! 1)))))

(deftest test-set-big-int
  (let [b! (b/buffer)
        data 42N]
    (b/set! b! 1 data)
    (is (= (long data) (b/get-long b! 1)))))

(deftest test-set-string-default-encoding
  (let [b! (b/buffer)
        data "ham-biscuit"]
    (b/set! b! 1 data)
    (is (= data (b/get-string b! 1 (+ 1 (.length data)))))))

;; TODO: implement
(deftest test-set-string-other-encoding
  )

(deftest test-as-buffer-buffer
  (let [b1! (t/random-buffer 100)
        b2! (b/as-buffer b1!)]
    (is (identical? b2! b1!))))

(deftest test-as-buffer-byte
  (let [data (byte 1)
        b! (b/as-buffer data)]
    (is (= data (b/get-byte b! 0)))))

(deftest test-as-buffer-int
  (let [data (int 1)
        b! (b/as-buffer data)]
    (is (= data (b/get-int b! 0)))))

(deftest test-as-buffer-long
  (let [data (long 1)
        b! (b/as-buffer data)]
    (is (= data (b/get-long b! 0)))))

(deftest test-as-buffer-double
  (let [data (double 1.0)
        b! (b/as-buffer data)]
    (is (= data (b/get-double b! 0)))))

(deftest test-as-buffer-float
  (let [data (float 1.0)
        b! (b/as-buffer data)]
    (is (= data (b/get-float b! 0)))))

(deftest test-as-buffer-short
  (let [data (short 1)
        b! (b/as-buffer data)]
    (is (= data (b/get-short b! 0)))))

(deftest test-as-buffer-bytes
  (let [data (.getBytes "ham")
        b! (b/as-buffer data)]
    (is (t/a= data (b/get-bytes b!)))
    (is (t/a= data (b/get-bytes b! 0 (alength data))))))

(deftest test-as-buffer-big-decimal
  (let [data 4.2M
        b! (b/as-buffer data)]
    (is (= (double data) (b/get-double b! 0)))))

(deftest test-as-buffer-ratio
  (let [data 22/7
        b! (b/as-buffer data)]
    (is (= (double data) (b/get-double b! 0)))))

(deftest test-as-buffer-big-int
  (let [data 42N
        b! (b/as-buffer data)]
    (is (= (long data) (b/get-long b! 0)))))

(deftest test-as-buffer-string
  (let [data "ham-biscuit"
        b! (b/as-buffer data)]
    (is (= data (b/get-string b! 0 (.length data))))))

(deftest test-delimited-parsing
  (let [lines (atom [])]
    (b/parse-delimited
     (->> (range 100)
          (map #(str "line " %))
          (str/join "\n")
          b/buffer)
     "\n" #(swap! lines conj (str %)))

    (doall
     (map-indexed (fn [idx line]
                    (is (= (str "line " idx) line)))
                  @lines))))


(deftest test-fixed-parsing
  (let [lines (atom [])]
    (b/parse-fixed
     (->> (range 100)
          (map #(format "line %03d" %))
          str/join
          b/buffer)
     8 #(swap! lines conj (str %)))

    (doall
     (map-indexed (fn [idx line]
                    (is (= (format "line %03d" idx) line)))
                  @lines))))
