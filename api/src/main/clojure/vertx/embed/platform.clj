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

(ns vertx.embed.platform
  "Functions for running Vert.x embedded with a platform manager."
  (:require [clojure.string :as str]
            [vertx.core :as core]
            [vertx.utils :as util])
  (:import java.net.URL
           [org.vertx.java.platform PlatformLocator PlatformManager]))

(defn platform-manager
  "Creates a platform manager.
   cluster-port and cluster-host specify the port and host to use for
   clustering, respectively. If not provided, clustering is disabled.
   quorum-size specify the minimum number of nodes in the cluster
   before deployments in this platform manager will be activated. If
   not provided, ha is disabled. ha-group specifies the group name in
   which this platform manager participates. If nil, \"__DEFAULT__\"
   is used."
  ([]
     (.createPlatformManager PlatformLocator/factory))
  ([cluster-port cluster-host]
     (.createPlatformManager PlatformLocator/factory cluster-port cluster-host))
  ([cluster-port cluster-host quorum-size ha-group]
     (.createPlatformManager PlatformLocator/factory
       cluster-port cluster-host quorum-size ha-group)))

(defn deploy-verticle
  "Deploys the verticle with the given main file path using platform-manager.
   main must be on the effective classpath.

   Takes the following keyword arguments [default]:

   :classpath  The classpath to use for the verticle as a
               sequence of URLs ['()]
   :config     A configuration map for the verticle [{}]
   :handler    Either be a two-arity fn that will be passed
               the exception (if any) and verticle id from the
               result of the deploy call, or a
               org.vertx.java.core.Handler that will be
               called with the AsyncResult object that wraps
               the exception and id [nil]
   :includes   A sequence of modules to include [nil]
   :instances  The number of instances of the verticle to
               deploy [1]"
  [^PlatformManager platform-manager main
   & {:keys [config classpath instances includes handler]
      :or {config {} instances 1}}]
  (.deployVerticle platform-manager
    main
    (util/encode config)
    (into-array URL classpath)
    instances
    (if includes (str/join "," includes))
    (core/as-async-result-handler handler)))

(defn deploy-worker-verticle
  "Deploys the worker verticle with the given main file path using platform-manager.
   main must be on the effective classpath.

   Takes the following keyword arguments [default]:

   :classpath        The classpath to use for the verticle as a
                     sequence of URLs ['()]
   :config           A configuration map for the verticle [{}]
   :handler          Either be a two-arity fn that will be passed
                     the exception-map (if any) and verticle id from the
                     result of the deploy call, or a
                     org.vertx.java.core.Handler that will be
                     called with the AsyncResult object that wraps
                     the exception and id [nil]
   :includes         A sequence of modules to include [nil]
   :instances        The number of instances of the verticle to
                     deploy [1]
   :multi-threaded?  When true, the verticle will be accessed
                     by multiple threads concurrently [false]"
  [^PlatformManager platform-manager main
   & {:keys [classpath config instances includes multi-threaded? handler]
      :or {config {} instances 1}}]
  (.deployWorkerVerticle platform-manager
    (boolean multi-threaded?)
    main
    (util/encode config)
    (into-array URL classpath)
    instances
    (if includes (str/join "," includes))
    (core/as-async-result-handler handler)))

(defn deploy-module
  "Deploys the module with the given name to platform-manager.

   Takes the following keyword arguments [default]:

   :config     A configuration map for the module [{}]
   :ha?        If true then the module is enabled for ha and will
               failover to any other vert.x instances with the
               same group in the cluster [false]
   :handler    Either be a two-arity fn that will be passed
               the exception-map (if any) and module id from the
               result of the deploy call, or a
               org.vertx.java.core.Handler that will be
               called with the AsyncResult object that wraps
               the exception and id [nil]
   :instances  The number of instances of the module to
               deploy [1]"
  [^PlatformManager platform-manager module-name
   & {:keys [config instances ha? handler]
      :or {config {} instances 1}}]
  (.deployModule platform-manager
    module-name
    (util/encode config)
    instances
    (boolean ha?)
    (core/as-async-result-handler handler)))

(defn deploy-module-from-zip
  "Deploys the module from the given zip file to platform-manager.
   The zip must contain a valid Vert.x module. Vert.x will
   automatically install the module from the zip into the local mods
   dir or the system mods dir (if it's a system module), or VERTX_MODS
   if set, and then deploy the module.

   Takes the following keyword arguments [default]:

   :config     A configuration map for the module [{}]
   :ha?        If true then the module is enabled for ha and will
               failover to any other vert.x instances with the
               same group in the cluster [false]
   :handler    Either be a two-arity fn that will be passed
               the exception-map (if any) and module id from the
               result of the deploy call, or a
               org.vertx.java.core.Handler that will be
               called with the AsyncResult object that wraps
               the exception and id [nil]
   :instances  The number of instances of the module to
               deploy [1]"
  [^PlatformManager platform-manager zip-file-path
   & {:keys [config instances handler]
      :or {config {} instances 1}}]
  (.deployModuleFromZip platform-manager
    zip-file-path
    (util/encode config)
    instances
    (core/as-async-result-handler handler)))

(defn deploy-module-from-classpath
  "Deploys a module from the classpath to platform-manager.
   The classpath must contain a single mod.json and the resources for
   that module only.

   Takes the following keyword arguments [default]:

   :classpath  The classpath to use for the verticle as a
               sequence of URLs ['()]
   :config     A configuration map for the module [{}]
   :handler    Either be a two-arity fn that will be passed
               the exception-map (if any) and module id from the
               result of the deploy call, or a
               org.vertx.java.core.Handler that will be
               called with the AsyncResult object that wraps
               the exception and id [nil]
   :instances  The number of instances of the module to
               deploy [1]"
  [^PlatformManager platform-manager module-name
   & {:keys [classpath config instances handler]
      :or {config {} instances 1}}]
  (.deployModuleFromClasspath platform-manager
    module-name
    (util/encode config)
    instances
    (into-array URL classpath)
    (core/as-async-result-handler handler)))

(defn undeploy
  "Undeploys the deployment identified by id from platform-manager.
   handler can either be a single-arity fn that will be passed the
   exception (if any) from the result of the undeploy call, or a
   org.vertx.java.core.Handler that will be called with the
   AsyncResult object that wraps the exception."
  ([^PlatformManager platform-manager id]
     (undeploy platform-manager id nil))
  ([^PlatformManager platform-manager id handler]
     (.undeploy platform-manager id
       (core/as-async-result-handler handler false))))

(defn undeploy-all
  "Undeploys the all deployments from platform-manager.
   handler can either be a single-arity fn that will be passed the
   exception (if any) from the result of the undeploy call, or a
   org.vertx.java.core.Handler that will be called with the
   AsyncResult object that wraps the exception."
  ([^PlatformManager platform-manager]
     (undeploy-all platform-manager nil))
  ([^PlatformManager platform-manager handler]
     (.undeployAll platform-manager
       (core/as-async-result-handler handler false))))

(defn instances
  "Returns a map of deployment ids to number of instances."
  [^PlatformManager platform-manager]
  (into {} (.listInstances platform-manager)))

(defn install-module
  "Downloads and installs the named module locally.
   handler can either be a single-arity fn that will be passed the
   exception (if any) from the result of the install call, or a
   org.vertx.java.core.Handler that will be called with the
   AsyncResult object that wraps the exception."
  ([^PlatformManager platform-manager module-name]
     (install-module platform-manager module-name nil))
  ([^PlatformManager platform-manager module-name handler]
     (.installModule platform-manager module-name
       (core/as-async-result-handler handler false))))

(defn uninstall-module
  "Removes the module from the local installation.
   handler can either be a single-arity fn that will be passed the
   exception (if any) from the result of the uninstall call, or a
   org.vertx.java.core.Handler that will be called with the
   AsyncResult object that wraps the exception."
  ([^PlatformManager platform-manager module-name]
     (uninstall-module platform-manager module-name nil))
  ([^PlatformManager platform-manager module-name handler]
     (.uninstallModule platform-manager module-name
       (core/as-async-result-handler handler false))))

(defn pull-in-dependencies
  "Pull in all the dependencies (the 'includes' and the 'deploys'
   fields in mod.json) and copy them into an internal mods directory
   in the module. This allows a self contained module to be created.
   handler can either be a single-arity fn that will be passed the
   exception (if any) from the result of the call, or a
   org.vertx.java.core.Handler that will be called with the
   AsyncResult object that wraps the exception."
  ([^PlatformManager platform-manager module-name]
     (pull-in-dependencies platform-manager module-name nil))
  ([^PlatformManager platform-manager module-name handler]
     (.pullInDependencies platform-manager module-name
       (core/as-async-result-handler handler false))))

(defn make-fat-jar
  "Create a fat executable jar which includes the Vert.x binaries and
   the module so it can be run directly with java without having to
   pre-install Vert.x. e.g.: java -jar mymod~1.0-fat.jar
   handler can either be a single-arity fn that will be passed the
   exception (if any) from the result of the call, or a
   org.vertx.java.core.Handler that will be called with the
   AsyncResult object that wraps the exception."
  ([^PlatformManager platform-manager module-name output-dir]
     (make-fat-jar platform-manager module-name output-dir nil))
  ([^PlatformManager platform-manager module-name output-dir handler]
     (.makeFatJar platform-manager module-name output-dir
       (core/as-async-result-handler handler false))))

(defn on-exit
  "Register a handler that will be called when the platform exits.
   handler can either be a zero-arity fn or a Handler instance."
  [^PlatformManager platform-manager handler]
  (.registerExitHandler platform-manager
    (core/as-void-handler handler)))


