(ns isolation.v1
  (:require [vertx.eventbus :as eb]
            [isolation.shared :as shared]))

(println "v1: value is " @shared/a-value)
(eb/send "isolation" (swap! shared/a-value inc))
