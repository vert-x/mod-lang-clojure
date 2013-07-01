(ns vertx.core
  (:import [org.vertx.java.core.json JsonObject]
           [org.vertx.java.core Vertx Handler]
           [org.vertx.java.platform Verticle]
           [org.vertx.java.core.impl DefaultVertx]
           [org.vertx.java.platform.impl DefaultPlatformManager DefaultContainer])
           [clojure.lang ArityException])
  (:require [vertx.utils :refer :all])

(println "vert.x core initializing")

;;(dorun (map #(println %) (.getURLs (^URLClassLoader ClassLoader/getSystemClassLoader))))

(def no-vertx-runtime-error
  (Exception. (str "there are not Vertx instance in this process.")))

(def ^:dynamic *vertx* (atom nil))

(defn get-vertx [] 
  (or @*vertx*
      (throw no-vertx-runtime-error)))

(defmacro start-vertx
  [vertx & body]
  `(do (reset! *vertx* ~vertx) ~@body))

(defn bus-send
  "Send message with ```EventBus``` and it's address"
  ([addr content] (.send (.eventBus (get-vertx)) addr content))
  ([addr content handler] (.send (.eventBus (get-vertx)) addr content handler)))

(defn bus-register
  "Register message handler with address in Eventbus"
  ([addr handler] (.registerHandler (.eventBus (get-vertx)) addr handler))
  ([addr handler rest-handler] (.registerHandler (.eventBus (get-vertx)) addr handler rest-handler)))


(defn periodic [delay handler]
  (.setPeriodic (get-vertx) delay handler))

(defn timer [delay handler]
  (.setTimer (get-vertx) delay handler))

(defn cancel-timer [id]
  (.cancelTimer (get-vertx) id))

(declare vertx)
(declare container)



(defmacro handler
  [bindings & body]
     (handle ~bindings ~@body)))
  `(reify org.vertx.java.core.Handler
     (handle [_# ~@bindings] ~@body)))

(defmacro defhandler
  [name & rest]
  `(def ~name (handler ~@rest)))

(defn deploy-module
  ([module-name conf]
     (fn [vertx# container#]
       (.deployModule container# module-name (map->json conf))))
  ([module-name conf instances]
     (fn [vertx# container#]
       (.deployModule container# module-name (map->json conf) instances)))
  ([module-name conf instances done-handler]
     (fn [vertx# container#]
       (.deployModule container# module-name (map->json conf) instances done-handler)))
  )

(defmacro deploy-module [container & body]
  `(.deployModule ~container ~@body))

(defmacro undeploy-module [container id & body]
  `(.undeployModule ~container ~id ~@body))


(defmacro deploy-verticle [container & body]
  `(.deployVerticle ~container ~@body))

(defmacro undeploy-verticle [container id & body]
  `(.undeployVerticle ~container ~id ~@body))

(defn deploy-verticle* [container main {:keys [config instances handler]}]
  (.deployVerticle container main
                   (map->json config)
                   (or instances 1)
                   handler))

(defmacro deploy-worker-verticle [container & body]
  `(.deployWorkerVerticle ~container  ~@body))
