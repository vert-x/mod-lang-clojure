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

(ns vertx.dns
  "Provides functions for asynchronous DNS operations."
  (:refer-clojure :exclude [resolve])
  (:require [vertx.core :as core]
            [vertx.utils :as u]
            [clojure.string :as str])
  (:import (java.net InetAddress InetSocketAddress)
           (org.vertx.java.core.dns DnsClient DnsException MxRecord SrvRecord)))

(extend-type DnsException
  u/ExceptionAsMap
  (exception->map [e]
    (let [code (.code e)]
      {:type (keyword (.name code))
       :message (str code)
       :basis e})))

(defn- sock-address-from-string [address]
  (let [[server port] (str/split address #":")]
    (InetSocketAddress. ^String server (int (if port (Integer/parseInt port) 53)))))

(defn- as-inet-socket-address [address]
  (if (instance? InetSocketAddress address)
    address
    (sock-address-from-string address)))

(defn- client [servers]
  (.createDnsClient (core/get-vertx)
                    (into-array (map as-inet-socket-address
                                     (if (coll? servers) servers [servers])))))

(defn- ^DnsClient as-client [client-or-servers]
  (if (instance? DnsClient client-or-servers)
    client-or-servers
    (client client-or-servers)))

(defn lookup
  "Returns the first found A (ipv4) or AAAA (ipv6) record for name.

   client-or-servers can be one of:

   * the client object returned by any of the functions in the
     vertx.dns namespace
   * a host or host:port string specifying the server
   * a collection of host or host:port strings specifying dns
     servers

   If no port is specified, the dns default port (53) is assumed.

   type can be one of :ipv4, :ipv6, or :any (the default), which limit
   the lookup to ipv4, ipv6, or first found, respectively.

   handler can either be a two-arity fn that will be passed the
   exception-map (if any) and result of the lookup call, or a
   org.vertx.java.core.Handler that will be called with the
   AsyncResult object that wraps the exception and raw result.

   The exception-map that may be passed to the handler will be a
   map of the form:

     {:type :NXDOMAIN ;; or other error type
      :message \"NXDOMAIN: type 3, name error\"
      :basis the-DnsException-object}

   The result passed to a handler function will be a map of the form:

     {:address \"127.0.0.1\"
      :basis the-InetAddress-object}

   Returns a client object that can be passed to other dns calls."
  ([client-or-servers name handler]
     (lookup client-or-servers name :any handler))
  ([client-or-servers name type handler]
     (let [client (as-client client-or-servers)
           h (core/as-async-result-handler handler u/inet-address->map)]
       (case type
         :ipv4  (.lookup4 client name h)
         :ipv6  (.lookup6 client name h)
         :any (.lookup client name h)
         (throw (IllegalArgumentException. "type must be one of: :any, :ipv4, :ipv6"))))))

(defn resolve
  "Resolves records of type for name.

   client-or-servers can be one of:

   * the client object returned by any of the functions in the
     vertx.dns namespace
   * a host or host:port string specifying the server
   * a collection of host or host:port strings specifying dns
     servers

   If no port is specified, the dns default port (53) is assumed.

   type must be one of the following:

   :A     Resolves the A records for name. The handler fn will be
          passed a collection of maps of the form:

            {:address \"127.0.0.1\"
             :basis the-InetAddress-object}

   :AAAA  Resolves the AAAA records for name. The handler fn will be
          passed a collection of maps of the same form as :A.
   :CNAME Resolves the CNAME records for name. The handler fn will
          be passed a collection of Strings.
   :MX    Resolves the MX records for name. The handler fn will be
          passed a collection of maps of the form:

            {:priority 1
             :name \"mail.example.com\"
             :basis the-MxRecord-object}

   :NS    Resolves the NS records for name. The handler fn will be
          passed a collection of NS servers as Strings.
   :PTR   Resolves the PTR records for name. The handler fn will be
          passed the PTR String.
   :SRV   Resolves the SRV records for name. The handler fn will be
          passed a collection of maps of the form:

            {:priority 1
             :weight 0
             :port 1234
             :name \"someserver.example.com\"
             :protocol \"_tcp\"
             :service \"_http\"
             :target \"target-name\"
             :basis the-SrvRecord-object}

   :TXT   Resolves the TXT records for name. The handler fn will be
          passed a collection of TXT entries as Strings.

   handler can either be a two-arity fn that will be passed the
   exception-map (if any) and result from the resolve call, or a
   org.vertx.java.core.Handler that will be called with the
   AsyncResult object that wraps the exception and raw result.

   The exception-map that may be passed to the handler will be a
   map of the form:

     {:type :NXDOMAIN ;; or other error type
      :message \"NXDOMAIN: type 3, name error\"
      :basis the-DnsException-object}

   Returns a client object that can be passed to other dns calls."
  [client-or-servers name type handler]
  (let [client (as-client client-or-servers)
        ->handler (partial core/as-async-result-handler handler)]
    (case type
      :A     (.resolveA client name (->handler (partial map u/inet-address->map)))
      :AAAA  (.resolveAAAA client name (->handler (partial map u/inet-address->map)))
      :CNAME (.resolveCNAME client name (->handler seq))
      :MX    (.resolveMX client name
                         (->handler
                          (partial map
                                   (fn [^MxRecord mx]
                                     {:priority (.priority mx)
                                      :name (.name mx)
                                      :basis mx}))))
      :NS    (.resolveNS client name (->handler seq))
      :PTR   (.resolvePTR client name (->handler identity))
      :SRV   (.resolveSRV client name
                          (->handler
                           (partial map
                                    (fn [^SrvRecord srv]
                                      {:priority (.priority srv)
                                       :weight (.weight srv)
                                       :port (.port srv)
                                       :name (.name srv)
                                       :protocol (.protocol srv)
                                       :service (.service srv)
                                       :target (.target srv)
                                       :basis srv}))))
      :TXT   (.resolveTXT client name (->handler seq))
      (throw (IllegalArgumentException. "type must be one of: :A, :AAAA, :CNAME, :MX, :NS, :PTR, :SRV, :TXT")))))

(defn reverse-lookup
  "Performs a reverse lookup for the given address.

   client-or-servers can be one of:

   * the client object returned by any of the functions in the
     vertx.dns namespace
   * a host or host:port string specifying the server
   * a collection of host or host:port strings specifying dns
     servers

   If no port is specified, the dns default port (53) is assumed.

   handler can either be a two-arity fn that will be passed the
   exception-map (if any) and result of the lookup call, or a
   org.vertx.java.core.Handler that will be called with the
   AsyncResult object that wraps the exception and raw result.

   The exception-map that may be passed to the handler will be a
   map of the form:

     {:type :NXDOMAIN ;; or other error type
      :message \"NXDOMAIN: type 3, name error\"
      :basis the-DnsException-object}

   The result passed to a handler function will be a map of the form:

     {:host \"somehost.example.com\"
      :basis the-InetAddress-object}

   Returns a client object that can be passed to other dns calls."
  [client-or-servers address handler]
  (.reverseLookup (as-client client-or-servers)
                  address
                  (core/as-async-result-handler
                   handler
                   (fn [addr]
                     (if-let [addr-map (u/inet-address->map addr)]
                       (assoc addr-map :host (.getHostName ^InetAddress (:basis addr-map))))))))
