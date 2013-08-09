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
  "Functions for operating on Vertx.x RouteMatcher.
   RouteMatchers allows you to do route requests based on the HTTP
   verb and the request URI, in a manner similar Sinatra or Express.

   RouteMatchers also let you extract parameters from the request URI
   either a via simple pattern or using regular expressions for more
   complex matches. Any parameters extracted will be added to the
   requests parameters which will be available to you in your request
   handler."
  (:refer-clojure :exclude [get])
  (:require [clojure.string :as string]
            [vertx.core :as core])
  (:import [org.vertx.java.core.http RouteMatcher]))

(defn matcher
  "Create a RouteMatcher instance."
  [] (RouteMatcher.))

(defmacro ^:private def-match-fn [name]
  (let [doc (format "Specify a handler that will be called for a matching HTTP %s.

   If no matcher is provided, a new one will be created. pattern can
   either be a simple pattern or regular expression. handler can
   either be a single-arity fn or a Handler instance that will be
   called with the HttpServerRequest object.

   To use a simple pattern to extract parameters simply prefix the
   parameter name in the pattern with a ':' (colon) For more complex
   matches regular expressions can be used in the pattern. When
   regular expressions are used, the extracted parameters do not
   have a name, so they are put into the HTTP request with names of
   param0, param1, param2 etc. Multiple matches can be specified
   for each HTTP %s. In the case there are more than one matching
   patterns for a particular request, the first matching one will be
   used.  Returns the matcher."
                    (string/upper-case name)
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
   handler can either be a single-arity fn or a Handler instance that
   will be called with the HttpServerRequest object If this handler is
   not specified. Default behaviour is to return a 404."
  ([handler]
     (no-match (matcher handler)))
  ([matcher handler]
     (.noMatch matcher (core/as-handler handler)) matcher))
