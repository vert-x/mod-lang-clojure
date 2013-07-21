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

(ns example.route-match.router
  (:require [vertx.http :as http]
            [vertx.http.route :as rm]))

(let [router (rm/matcher)]
  (doto router
    (rm/match :GET "/details/:user/:id"
           (fn [req]
             (let [params (http/params req)
                   resp (http/server-response req)]
               (http/end resp (str "User: " (:user params) "ID: " (:id params))))))

    ;;Catch all - serve the index page
    (rm/match :ALL #".*"
           (fn [req]
             (http/send-file (http/server-response req) "route_match/index.html"))))

  (-> (http/server)
      (http/on-request router)
      (http/listen 8080 "localhost" (println "Starting Http server on localhost:8080"))))
