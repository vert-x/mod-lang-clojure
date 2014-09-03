;; Copyright 2011-2012 the original author or authors.
;;
;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at
;;
;;     http://www.apache.org/licenses/LICENSE-2.0
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.

(ns vertx.client.eventbus
  "A ClojureScript wrapper around vertxbus.js."
  (:require vertx.eventbusjs))

(defn eventbus
  "Creates a new eventbus with the given url and options."
  ([url]
     (js/vertx.EventBus. url))
  ([url options]
     (js/vertx.EventBus. url (clj->js options))))

(defn- ready-state [eb]
  (.readyState eb))

(defn connecting?
  "Returns true if the given eventbus is in the CONNECTING state."
  [eb]
  (= js/vertx.EventBus.CONNECTING (ready-state eb)))

(defn open?
  "Returns true if the given eventbus is in the OPEN state."
  [eb]
  (= js/vertx.EventBus.OPEN (ready-state eb)))

(defn closing?
  "Returns true if the given eventbus is in the CLOSING state."
  [eb]
  (= js/vertx.EventBus.CLOSING (ready-state eb)))

(defn closed?
  "Returns true if the given eventbus is in the CLOSED state."
  [eb]
  (= js/vertx.EventBus.CLOSED (ready-state eb)))

(defn- extend-callback [old-f new-f]
  (fn []
    (and old-f (old-f))
    (new-f)))

(defn on-open
  "Registers a fn to be called when the eventbus is opened.
   The function should take one argument, and will be passed the
   eventbus. If the eventbus is currently open, the fn is called
   immediately. If the eventbus is closing or is closed, and error is
   thrown. Can be called multiple times to register multiple fns."
   [eb f]
  (if (or (closing? eb)
          (closed? eb))
    (throw (js/Error. "EventBus is closing or has closed.")))
  (let [with-eb #(f eb)]
    (if (open? eb)
      (with-eb)
      (set! (.-onopen eb) (extend-callback (.-onopen eb) with-eb))))
  eb)

(defn on-close
  "Registers a fn to be called when the eventbus is closed.
   If the eventbus is currently closed, the fn is called immediately.
   Can be called multiple times to register multiple fns."
  [eb f]
  (if (closed? eb)
    (f)
    (set! (.-onclose eb) (extend-callback (.-onclose eb) f)))
  eb)

(defn- wrap-handler [f]
  #(f (js->clj % :keywordize-keys true)))

(defn login
  "Sends a login message to the authmanager."
  [eb username password reply-handler]
  (.login eb username password (wrap-handler reply-handler)))

(defn send
  "Sends a JSON message to the given address.
   clj->js is called on content to convert to JSON. If a reply-handler
   is provided, it will be called with any reply message (after going
   through js->clj)."
  ([eb addr content]
     (send eb addr content nil))
  ([eb addr content reply-handler]
     (.send eb addr (clj->js content) (wrap-handler reply-handler))))

(defn publish
  "Publishes a JSON message to the given address.
   clj->js is called on content to convert to JSON. If a reply-handler
   is provided, it will be called with any reply message (after going
   through js->clj)."
  ([eb addr content]
     (publish eb addr content nil))
  ([eb addr content reply-handler]
     (.publish eb addr (clj->js content) (wrap-handler reply-handler))))

(let [handlers (atom {})]
  (defn on-message
    "Registers the given fn as a JSON message handler for address on eb.
     js->clj is called on each message before it is passed to your
     handler fn. Returns an id that can be used to unregister the
     handler."
    [eb address handler]
    (let [id (gensym "handler")
          wrapped (wrap-handler handler)]
      (swap! handlers assoc id {:eb eb
                                :address address
                                :handler wrapped})
      (.registerHandler eb address wrapped)
      id))

  (defn unregister-handler
    "Removes the EventBus message handler with the given id.
     See on-message."
    [id]
    (let [{:keys [eb address handler]} (get @handlers id)]
      (if eb
        (.unregisterHandler eb address handler)
        (throw (js/Error. "Handler not registered")))
      (swap! handlers dissoc id))))

(defn close
  "Closes the given EventBus."
  [eb]
  (.close eb))
