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

(ns vertx.http.route
  "Functions for operating on Vertx.x RouteMatcher."
  (:refer-clojure :exclude [get])
  (:require [clojure.string :as string]
            [vertx.core :as core])
  (:import [org.vertx.java.core.http RouteMatcher]))

(defn matcher
  "TODO: docs"
  [] (RouteMatcher.))

(defmacro ^:private def-match-fn [name]
  (let [doc (format "Specify a handler that will be called for a matching HTTP %s"
                    (string/upper-case name))
        method (symbol (str "." name))
        re-method (symbol (str "." name "WithRegEx"))]
    `(defn ~name ~doc
       ([~'pattern ~'handler]
          (~name (matcher) ~'pattern ~'handler))
       ([~'matcher ~'pattern ~'handler]
          (if (instance? java.util.regex.Pattern ~'pattern)
            (~re-method ~'matcher (str ~'pattern) (core/as-handler ~'handler))
            (~method ~'matcher ~'pattern (core/as-handler ~'handler)))))))

(def-match-fn get)
(def-match-fn put)
(def-match-fn post)
(def-match-fn delete)
(def-match-fn head)
(def-match-fn options)
(def-match-fn connect)
(def-match-fn trace)
(def-match-fn patch)
(def-match-fn all)

(defn no-match
  "Specify a handler that will be called when no other handlers match.
   If this handler is not specified default behaviour is to return a 404"
  ([handler]
     (no-match (matcher handler)))
  ([matcher handler]
     (.noMatch matcher (core/as-handler handler)) matcher))
