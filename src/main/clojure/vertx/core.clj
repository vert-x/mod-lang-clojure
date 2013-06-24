(ns vertx.core
  (:import [org.vertx.java.core.json JsonObject]
           [clojure.lang ArityException])
  (:use vertx.utils)
  )
(println "vert.x core initializing")

;; setup for vertx

;;(org.vertx.java.platform.impl.ClojureVerticle/reloadCore)
;;(map #(println %) (.getURLs (^URLClassLoader ClassLoader/getSystemClassLoader)))




(defmacro bus-send
  "accept address and handler which is option in body"
  [addr content & body]
  `(fn [vertx# _#]
     (.send (.eventBus vertx#) addr content ~@body)
     )
  )

(defmacro handle
  [bindings & body]
  `(reify org.vertx.java.core.Handler
     (handle ~bindings ~@body)))

(defmacro defhandle
  [name & rest]
  `(def ~name (handle ~@rest)))

;; undeployModule
;; undeployWorkerVerticle
;; undeployVerticle

(defmacro defverticle
  "Define a vertx runtime, in which we wrapped the instance of vertx and
container. user could only pass the marco as arguments which include all the action
of verticle"
  [body & interal]
  `(fn [vertx container]
     (~body vertx container))
  )

(defn deploy-module
  ([module-name conf]
     (fn [vertx# container#]
       (.deployModule container# module-name (json conf))))
  ([module-name conf instances]
     (fn [vertx# container#]
       (.deployModule container# module-name (json conf) instances)))
  ([module-name conf instances done-handler]
     (fn [vertx# container#]
       (.deployModule container# module-name (json conf) instances done-handler)))
  )


(defmacro deployVerticle [container & args]
  `(do
     (.deployVerticle ~container  ~@args)
     container))

(defmacro deployWorkerVerticle [container & args]
  `(do
     (.deployWorkerVerticle ~container  ~@args)
     container))
