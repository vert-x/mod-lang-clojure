(ns isolation.v2
  (:require [vertx.eventbus :as eb]
            [isolation.shared :as shared]))

(println "v2: value is " @shared/a-value)
(eb/send "isolation" (swap! shared/a-value inc))
