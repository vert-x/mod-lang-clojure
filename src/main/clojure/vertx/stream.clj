(ns vertx.stream
  "Functions that operate on Vert.x ReadStreams and WriteStreams."
  (:require [vertx.core :as core])
  (:import (org.vertx.java.core.streams Pump)))

(defn on-data
  "Set a data handler on a ReadStream.
   As data is read, the handler will be called with the data as a Buffer.
   handler can either be a Handler or a single-arity fn.
   Returns the stream."
  [stream handler]
  (.dataHandler stream (core/as-handler handler)))

(defn on-drain
  "Set a drain handler on a WriteStream.
   If the write queue is full, then the handler will be called when
   the write queue has been reduced to maxSize / 2.
   handler can either be a Handler or a zero-arity fn.
   Returns the stream."
  [stream handler]
  (.drainHandler stream (core/as-void-handler handler)))

(defn on-end
  "Set an end handler on a ReadStream.
   Once the stream has ended, and there is no more data to be read,
   this handler will be called.
   handler can either be a Handler or a zero-arity fn.
   Returns the stream."
  [stream handler]
  (.endHandler stream (core/as-void-handler handler)))

(defn on-exception
  "Set an exception handler on a stream.
   handler can either be a Handler or a single-arity fn that will be
   passed the exception.
   Returns the stream."
  [stream handler]
  (.exceptionHandler stream (core/as-handler handler)))

(defn pause
  "Pause the ReadStream.
   While the stream is paused, no data will be sent to the on-data handler.
   Returns the stream."
  [stream]
  (.pause stream))

(defn resume
  "Resumes reading on the ReadStream.
   Returns the stream."
  [stream]
  (.resume stream))

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

   If start? is true, the Pump will be started."
  ([read-stream write-stream]
     (Pump/createPump read-stream write-stream))
  ([read-stream write-stream start?]
     (let [p (pump read-stream write-stream)]
       (if start?
         (.start p)
         p))))


;; TODO: pump controls?
;; TODO: readstream controls?
;; TODO: writestream controls and options?
