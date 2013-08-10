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

(ns vertx.deploy-test
  (:require [vertx.testtools :as t]
            [vertx.core :as core]
            [vertx.eventbus :as eb]))

(defn test-deploy []
  (eb/on-message
   "test.data"
   (fn [m]
     (t/test-complete
      (t/assert= "started" m))))
  
  (core/deploy-verticle "deploy/child.clj" {:ham "biscuit"}))

(defn test-deploy-with-handler []
  (core/deploy-verticle
   "deploy/child.clj" {:ham "biscuit"} 1
   (fn [err id]
     (t/test-complete
      (t/assert-nil err)
      (t/assert-not-nil id)))))

(defn test-deploy-failure []
  (core/deploy-verticle
   "deploy/does_not_exist.clj" nil 1
   (fn [err id]
     (t/test-complete
      (t/assert-not-nil err)
      (t/assert-nil id)))))

(defn test-undeploy []
  (eb/on-message
   "test.data"
   (fn [m]
     (when (= "stopped" m)
       (t/test-complete))))

  (core/deploy-verticle
   "deploy/child.clj" {:ham "biscuit"} 1
   (fn [err id]
     (t/assert-not-nil id)
     (core/undeploy-verticle id))))

(defn test-undeploy-with-handler []
  (core/deploy-verticle
   "deploy/child.clj" {:ham "biscuit"} 1
   (fn [err id]
     (t/assert-not-nil id)
     (core/undeploy-verticle
      id
      (fn [err]
        (t/test-complete
         (t/assert-nil err)))))))

(defn test-undeploy-failure []
  (core/undeploy-verticle
      "not-deployed"
      (fn [err]
        (t/test-complete
         (t/assert-not-nil err)))))

(t/start-tests)

