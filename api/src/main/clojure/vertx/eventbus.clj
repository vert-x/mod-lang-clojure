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

(ns vertx.eventbus
  "Functions for operating on the Vert.x EventBus and its Messages.
   The event bus implements publish / subscribe, point to point
   messaging and request-response messaging.

   Messages sent over the event bus are represented by instances of
   the org.vertx.java.core.eventbus.Message class.

   For publish / subscribe, messages can be published to an address
   using publish function. An address is a simple String instance.

   Handlers are registered against an address. There can be multiple
   handlers registered against each address, and a particular handler
   can be registered against multiple addresses. The event bus will
   route a sent message to all handlers which are registered against
   that address.

   For point to point messaging, messages can be sent to an address
   using the send function. The messages will be delivered to a single
   handler, if one is registered on that address. If more than one
   handler is registered on the same address, Vert.x will choose one
   and deliver the message to that. Vert.x will aim to fairly
   distribute messages in a round-robin way, but does not guarantee
   strict round-robin under all circumstances.

   All messages sent over the bus are transient. On event of failure
   of all or part of the event bus messages may be lost. Applications
   should be coded to cope with lost messages, e.g. by resending them,
   and making application services idempotent.

   The order of messages received by any specific handler from a
   specific sender should match the order of messages sent from that
   sender.

   When sending a message, a reply handler can be provided. If so, it
   will be called when the reply from the receiver has been
   received. Reply messages can also be replied to, etc, ad infinitum.

   Different event bus instances can be clustered together over a
   network, to give a single logical event bus.

   If handlers are registered from an event loop, they will be
   executed using that same event loop. If they are registered from
   outside an event loop (i.e. when using Vert.x embedded) then Vert.x
   will assign an event loop to the handler and use it to deliver
   messages to that handler."
  (:refer-clojure :exclude [send])
  (:require [vertx.core :as core]
            [vertx.utils :refer :all])
  (:import [org.vertx.java.core AsyncResult Handler]
           org.vertx.java.core.buffer.Buffer
           [org.vertx.java.core.json JsonArray JsonObject]
           [org.vertx.java.core.eventbus EventBus Message ReplyException]))

(extend-type ReplyException
  ExceptionAsMap
  (exception->map [e]
    {:type (keyword (.name (.failureType e)))
     :code (.failureCode e)
     :message (.getMessage e)
     :basis e}))

(def ^{:dynamic true
       :doc "The currently active EventBus instance.
             If not bound, the EventBus from vertx.core/*vertx* will be used.
             You should only need to bind this for advanced usage."}
  *eventbus* nil)

(defn ^EventBus get-eventbus
  "Returns the currently active EventBus instance."
  []
  (or *eventbus* (.eventBus (core/get-vertx))))

(def ^:dynamic *current-message*
  "Bound to the current Message instance when a handler fn is called."
  nil)

(def ^:dynamic *current-address*
  "Bound to the current receiving address when a handler fn is called."
  nil)

(defn ^:private ^Handler message-handler
  "Wraps a fn in a Handler, binding *current-message*"
  ([handler]
     (message-handler handler nil))
  ([handler with-timeout?]
     (if (or (nil? handler) (core/handler? handler))
       handler
       (core/as-handler
        (fn [val]
          (let [^Message msg (if with-timeout? (.result ^AsyncResult val) val)
                decoded (if msg (decode (.body msg)))]
            (binding [*current-message* msg
                      *current-address* (if msg (.address msg))]
              (if with-timeout?
                (handler (exception->map (.cause ^AsyncResult val)) decoded)
                (handler decoded)))))))))

(defn default-reply-timeout
  "Retrieves the current default reply timeout.
   See set-default-reply-timeout!"
  []
  (.getDefaultReplyTimeout (get-eventbus)))

(defn set-default-reply-timeout!
  "Sets a default timeout, in ms, for replies.
   If a messages is sent specifying a reply handler but without
   specifying a timeout, then the reply handler is timed out, i.e. it
   is automatically unregistered if a message hasn't been received
   before timeout. The default value for default send timeout is -1,
   which means 'never timeout'."
  [timeout]
  (.setDefaultReplyTimeout (get-eventbus) timeout))

(defn send
  "Sends a message to the given address.
   A send will only be received by one handler, regardless of the
   number registered.

   A reply-handler can be specified to receive any replies to the
   message. If no timeout is provided, the reply-handler should be a
   single-arity function that will receive the reply. If a timeout is
   specified, the reply-handler should be a two-arity function that
   will be passed an exception-map as the first parameter, and the
   reply as the second. The exception-map will be nil unless the
   timeout expired before a reply was received or no handlers were
   listening to the address.

   reply-handler can also be a org.vertx.java.core.Handler that will
   that will called with the raw object that represents the reply.

   If reply-handler is a fn, *current-message* will be bound to the
   actual Message object when the fn is called."
  ([addr content]
     (send addr content nil))
  ([^String addr content reply-handler]
     (let [eb (get-eventbus)
           h (message-handler reply-handler)
           encoded (encode content)]
       (condp instance? encoded
         Boolean        (.send eb addr ^Boolean encoded h)
         byte-arr-class (.send eb addr ^bytes encoded h)
         Buffer         (.send eb addr ^Buffer encoded h)
         Byte           (.send eb addr ^Byte encoded h)
         Character      (.send eb addr ^Character encoded h)
         Double         (.send eb addr ^Double encoded h)
         Float          (.send eb addr ^Double encoded h)
         JsonArray      (.send eb addr ^JsonArray encoded h)
         JsonObject     (.send eb addr ^JsonObject encoded h)
         Short          (.send eb addr ^Double encoded h)
         String         (.send eb addr ^String encoded h)
         (.send eb addr ^Object encoded h))))
  ([^String addr content ^Long timeout reply-handler]
     (let [eb (get-eventbus)
           h (message-handler reply-handler :with-timeout)
           encoded (encode content)]
       (condp instance? encoded
         Boolean        (.sendWithTimeout eb addr ^Boolean encoded timeout h)
         byte-arr-class (.sendWithTimeout eb addr ^bytes encoded timeout h)
         Buffer         (.sendWithTimeout eb addr ^Buffer encoded timeout h)
         Byte           (.sendWithTimeout eb addr ^Byte encoded timeout h)
         Character      (.sendWithTimeout eb addr ^Character encoded timeout h)
         Double         (.sendWithTimeout eb addr ^Double encoded timeout h)
         Float          (.sendWithTimeout eb addr ^Double encoded timeout h)
         JsonArray      (.sendWithTimeout eb addr ^JsonArray encoded timeout h)
         JsonObject     (.sendWithTimeout eb addr ^JsonObject encoded timeout h)
         Short          (.sendWithTimeout eb addr ^Double encoded timeout h)
         String         (.sendWithTimeout eb addr ^String encoded timeout h)
         (.sendWithTimeout eb addr ^Object encoded timeout h)))))

(defn publish
  "Publishes a message to the given address.
   A publish will be received by all handlers registered on the
  address."
  [^String addr content]
  (let [eb (get-eventbus)
        encoded (encode content)]
       (condp instance? encoded
         Boolean        (.publish eb addr ^Boolean encoded)
         byte-arr-class (.publish eb addr ^bytes encoded)
         Buffer         (.publish eb addr ^Buffer encoded)
         Byte           (.publish eb addr ^Byte encoded)
         Character      (.publish eb addr ^Character encoded)
         Double         (.publish eb addr ^Double encoded)
         Float          (.publish eb addr ^Double encoded)
         JsonArray      (.publish eb addr ^JsonArray encoded)
         JsonObject     (.publish eb addr ^JsonObject encoded)
         Short          (.publish eb addr ^Double encoded)
         String         (.publish eb addr ^String encoded)
         (.publish eb addr ^Object encoded))))

(defn reply*
  "Replies to the given message.
   A reply-handler can be specified to receive any replies to the
   message. If no timeout is provided, the reply-handler should be a
   single-arity function that will receive the reply. If a timeout is
   specified, the reply-handler should be a two-arity function that
   will be passed an exception-map as the first parameter, and the
   reply as the second. The exception-map will be nil unless the
   timeout expired before a reply was received or no handlers were
   listening to the address.

   reply-handler can also be a org.vertx.java.core.Handler that will
   that will called with the raw object that represents the reply.

   If reply-handler is a fn, *current-message* will be bound to the
   actual Message object when the fn is called."
  ([^Message m]
     (.reply m))
  ([^Message m content]
     (.reply m (encode content)))
  ([^Message m content reply-handler]
     (.reply m (encode content) (message-handler reply-handler)))
  ([^Message m content ^Long timeout reply-handler]
     (let [h (message-handler reply-handler :with-timeout)
           encoded (encode content)]
       (condp instance? encoded
         Boolean        (.replyWithTimeout m ^Boolean encoded timeout h)
         byte-arr-class (.replyWithTimeout m ^bytes encoded timeout h)
         Buffer         (.replyWithTimeout m ^Buffer encoded timeout h)
         Byte           (.replyWithTimeout m ^Byte encoded timeout h)
         Character      (.replyWithTimeout m ^Character encoded timeout h)
         Double         (.replyWithTimeout m ^Double encoded timeout h)
         Float          (.replyWithTimeout m ^Double encoded timeout h)
         JsonArray      (.replyWithTimeout m ^JsonArray encoded timeout h)
         JsonObject     (.replyWithTimeout m ^JsonObject encoded timeout h)
         Short          (.replyWithTimeout m ^Double encoded timeout h)
         String         (.replyWithTimeout m ^String encoded timeout h)
         (.replyWithTimeout m ^Object encoded timeout h)))))

(defn reply
  "Replies to *current-message*.
   See reply*."
  ([content]
     (reply* *current-message* content))
  ([content reply-handler]
     (reply* *current-message* content reply-handler))
  ([content timeout reply-handler]
     (reply* *current-message* content timeout reply-handler)))

(defn fail*
  "Sends a failure reply to the sender of the given message.
   failure-code and message are an integer code and string specific to
  your application."
  [^Message m failure-code message]
  (.fail m failure-code message))

(defn fail
  "Sends a failure reply to the sender of *current-message*.
   See reply-failure*."
  [failure-code message]
  (fail* *current-message* failure-code message))

(defonce ^:private registered-handlers (atom {}))

(defn on-message
  "Registers a handler to receive messages on an address.
   handler can either be a single-arity fn that will be passed the
   body of the message, or a org.vertx.java.core.Handler that will be
   called with the Message object itself. If handler is a fn,
   *current-message* and *current-address* will be bound to the actual
   Message object and address that received the message, respectively,
   when the fn is called.

   result-handler can either be a single-arity fn that will be passed
   the exception-map (if any) from the result of the deploy call, or a
   org.vertx.java.core.Handler that will be called with the
   AsyncResult object that wraps the exception. The result-handler
   will be called when the register has been propagated to all nodes.

   If local-only? is true, the handler won't be propagated to all
   nodes, and only registered on the current node. Default is false.

   Returns an id for the handler that can be passed to
   unregister-handler."
  ([addr handler]
     (on-message addr handler nil false))
  ([addr handler result-handler]
     (on-message addr handler result-handler false))
  ([addr handler result-handler local-only?]
     (let [h (message-handler handler)
           id (uuid)]
       (if local-only?
         (.registerLocalHandler (get-eventbus) addr h)
         (.registerHandler (get-eventbus) addr h
                           (core/as-async-result-handler result-handler false)))
       (swap! registered-handlers assoc id [addr h])
       id)))

(defn unregister-handler
  "Removes the message handler with the given id."
  ([id]
     (unregister-handler id nil))
  ([id result-handler]
     (if-let [[addr h] (@registered-handlers id)]
       (do
         (.unregisterHandler
          (get-eventbus) addr h
          (core/as-async-result-handler result-handler false))
         (swap! registered-handlers dissoc id))
       (throw (IllegalArgumentException.
               (format "No handler with id %s found." id))))
     nil))
