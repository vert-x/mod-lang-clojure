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

(ns vertx.routematcher-test
  (:require [vertx.testtools :as t]
            [vertx.http :as http]
            [vertx.core :as core]
            [vertx.routematcher :as rm]))

(def ^:dynamic *resp-num* (atom 0))

(defn test-matcher []
  (letfn [(req-handler [params req]
            (t/assert= params (http/params req))
            (http/end (http/server-response req {:status-code 200})))

          (client-resp-handler [resp]
            (t/assert= (int 200) (http/status-code resp))
            (swap! *resp-num* inc)
            (when (= 2 @*resp-num*)
              (t/test-complete)))

          (server-listen-handler [orig-server port host matcher err server]
            (t/assert-nil err)
            (t/assert= orig-server server)
            (let [client (http/client {:port port :host host})
                  params1 {:name "foo" :version "v0.1"}
                  pattern1 "/:name/:version"
                  params2 {:param0 "foo" :param1 "v0.1"}
                  pattern2 "\\/([^\\/]+)\\/([^\\/]+)"]

              (rm/match matcher :GET pattern1 (partial req-handler params1))
              (rm/match-regx matcher :GET pattern2 (partial req-handler params2))
              (dotimes [_ 2]
                (http/end (http/request client :GET "/foo/v0.1" client-resp-handler)))
              ))
          ]
    (let [server (http/server)
          matcher (rm/matcher)
          port 8080
          host "localhost"]
      (-> server
          (http/req-handler matcher)
          (http/listen port host
                       (partial server-listen-handler server port host matcher))))))

(t/start-tests)
