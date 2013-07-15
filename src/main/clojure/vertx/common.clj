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

(ns ^:no-doc vertx.common
    (:require [vertx.core :as c]
              [vertx.buffer :as buf]))

;; TODO: these should probably be a protocol
(defn ^:internal ^:no-doc internal-close
  "A common close implementation. Should be wrapped by other namespaces."
  [obj handler]
  (.close obj (c/as-async-result-handler handler false)))

(defn ^:internal ^:no-doc internal-write
  "A common write implementation. Should be wrapped by other namespaces."
  ([obj content]
     (.write obj (buf/as-buffer content)))
  ([obj content-str enc]
     (.write obj content-str enc)))
