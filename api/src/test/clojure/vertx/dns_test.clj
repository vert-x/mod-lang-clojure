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

(ns vertx.dns-test
  (:require [vertx.dns :as d]
            [vertx.testtools :as t]
            [clojure.string :as str]
            [clojure.test :refer :all])
  (:import org.vertx.java.core.dns.DnsException
           java.net.InetAddress
           (org.vertx.java.core.dns MxRecord)))

(use-fixtures :each t/as-embedded)

(def v4-regex #"^(\d+\.){3}\d+$")
(def v6-regex #"^([^:]+:){7}[^:]+$")

(def server "8.8.8.8")

(deftest lookup-with-no-type
  (d/lookup server "google.com"
            (fn [err r]
              (t/test-complete
               (is (nil? err))
               (is (:address r))
               (is (:basis r))
               (is (instance? InetAddress (:basis r)))))))

(deftest lookup-with-:any
  (d/lookup server "google.com" :any
            (fn [_ r]
              (t/test-complete
               (is (:address r))))))

(deftest lookup-with-:ipv4
  (d/lookup server "google.com" :ipv4
            (fn [_ r]
              (t/test-complete
               (is (:address r))
               (is (re-find v4-regex (:address r)))))))

(deftest lookup-with-:ipv6
  (d/lookup server "google.com" :ipv6
            (fn [_ r]
              (t/test-complete
               (is (:address r))
               (is (re-find v6-regex (:address r)))))))

(deftest lookup-with-invalid-host-name
  (d/lookup server "this-should-never-resolve-i-hope.biscuit"
            (fn [err r]
              (t/test-complete
               (is err)
               (is (instance? DnsException (:basis err)))
               (is (= :NXDOMAIN (:type err)))
               (is (nil? r))))))

(defn check-resolve-result [check-fn err r]
  (t/test-complete
   (is (nil? err))
   (is (seq? r))
   (mapv check-fn r)))

(deftest resolve-:A
  (d/resolve server "google.com" :A
             (partial check-resolve-result
                      (fn [r]
                        (is (re-find v4-regex (:address r)))
                        (is (instance? InetAddress (:basis r)))))))

(deftest resolve-:AAAA
  (d/resolve server "google.com" :AAAA
             (partial check-resolve-result
                      (fn [r]
                        (is (re-find v6-regex (:address r)))
                        (is (instance? InetAddress (:basis r)))))))

(deftest resolve-:CNAME
  (d/resolve server "downloads.immutant.org" :CNAME
             (partial check-resolve-result
                      #(is (string? %)))))

(deftest resolve-:MX
  (d/resolve server "google.com" :MX
             (partial check-resolve-result
                      (fn [r]
                        (is (:priority r))
                        (is (:name r))
                        (is (instance? MxRecord (:basis r)))))))

(deftest resolve-:NS
  (d/resolve server "google.com" :NS
             (partial check-resolve-result
                        #(is (string? %)))))

(deftest resolve-:PTR
  (d/resolve server "downloads.immutant.org" :PTR
             (fn [err r]
               (t/test-complete
                (is (nil? err))
                (is (string? r))))))

;; TODO: test SRV

(deftest resolve-:TXT
  (d/resolve server "google.com" :TXT
             (partial check-resolve-result
                      #(is (string? %)))))

(deftest reverse-lookup
  (let [c server]
    (d/lookup c "google.com"
              (fn [_ {:keys [address]}]
                (d/reverse-lookup c address
                                  (fn [err {:keys [host basis]}]
                                    (t/test-complete
                                     (is (nil? err))
                                     (is host)
                                     (is basis)
                                     (is (instance? InetAddress basis)))))))))

(deftest reverse-lookup-for-nonexistent-address
  (d/reverse-lookup server "123"
                    (fn [err r]
                      (t/test-complete
                       (is err)
                       (is (instance? DnsException (:basis err)))
                       (is (= :NXDOMAIN (:type err)))
                       (is (nil? r))))))

(deftest sock-address-from-string
  (let [sock-address-from-string #'d/sock-address-from-string]
    (testing "with a port and host"
      (let [addr (sock-address-from-string "foo:1234")]
        (is (= "foo" (.getHostString addr)))
        (is (= 1234 (.getPort addr)))))
    
    (testing "with no port"
      (let [addr (sock-address-from-string "foo")]
        (is (= "foo" (.getHostString addr)))
        (is (= 53 (.getPort addr))))))
  (t/test-complete))

