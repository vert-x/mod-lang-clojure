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

(ns vertx.routematcher
  "Functions for operating on Vertx.x RouteMatcher."
  (:require [clojure.string :as string]
            [vertx.core :as core])
  (:import [org.vertx.java.core.http RouteMatcher]))

(defn matcher [] (RouteMatcher.))

(defn match
  "Specify a handler that will be called for a matching HTTP method
   including
   :OPTIONS :GET :HEAD :POST :PUT :DELETE :TRACE :CONNECT :PATCH :ALL
   ALL is special, which will be called for all Http method
  "
  ([method pattern handler]
     (match (matcher) method pattern handler))

  ([matcher method pattern handler]
     (clojure.lang.Reflector/invokeInstanceMethod
      matcher
      (string/lower-case (name method))
      (into-array Object [pattern (core/as-handler handler)])) matcher))

(defn match-regx
  "Specify a handler that will be called for a matching HTTP method
   including
   :OPTIONS :GET :HEAD :POST :PUT :DELETE :TRACE :CONNECT :PATCH :ALL
   ALL is special, which will be called for all Http method
  "
  ([method pattern handler]
     (match-regx (matcher) method pattern handler))
  ([matcher method pattern handler]
     (clojure.lang.Reflector/invokeInstanceMethod
      matcher
      (str (string/lower-case (name method)) "WithRegEx")
      (into-array Object [pattern (core/as-handler handler)])) matcher))

(defn no-match 
  "Specify a handler that will be called when no other handlers match.
   If this handler is not specified default behaviour is to return a 404"
  ([handler]
     (no-match (matcher handler)))
  ([matcher handler]
     (.noMatch matcher (core/as-handler handler)) matcher))


