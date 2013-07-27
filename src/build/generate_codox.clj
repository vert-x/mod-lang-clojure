(ns generate-codox
  (:require [codox.main :as codox]
            codox.writer.html))

(codox/generate-docs
 {:group "${project.groupId}",
  :output-dir "target/html-docs",
  :name "Vert.x Clojure language module",
  :sources ["src/main/clojure"],
  :description ""
  :src-dir-uri "https://github.com/vert-x/mod-lang-clojure/tree/master/"
  :src-linenum-anchor-prefix "L"
  :writer 'codox.writer.html/write-docs})
