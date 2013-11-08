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

(ns example.udp.udp-group-sender
  (:require [vertx.core :as vertx]
            [vertx.datagram :as udp]))

(let [peer (udp/socket)]
  (doseq [i (range 10)]
    (let [s (format "hello %s\n" i)]
      (udp/send peer "group-data" "230.0.0.1" 1234
                (fn [err _]
                  (if (nil? err)
                    (println "send group packet:" s)
                    (println (.getMessage err)))))
      )))
