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

(ns vertx.stream
  "Functions that operate on Vert.x ReadStreams and WriteStreams."
  (:require [vertx.buffer :as buf]
            [vertx.core :as core]
            [vertx.utils :as util])
  (:import (org.vertx.java.core.streams ExceptionSupport Pump ReadStream WriteStream)))

(defn on-data
  "Set a data handler on a ReadStream.
   As data is read, the handler will be called with the data as a Buffer.
   handler can either be a Handler or a single-arity fn.
   Returns the stream."
  [^ReadStream stream handler]
  (.dataHandler stream (core/as-handler handler)))

(defn on-drain
  "Set a drain handler on a WriteStream.
   If the write queue is full, then the handler will be called when
   the write queue has been reduced to maxSize / 2.
   handler can either be a Handler or a zero-arity fn.
   Returns the stream."
  [^WriteStream stream handler]
  (.drainHandler stream (core/as-void-handler handler)))

(defn on-end
  "Set an end handler on a ReadStream.
   Once the stream has ended, and there is no more data to be read,
   this handler will be called.
   handler can either be a Handler or a zero-arity fn.
   Returns the stream."
  [^ReadStream stream handler]
  (.endHandler stream (core/as-void-handler handler)))

(defn on-exception
  "Set an exception handler on a stream.
   handler can either be a Handler or a single-arity fn that will be
   passed the exception.
   Returns the stream."
  [^ExceptionSupport stream handler]
  (.exceptionHandler stream (core/as-handler handler util/exception->map)))

(defn write
  "Write data to the stream.
   data can anything bufferable (see vertx.buffer)."
  ([^WriteStream stream data]
     (.write stream (buf/as-buffer data)))
  ([^WriteStream stream data-str enc]
     (.write stream (buf/buffer data-str enc))))

(defn pump
  "Creates a Pump instance.

   A Pump pumps data from a ReadStream to a WriteStream and performs
   flow control where necessary to prevent the write stream buffer
   from getting overfull.

   Read bytes from a ReadStream and writes them to a WriteStream. If
   data can be read faster than it can be written this could result in
   the write queue of the WriteStream growing without bound,
   eventually causing it to exhaust all available RAM.

   To prevent this, after each write, it checks whether the write
   queue of the WriteStream is full, and if so, the ReadStream is
   paused, and a drainHandler is set on the WriteStream. When the
   WriteStream has processed half of its backlog, the
   drainHandler will be called, which results in the pump resuming
   the ReadStream.

   If start? is true (the default), the Pump will be started."
  ([read-stream write-stream]
     (pump read-stream write-stream true))
  ([read-stream write-stream start?]
     (let [p (Pump/createPump read-stream write-stream)]
       (when start?
         (.start p))
       p)))
