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

(ns vertx.embed
  "Functions for running Vert.x embedded."
  (:require [vertx.core :as core])
  (:import [org.vertx.java.core VertxFactory]))

(defn set-vertx!
  "Binds the root value of vertx.core/*vertx*."
  [vertx]
  (.bindRoot #'core/*vertx* vertx))

(defn vertx
  "Creates a Vertx instance.
   If host provided, the instance will be clusted, listening for
   connections on host:port. port defaults to 25500."
  ([]
     (VertxFactory/newVertx))
  ([host]
     (VertxFactory/newVertx host))
  ([host port]
     (VertxFactory/newVertx port host)))

(defmacro with-vertx
  "Executes body with vertx.core/*vertx* bound to vertx."
  [vertx & body]
  `(binding [core/*vertx* ~vertx]
     ~@body))




(defn platform
  ([]
     (.createPlatformManager PlatformLocator/factory))
  ([port host]
     (.createPlatformManager PlatformLocator/factory port host)))

;;TODO: should we use Vertx instance which come from PM?
;; if it is, should we make PM as a dynamic?
(defn get-vertx
  [platform]
  (.vertx platform))

(defn container 
  "Wrap a Container with PM"
  [platform]  
  (let [cp (make-array java.net.URL 1)
        tmp (aset cp 0 (io/as-url (java.io.File. ".")))]
    (proxy [Container] []
      (deployWorkerVerticle [main conf instances thread? h]
        (.deployWorkerVerticle platform thread? main conf cp instances nil h))

      (deployModule [main conf instances h]
        (.deployModule platform main conf instances h))

      (deployVerticle [main conf instances h]
        (.deployVerticle platform main conf cp instances nil h))

      (undeployVerticle [id h]
        (.undeployVerticle platform id h))

      (undeployModule [id h]
        (.undeployModule platform id h))

      (config []
        (.config platform))

      (logger []
        (.logger platform))

      (exit []
        (.exit platform))

      (env []
        (System/getenv)))))

