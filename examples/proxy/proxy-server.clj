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

(ns example.proxy.proxy-server
  (:require [vertx.http :as http]
            [vertx.stream :as stream]
            [vertx.buffer :as buf]))

(defn client-respense-handler [server-req client-resp]
  (println "Proxying response: " (http/status-code client-resp))
  (let [server-resp (http/server-response server-req
                                          {:status-code (http/status-code client-resp)
                                           :chunked true})]
    (.set (.headers server-req) (.headers client-resp))
    (stream/on-data client-resp #((println "Proxying response body:" %)
                      (http/write (http/server-response server-req) %)))
    (stream/on-end client-resp #(http/end server-resp))))

(defn req-handler [client req]
  (println "Proxying request: " (http/uri req))
  (let [client-req (http/request client
                                 (http/method req)
                                 (http/uri req)
                                 (partial client-respense-handler req))]
    (.set (.headers client-req) (.headers req))
    (http/request-prop client-req {:chunked true})
    (stream/on-data req #((println "Proxying request body:" %)
                          (http/write client-req %)))

    (stream/on-end req #((println "end of the request")
                         (http/end client-req)))))


(let [client (http/client {:port 8282 :host "localhost"})
      server (http/server)]

  (doto server
    (http/on-request (partial req-handler client))
    (http/listen 8080 "localhost")))
