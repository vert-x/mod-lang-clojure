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

(ns vertx.repl-test
  (:require [vertx.testtools :as t]
            [vertx.repl :as repl]
            [clojure.java.io :as io]
            [clojure.tools.nrepl :as nrepl]
            [clojure.test :refer [deftest is use-fixtures]]))

(use-fixtures :each t/as-embedded)

(deftest start-repl-should-return-an-id
  (let [id (repl/start-repl)]
    (is id)
    (t/test-complete (repl/stop-repl id))))

(deftest start-repl-should-write-port-file
  (let [id (repl/start-repl)]
    (is (t/wait-for #(.exists (io/file ".nrepl-port"))))
    (t/test-complete (repl/stop-repl id))))

(deftest start-repl-should-actually-provide-a-repl
  (let [id (repl/start-repl 4321)]
    ;; wait for the port file to be written; at that point,
    ;; nrepl should be bound to the port
    (t/wait-for #(.exists (io/file ".nrepl-port")))
    (with-open [conn (nrepl/connect :port 4321)]
    (let [client (nrepl/client conn 120000)]
      (is (= "success"
             (first (nrepl/response-values
                     (nrepl/message
                      client
                      {:op :eval :code "(str \"success\")"})))))))
    (t/test-complete (repl/stop-repl id))))




