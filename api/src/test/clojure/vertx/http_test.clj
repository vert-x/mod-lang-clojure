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

(ns vertx.http-test
  (:require [vertx.http :as http]
            [vertx.http.websocket :as websocket]
            [vertx.buffer :as buf]
            [vertx.stream :as stream]
            [vertx.testtools :as t]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is use-fixtures]]))

(use-fixtures :each t/as-embedded)

(defn assert-status-code [resp]
  (is (= (int 200) (.statusCode resp))))

(deftest base-request
  (letfn [(req-handler [req]
            (let [header (http/headers req)
                  addr (http/remote-address req)]
              (is (= :GET (http/request-method req)))
              (is (= "/get/now?k=v" (.uri req)))
              (is (= "/get/now" (.path req)))
              (is (= "k=v" (.query req)))
              (is (= {:k "v"} (http/params req)))
              (is (= "dummy" (:dummy header)))
              (is (= ["v1" "v2"] (:dummy-v header)))
              (is (= (:host addr) "127.0.0.1"))
              (is (> (:port addr) -1))
              (is (.absoluteURI req)))

            (let [resp (http/server-response req {:status-code 200
                                                  :status-message "status-msg"
                                                  :chunked false})]
              (doto resp
                (http/add-header "add-header" "add-header")
                (http/end "body-content"))))

          (client-request [client]
            (http/get-now client "/get/now?k=v" {:dummy "dummy"
                                                 :dummy-v ["v1" "v2"]}
              (fn [resp]
                (assert-status-code resp)
                (is (= "status-msg" (.statusMessage resp)))
                (is (= "add-header" (:add-header (http/headers resp))))
                (http/on-body
                  resp (fn [buf]
                         (t/test-complete
                           (is (= (buf/buffer "body-content") buf))))))))

          (server-listen-handler [orig-server port host err server]
            (is (not err))
            (is (= orig-server server))
            (client-request (http/client {:port port :host host :try-use-compression true})))]

    (let [server (http/server {:compression-supported true}) port 8888 host "localhost"]
      (-> server
        (http/on-request req-handler)
        (http/listen port host (partial server-listen-handler server port host))))))


(deftest form-request
  (letfn [(req-handler [req]
            (is (.startsWith (.uri req) "/form"))
            (http/expect-multi-part req)
            (let [resp (http/server-response req {:chunked true})]
              (stream/on-end req (fn []
                                   (let [forms (http/form-attributes req)]
                                     (prn forms)
                                     (is (= "junit-testUserAlias" (:origin forms)))
                                     (is (= "admin@foo.bar" (:login forms)))
                                     (is (= "admin" (:pass-word forms)))
                                     (http/end resp))))))

          (server-listen-handler [orig-server port host err server]
            (is (not err))
            (is (= orig-server server))
            (let [body "origin=junit-testUserAlias&login=admin%40foo.bar&pass+word=admin"]
              (-> (http/client {:port port :host host})
                (http/post "/form"
                  (fn [resp]
                    (assert-status-code resp)
                    (http/on-body resp
                      (fn [body]
                        (t/test-complete
                          (is (= (int 0) (.length body))))))))

                (http/add-header :content-length (.length body))
                (http/add-header :content-type (str "application/x-www-form-urlencoded"))
                (http/end body))))]

    (let [server (http/server) port 8888 host "localhost"]
      (-> server
        (http/on-request req-handler)
        (http/listen port host (partial server-listen-handler server port host))))))



(deftest upload-request
  (letfn [(req-handler [req]
            (is (.startsWith (.uri req) "/form"))
            (http/expect-multi-part req)
            (let [resp (http/server-response req {:chunked true})]
              (http/on-upload req
                (fn [file-info]
                  (is (= "file" (:name file-info)))
                  (is (= "tmp-0.txt" (:filename file-info)))
                  (is (= "image/gif" (:content-type file-info)))
                  (stream/on-data
                    (:basis file-info)
                    (fn [data]
                      (is (= (buf/buffer "Vert.x Rocks!") data))))))

              (stream/on-end req (fn []
                                   (let [forms (http/form-attributes req)]
                                     (is (= (int 0) (count forms)))
                                     (http/end resp))))))

          (server-listen-handler [orig-server port host err server]
            (is (not err))
            (is (= orig-server server))
            (let [boundary "dLV9Wyq26L_-JQxk6ferf-RT153LhOO"
                  body (str "--" boundary "\r\n"
                         "Content-Disposition: form-data; name=\"file\"; filename=\"tmp-0.txt\"\r\n"
                         "Content-Type: image/gif\r\n"
                         "\r\n"
                         "Vert.x Rocks!\r\n"
                         "--" boundary "--\r\n")]
              (-> (http/client {:port port :host host})
                (http/post "/form"
                  (fn [resp]
                    (assert-status-code resp)
                    (http/on-body resp
                      (fn [body]
                        (t/test-complete
                          (is (= (int 0) (.length body))))))))

                (http/add-header :content-length (str (.length body)))
                (http/add-header :content-type (str "multipart/form-data; boundary="boundary))
                (http/end body))))]

    (let [server (http/server) port 8888 host "localhost"]
      (-> server
        (http/on-request req-handler)
        (http/listen port host (partial server-listen-handler server port host))))))


(deftest ssl-request
  (letfn [(req-handler [req]
            (is (= :GET (http/request-method req)))
            (is (= "/get/ssl/" (.uri req)))
            (let [resp (http/server-response req)]
              (http/end resp "body-content")))

          (server-listen-handler [orig-server port host err server]
            (is (not err))
            (is (= orig-server server))
            (-> (http/client {:host "localhost"
                              :port 4043
                              :SSL true
                              :trust-all true
                              :key-store-path (t/resource-path "keystores/client-keystore.jks")
                              :key-store-password "wibble"
                              :trust-store-path (t/resource-path "keystores/client-truststore.jks")
                              :trust-store-password "wibble"})
              (http/get "/get/ssl/"
                (fn [resp]
                  (assert-status-code resp)
                  (http/on-body resp
                    (fn [buf]
                      (t/test-complete
                        (is (= (buf/buffer "body-content") buf)))))))
              (http/end)))]

    (let [server (http/server {:SSL true
                               :key-store-path (t/resource-path "keystores/server-keystore.jks")
                               :key-store-password "wibble"
                               :trust-store-path (t/resource-path "keystores/server-truststore.jks")
                               :trust-store-password "wibble"
                               :client-auth-required true})
          port 4043
          host "localhost"]
      (-> server
        (http/on-request req-handler)
        (http/listen port host
          (partial server-listen-handler server port host))))))


(deftest compression-request
  (letfn [(req-handler [req]
            (is (= :GET (http/request-method req)))
            (is (= "/get/compression/" (.uri req)))
            (let [resp (http/server-response req)]
              (http/end resp "body-content")))

          (server-listen-handler [orig-server port host err server]
            (is (not err))
            (is (= orig-server server))
            (-> (http/client {:host "localhost"
                              :port 4043
                              :try-use-compression true})
              (http/get "/get/compression/"
                (fn [resp]
                  (assert-status-code resp)
                  (http/on-body resp
                    (fn [buf]
                      (t/test-complete
                        (is (= (buf/buffer "body-content") buf)))))))
              (http/end)))]

    (let [server (http/server {:compression-supported true})
          port 4043
          host "localhost"]
      (-> server
        (http/on-request req-handler)
        (http/listen port host
          (partial server-listen-handler server port host))))))


(deftest http-client-request
  (letfn [(send-request [client method h]
            (-> client (http/request method "/some-uri" h) (http/end)))

          (req-handler [req]
            (let [resp (http/server-response req {:status-code 200})]
              (http/end resp)))

          (connect-request [client]
            (send-request client :CONNECT (fn [resp]
                                            (t/test-complete (assert-status-code resp)))))

          (patch-request [client]
            (send-request client :PATCH (fn [resp] (assert-status-code resp)
                                          (connect-request client))))

          (trace-request [client]
            (send-request client :TRACE (fn [resp] (assert-status-code resp)
                                          (patch-request client))))

          (head-request [client]
            (send-request client :HEAD (fn [resp] (assert-status-code resp)
                                         (trace-request client))))

          (delete-request [client]
            (send-request client :DELETE (fn [resp] (assert-status-code resp)
                                           (head-request client))))

          (put-request [client]
            (send-request client :PUT (fn [resp] (assert-status-code resp)
                                        (delete-request client))))

          (post-request [client]
            (send-request client :POST (fn [resp] (assert-status-code resp)
                                         (put-request client))))

          (server-listen-handler [orig-server port host err server]
            (is (not err))
            (is (= orig-server server))
            (let [client (http/client {:port port :host host})]
              (send-request client :OPTIONS (fn [resp] (assert-status-code resp)
                                              (post-request client)))))]

    (let [server (http/server)
          port 8111
          host "localhost"]
      (-> server
        (http/on-request req-handler)
        (http/listen port host
          (partial server-listen-handler server port host))))))

(deftest websocket-request
  (letfn [(ws-handler [ws]
            (is (= "/some/path" (.path ws)))
            (is (= "foo=bar&wibble=eek" (.query ws)))

            (is (= "127.0.0.1" (:host (websocket/remote-address ws))))
            (is (> (:port (websocket/remote-address ws)) 1000))

            (stream/on-data ws (fn [data]
                                 (stream/write ws data))))

          (server-listen-handler [orig-server port host err server]
            (is (not err))
            (is (= orig-server server))
            (-> (http/client {:port port :host host})
              (websocket/connect "/some/path?foo=bar&wibble=eek" :RFC6455
                (fn [ws]
                  (let [sent-buf! (buf/buffer)
                        rcv-buf! (buf/buffer)
                        send-count 10
                        send-size 100]
                    (stream/on-data ws (fn [data]
                                         (buf/append! rcv-buf! data)
                                         (when (= (.length rcv-buf!) (* send-count send-size))
                                           (t/test-complete
                                             (is (= sent-buf! rcv-buf!))))))
                    (dotimes [_ send-count]
                      (let [data (t/random-buffer send-size)]
                        (buf/append! sent-buf! data)
                        (websocket/write-binary-frame ws data))))))))]
    (let [server (http/server)
          port 8111
          host "localhost"]
      (-> server
        (websocket/on-websocket ws-handler)
        (http/listen port host
          (partial server-listen-handler server port host))))))
