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
            [vertx.http.route :as rm]))

(defn test-matcher []
  (letfn [(req-handler [params req]
            (t/assert= params (http/params req))
            (http/end (http/server-response req {:status-code 200})))

          (no-match-handler [req]
            (http/end (http/server-response req) "Not Found"))

          (client-resp-no-match [resp]
            (t/assert= (int 200) (.statusCode resp))
            (http/on-body resp (fn [buf] (t/test-complete
                                           (t/assert= (.toString buf) "Not Found")))))

          (client-resp-handler-regx [client resp]
            (t/assert= (int 200) (.statusCode resp))
            (http/end (http/request client :PUT "no-such-uri" (partial client-resp-no-match))))

          (client-resp-handler [client resp]
            (t/assert= (int 200) (.statusCode resp))
            (http/end (http/request client :GET "/bar/v0.2" (partial client-resp-handler-regx client))))

          (server-listen-handler [orig-server port host matcher err server]
            (t/assert-nil err)
            (t/assert= orig-server server)
            (let [client (http/client {:port port :host host})
                  params1 {:name "foo" :version "v0.1"}
                  pattern1 "/mod/:name/:version/"
                  params2 {:param0 "bar" :param1 "v0.2"}
                  pattern2 #"/([^/]+)/([^/]+)"]

              (rm/post matcher pattern1 (partial req-handler params1))
              (rm/get matcher pattern2 (partial req-handler params2))
              (rm/no-match matcher no-match-handler)

              (http/end (http/request client :POST "/mod/foo/v0.1/"
                                      (partial client-resp-handler client)))))
          ]
    (let [server (http/server)
          matcher (rm/matcher)
          port 8080
          host "localhost"]
      (-> server
          (http/on-request matcher)
          (http/listen port host
                       (partial server-listen-handler server port host matcher))))))


(t/start-tests)
