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
            [vertx.http.websocket :as ws]
            [vertx.buffer :as buf]
            [vertx.stream :as stream]
            [vertx.testtools :as t]
            [clojure.java.io :as io]))


(defn resource-path [name]
  (.getAbsolutePath (io/file (io/resource name))))

(defn assert-stauts-code [resp]
  (t/assert= (int 200) (http/status-code resp)))

(defn test-base-request []
  (letfn [(req-handler [req]
            (let [header (http/headers req)
                  addr (http/remote-address req)]
              (t/assert= :GET (http/request-method req))
              (t/assert= "/get/now?k=v" (http/uri req))
              (t/assert= "/get/now" (http/path req))
              (t/assert= "k=v" (http/query req))
              (t/assert= {:k "v"} (http/params req))
              (t/assert= "dummy" (:dummy header))
              (t/assert= ["v1" "v2"] (:dummy-v header))
              (t/assert  (.startsWith (:host addr) "localhost"))
              (t/assert  (> (:port addr) -1))
              (t/assert-not-nil (-> req http/absolute-uri)))

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
                            (assert-stauts-code resp)
                            (t/assert= "status-msg" (http/status-msg resp))
                            (t/assert= "add-header" (:add-header (http/headers resp)))
                            (http/on-body
                             resp (fn [buf]
                                    (t/test-complete
                                     (t/assert= (buf/buffer "body-content") buf)))))))

          (server-listen-handler [orig-server port host err server]
            (t/assert-nil err)
            (t/assert= orig-server server)
            (client-request (http/client {:port port :host host}))
            )]

    (let [server (http/server) port 8888 host "localhost"]
      (-> server
          (http/on-request req-handler)
          (http/listen port host (partial server-listen-handler server port host))
          ))))



(defn test-form-request []
  (letfn [(req-handler [req]
            (t/assert (.startsWith (http/uri req) "/form"))
            (let [resp (http/server-response req {:chunked true})]
              (stream/on-end req (fn []
                                   (let [forms (http/form-attributes req)]
                                     (prn forms)
                                     (t/assert= "junit-testUserAlias" (:origin forms))
                                     (t/assert= "admin@foo.bar" (:login forms))
                                     (t/assert= "admin" (:pass-word forms))
                                     (http/end resp))))))

          (server-listen-handler [orig-server port host err server]
            (t/assert-nil err)
            (t/assert= orig-server server)
            (let [body "origin=junit-testUserAlias&login=admin%40foo.bar&pass+word=admin"]
              (-> (http/client {:port port :host host})
                  (http/post "/form"
                             (fn [resp]
                               (assert-stauts-code resp)
                               (http/on-body resp
                                             (fn [body]
                                                  (t/test-complete
                                                   (t/assert= (int 0) (.length body)))))))
                  
                  (http/add-header :content-length (str (.length body)))
                  (http/add-header :content-type (str "application/x-www-form-urlencoded"))
                  (http/end body))))
          ]

    (let [server (http/server) port 8888 host "localhost"]
      (-> server
          (http/on-request req-handler)
          (http/listen port host (partial server-listen-handler server port host))
          ))))



(defn test-upload-request []
  (letfn [(req-handler [req]
            (t/assert (.startsWith (http/uri req) "/form"))
            (let [resp (http/server-response req {:chunked true})]
              (http/on-upload req
                              (fn [file-info]
                                (t/assert= "file" (:name file-info))
                                (t/assert= "tmp-0.txt" (:filename file-info))
                                (t/assert= "image/gif" (:content-type file-info))
                                (stream/on-data
                                 (:stream file-info)
                                 (fn [data]
                                   (t/assert= (buf/buffer "Vert.x Rocks!") data)))))
              
              (stream/on-end req (fn []
                                   (let [forms (http/form-attributes req)]
                                     (t/assert= (int 0) (count forms))
                                     (http/end resp))))))

          (server-listen-handler [orig-server port host err server]
            (t/assert-nil err)
            (t/assert= orig-server server)
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
                               (assert-stauts-code resp)
                               (http/on-body resp
                                             (fn [body]
                                               (t/test-complete
                                                (t/assert= (int 0) (.length body)))))))
                  
                  (http/add-header :content-length (str (.length body)))
                  (http/add-header :content-type (str "multipart/form-data; boundary="boundary))
                  (http/end body))))]

    (let [server (http/server) port 8888 host "localhost"]
      (-> server
          (http/on-request req-handler)
          (http/listen port host (partial server-listen-handler server port host))
          ))))



(defn test-ssl-request []
  (letfn [(req-handler [req]
            (t/assert= :GET (http/request-method req))
            (t/assert= "/get/ssl/" (http/uri req))
            (let [resp (http/server-response req)]
              (http/end resp "body-content")))

          (server-listen-handler [orig-server port host err server]
            (t/assert-nil err)
            (t/assert= orig-server server)
            (-> (http/client {:host "localhost"
                              :port 4043
                              :SSL true
                              :trust-all true
                              :key-store-path (resource-path "keystores/client-keystore.jks")
                              :key-store-password "wibble"
                              :trust-store-path (resource-path "keystores/client-truststore.jks")
                              :trust-store-password "wibble"})
                (http/get "/get/ssl/"
                          (fn [resp]
                            (assert-stauts-code resp)
                            (http/on-body resp
                                          (fn [buf]
                                            (t/test-complete
                                             (t/assert= (buf/buffer "body-content") buf))))))
                (http/end)))]

    (let [server (http/server {:SSL true
                               :key-store-path (resource-path "keystores/server-keystore.jks")
                               :key-store-password "wibble"
                               :trust-store-path (resource-path "keystores/server-truststore.jks")
                               :trust-store-password "wibble"
                               :client-auth-required true})
          port 4043
          host "localhost"]
      (-> server
          (http/on-request req-handler)
          (http/listen port host
                       (partial server-listen-handler server port host))))))



(defn test-http-client-request []
  (letfn [(send-request [client method h]
            (-> client (http/request method "/some-uri" h) (http/end)))

          (req-handler [req]
            (let [resp (http/server-response req {:status-code 200})]
              (http/end resp)))

          (connect-request [client]
            (send-request client :CONNECT (fn [resp]
                                            (t/test-complete (assert-stauts-code resp)))))

          (patch-request [client]
            (send-request client :PATCH (fn [resp] (assert-stauts-code resp)
                                          (connect-request client))))

          (trace-request [client]
            (send-request client :TRACE (fn [resp] (assert-stauts-code resp)
                                          (patch-request client))))

          (head-request [client]
            (send-request client :HEAD (fn [resp] (assert-stauts-code resp)
                                         (trace-request client))))

          (delete-request [client]
            (send-request client :DELETE (fn [resp] (assert-stauts-code resp)
                                           (head-request client))))

          (put-request [client]
            (send-request client :PUT (fn [resp] (assert-stauts-code resp)
                                        (delete-request client))))

          (post-request [client]
            (send-request client :POST (fn [resp] (assert-stauts-code resp)
                                         (put-request client))))

          (server-listen-handler [orig-server port host err server]
            (t/assert-nil err)
            (t/assert= orig-server server)
            (let [client (http/client {:port port :host host})]
              (send-request client :OPTIONS (fn [resp] (assert-stauts-code resp)
                                              (post-request client)))))]

    (let [server (http/server)
          port 8080
          host "localhost"]
      (-> server
          (http/on-request req-handler)
          (http/listen port host
                       (partial server-listen-handler server port host))))))

(defn test-websocket-request []
  (letfn [(ws-handler [ws]
            (t/assert= "/some/path" (http/path ws))
            (t/assert= "foo=bar&wibble=eek" (http/query ws))
            (stream/on-data ws (fn [data]
                                 (stream/write ws data))))

          (server-listen-handler [orig-server port host err server]
            (t/assert-nil err)
            (t/assert= orig-server server)
            (-> (http/client {:port port :host host})
                (ws/connect "/some/path?foo=bar&wibble=eek" :RFC6455
                            (fn [ws]
                              (let [sent-buf! (buf/buffer)
                                    rcv-buf! (buf/buffer)
                                    send-count 10
                                    send-size 100]
                                (stream/on-data ws (fn [data]
                                                     (buf/append! rcv-buf! data)
                                                     (when (= (.length rcv-buf!) (* send-count send-size))
                                                       (t/test-complete
                                                        (t/assert= sent-buf! rcv-buf!))
                                                       )))
                                (dotimes [_ send-count]
                                  (let [data (t/random-buffer send-size)]
                                    (buf/append! sent-buf! data)
                                    (ws/write-binary-frame ws data))))))))]
    (let [server (http/server)
          port 8080
          host "localhost"]
      (-> server
          (ws/on-websocket ws-handler)
          (http/listen port host
                       (partial server-listen-handler server port host))))))

(t/start-tests)
