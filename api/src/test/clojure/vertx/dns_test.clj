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
           org.vertx.java.core.dns.MxRecord
           org.vertx.testtools.TestDnsServer))

(use-fixtures :each t/as-embedded)

(defn ^{:test t/test-complete*} start-server [server]
  (.start server)
  (t/on-complete
   #(.stop server))
  (-> server
      .getTransports
      first
      .getAcceptor
      .getLocalAddress))

(deftest lookup-with-no-type
  (let [ip "10.0.0.1"]
    (d/lookup (start-server
               (TestDnsServer/testLookup ip))
              "example.com"
              (fn [err r]
                (t/test-complete
                 (is (nil? err))
                 (is (= ip (:address r)))
                 (is (instance? InetAddress (:basis r))))))))

(deftest lookup-with-:any
  (let [ip "10.0.0.1"]
    (d/lookup (start-server
               (TestDnsServer/testLookup ip))
              "example.com" :any
              (fn [err r]
                (t/test-complete
                 (is (nil? err))
                 (is (= ip (:address r)))
                 (is (instance? InetAddress (:basis r))))))))

(deftest lookup-with-:ipv4
  (let [ip "10.0.0.1"]
    (d/lookup (start-server
               (TestDnsServer/testLookup4 ip))
              "example.com" :ipv4
              (fn [err r]
                (t/test-complete
                 (is (nil? err))
                 (is (= ip (:address r)))
                 (is (instance? InetAddress (:basis r))))))))

(deftest lookup-with-:ipv6
  (let [ip "0:0:0:0:0:0:0:1"]
    (d/lookup (start-server
               (TestDnsServer/testLookup6))
              "example.com" :ipv6
              (fn [err r]
                (t/test-complete
                 (is (nil? err))
                 (is (= ip (:address r)))
                 (is (instance? InetAddress (:basis r))))))))

(deftest lookup-with-invalid-host-name
  (d/lookup (start-server
             (TestDnsServer/testLookupNonExisting))
            "not-exist"
            (fn [err r]
              (t/test-complete
               (is err)
               (is (instance? DnsException (:basis err)))
               (is (= :NXDOMAIN (:type err)))
               (is (nil? r))))))

(defn ^{:test t/test-complete*} check-resolve-result [check-fn err r]
  (t/test-complete
   (is (nil? err))
   (is (seq? r))
   (mapv check-fn r)))

(deftest resolve-:A
  (let [ip "10.0.0.1"]
    (d/resolve (start-server
                (TestDnsServer/testResolveA ip))
               "example.com" :A
               (partial check-resolve-result
                      (fn [r]
                        (is (= ip (:address r)))
                        (is (instance? InetAddress (:basis r))))))))

(deftest resolve-:AAAA
    (let [ip "0:0:0:0:0:0:0:1"]
    (d/resolve (start-server
                (TestDnsServer/testResolveAAAA "::1"))
               "example.com" :AAAA
               (partial check-resolve-result
                      (fn [r]
                        (is (= ip (:address r)))
                        (is (instance? InetAddress (:basis r))))))))

(deftest resolve-:CNAME
  (let [cname "cname.example.com"]
    (d/resolve (start-server
                (TestDnsServer/testResolveCNAME cname))
               "example.com" :CNAME
               (partial check-resolve-result
                        #(is (= cname %))))))

(deftest resolve-:MX
  (let [mx "mail.example.com"
        prio 10]
    (d/resolve (start-server
                (TestDnsServer/testResolveMX prio mx))
               "example.com" :MX
               (partial check-resolve-result
                        (fn [r]
                          (is (= prio (:priority r)))
                          (is (= mx (:name r)))
                          (is (instance? MxRecord (:basis r))))))))

(deftest resolve-:NS
  (let [ns "ns.example.com"]
    (d/resolve (start-server
                (TestDnsServer/testResolveNS ns))
               "example.com" :NS
               (partial check-resolve-result
                        #(is (= ns %))))))

(deftest resolve-:PTR
  (let [ptr "ptr.example.com"]
    (d/resolve (start-server
                (TestDnsServer/testResolvePTR ptr))
               "example.com" :PTR
               (fn [err r]
                 (t/test-complete
                  (is (nil? err))
                  (is (= ptr r)))))))

(deftest resolve-:SRV
  (let [prio 10
        weight 1
        port 80
        target "example.com"]
    (d/resolve (start-server
                (TestDnsServer/testResolveSRV prio weight port target))
               "example.com" :SRV
               (partial check-resolve-result
                        (fn [r]
                          (is (= prio (:priority r)))
                          (is (= weight (:weight r)))
                          (is (= port (:port r)))
                          (is (= target (:target r))))))))

(deftest resolve-:TXT
  (let [txt "yadayada"]
    (d/resolve (start-server
                (TestDnsServer/testResolveTXT txt))
               "example.com" :TXT
               (partial check-resolve-result
                        #(is (= txt %))))))

(deftest reverse-lookup
  (let [addr "10.0.0.1"
        ptr "ptr.example.com"]
    (d/reverse-lookup
     (start-server (TestDnsServer/testReverseLookup ptr))
     addr
     (fn [err {:keys [address host basis]}]
       (t/test-complete
        (is (nil? err))
        (is (= ptr host))
        (is (= addr address))
        (is (instance? InetAddress basis)))))))

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



  
  
