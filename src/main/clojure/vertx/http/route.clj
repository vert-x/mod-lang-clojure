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
  "This class allows you to do route requests based on the HTTP verb and the request URI, in a manner similar
  to http://www.sinatrarb.com/ http://expressjs.com
  RouteMatcher also lets you extract parameters from the request URI either a simple pattern or using
  regular expressions for more complex matches. Any parameters extracted will be added to the requests parameters
  which will be available to you in your request handler.
  It's particularly useful when writing REST-ful web applications.
  To use a simple pattern to extract parameters simply prefix the parameter name in the pattern with a ':' (colon).
  Different handlers can be specified for each of the HTTP verbs, GET, POST, PUT, DELETE etc.
  For more complex matches regular expressions can be used in the pattern. When regular expressions are used, the extracted
  parameters do not have a name, so they are put into the HTTP request with names of param0, param1, param2 etc.
  Multiple matches can be specified for each HTTP verb. In the case there are more than one matching patterns for
  a particular request, the first matching one will be used.
  Instances of this class are not thread-safe
  @author Tim Fox"
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
