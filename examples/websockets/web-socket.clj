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

(ns example.webscokets.web-socket
  (:require [vertx.http :as http]
            [vertx.stream :as stream]))

(defn ws-data-handler [ws buf]
  (http/write-text-frame ws (.toString buf)))

(-> (http/server)
    (http/on-websocket
     (fn [ws] (if (= "/myapp" (http/path ws))
                (stream/on-data ws (partial ws-data-handler ws))
                (http/reject ws))))

    (http/on-request (fn [req]
                       (when (= "/" (http/path req))
                         (http/send-file (http/server-response req) "websockets/ws.html"))))
    (http/listen 8080 "localhost" (println "String http Server on localhost:8080")))
