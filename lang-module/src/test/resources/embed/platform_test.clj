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

(ns vertx.platform-test
  (:require [vertx.testtools :as t]
            [vertx.embed.platform :as p]
            [vertx.core :as core]
            [vertx.http :as http]))

(def ^:dynamic *pm* nil)

(defmacro with-platform-manager
  [& body]
  `(let [pm# (p/platform-manager)]
     (binding [*pm* pm#]
       ~@body)))

(defn test-deploy-verticle []
  (with-platform-manager
    (let [cfg {:port 6789}]
      (p/deploy-verticle *pm* "config_to_eb.clj"
        :config cfg
        :handler (fn [err _]
                   (t/assert-nil err)
                   (core/timer 1000
                     (-> (http/client cfg)
                       (http/get-now "/"
                         #(http/on-body %
                            (fn [resp] (t/test-complete
                                        (t/assert= cfg (-> resp str read-string)))))))))))))

(t/start-tests)
