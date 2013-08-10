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

(ns vertx.filesystem-sync-test
  (:require [vertx.filesystem.sync :as fs]
            [vertx.filesystem.sync :as afs]
            [vertx.buffer :as buf]
            [vertx.testtools :as t]
            [vertx.utils :as u]
            [clojure.java.io :as io])
  (:import [java.nio.file Files LinkOption Paths]
           java.nio.file.attribute.PosixFilePermissions
           org.vertx.java.core.file.FileSystemException))

(def ^:dynamic *tmp-dir* nil)

(defn with-tmp-dir [f]
  (let [tmp-dir (str "target/mod-lang-clojure-tests-" (u/uuid) "/")]
    (fs/mkdir tmp-dir)
    (t/on-complete
     (partial fs/delete tmp-dir true))
    (binding [*tmp-dir* tmp-dir]
      (f))))

(defn resource-path [name]
  (t/resource-path (str "filesystem/" name)))

(defn get-perms [path]
  (PosixFilePermissions/toString
   (Files/getPosixFilePermissions (Paths/get path (make-array String 0))
                                  (make-array LinkOption 0))))

(defn test-copy []
  (let [src (resource-path "test-data/file.txt")
        dest (str *tmp-dir* "file.txt")]
    (fs/copy src dest)
    (t/test-complete
     (t/assert= (slurp src) (slurp dest)))))

(defn test-copy-non-existent-file []
  (let [src "non-existent-file"
        dest (str *tmp-dir* "file.txt")]
    (try
      (fs/copy src dest)
      (catch FileSystemException _
        (t/test-complete)))))

(defn test-recursive-copy []
  (let [src (resource-path "test-data/dir")
        dest (str *tmp-dir* "/dir")]
    (fs/copy src dest true)
    (t/test-complete
     (t/assert= (slurp (str src "/a/b.txt")) (slurp (str dest "/a/b.txt"))))))

(defn test-move []
  (let [src (resource-path "test-data/file.txt")
        interim (str *tmp-dir* "file1.txt")
        dest (str *tmp-dir* "file.txt")]
    (fs/copy src interim)
    (fs/move interim dest)
    (t/test-complete
     (t/assert= (slurp src) (slurp dest)))))

(defn test-move-non-existent-file []
  (let [src "non-existent-file"
        dest (str *tmp-dir* "file.txt")]
    (try
      (fs/move src dest)
      (catch FileSystemException _
        (t/test-complete)))))

(defn test-truncate []
  (let [src (resource-path "test-data/file.txt")
        dest (str *tmp-dir* "file.txt")]
    (fs/copy src dest)
    (fs/truncate dest 1)
    (t/test-complete
     (t/assert= "b" (slurp dest)))))

(defn test-truncate-non-existent-file []
  (try
    (fs/truncate "non-existent-file" 0)
    (catch FileSystemException _
      (t/test-complete))))

(defn test-chmod []
  (let [file (str *tmp-dir* "some-file.txt")]
    (fs/create-file file "rw-------")
    (t/assert= "rw-------" (get-perms file))
    (fs/chmod file "rwxrwxrwx")
    (t/test-complete
     (t/assert= "rwxrwxrwx" (get-perms file)))))

(defn test-properties []
  (let [src (resource-path "test-data/file.txt")
        props (fs/properties src)]
    (t/test-complete
     (t/assert= 5 (:size props))
     (t/assert (:regular-file? props))
     (t/assert (not (:directory? props)))
     (t/assert (not (:other? props)))
     (t/assert (not (:symbolic-link? props))))))

(defn test-properties-with-dir []
  (let [src (resource-path "test-data")
        props (fs/properties src)]
    (t/test-complete
     (t/assert (:directory? props))
     (t/assert (not (:regular-file? props)))
     (t/assert (not (:other? props)))
     (t/assert (not (:symbolic-link? props))))))

(defn test-link []
  (let [src (resource-path "test-data/file.txt")
        dest (str *tmp-dir* "file.txt")]
    (fs/link dest src)
    (t/test-complete
     (t/assert (fs/exists? dest))
     (t/assert (:symbolic-link? (fs/properties dest false))))))

(defn test-hard-link []
  (let [src (resource-path "test-data/file.txt")
        dest (str *tmp-dir* "file.txt")]
    (fs/link dest src false)
    (let [props (fs/properties dest false)]
      (t/test-complete
       (t/assert (:regular-file? props))
       (t/assert (not (:symbolic-link? props)))))))

(defn test-resolve-symlink []
  (let [src (resource-path "test-data/file.txt")
        dest (str *tmp-dir* "file.txt")]
    (fs/link dest src)
    (t/test-complete
     (t/assert= src (fs/resolve-symlink dest)))))

(defn test-delete []
  (let [src (resource-path "test-data/file.txt")
        dest (str *tmp-dir* "file.txt")]
    (fs/copy src dest)
    (fs/delete dest)
    (t/test-complete
     (t/assert (not (.exists (io/file dest)))))))

(defn test-mkdir []
  (let [dir (str *tmp-dir* "foo")]
    (fs/mkdir dir)
    (t/test-complete
     (t/assert (.exists (io/file dir))))))

(defn test-mkdir-with-parents []
  (let [dir (str *tmp-dir* "foo/bar")]
    (fs/mkdir dir true)
    (t/test-complete
     (t/assert (.exists (io/file dir))))))

(defn test-read-dir []
  (let [dir (resource-path "test-data/dir")
        files (fs/read-dir dir)]
    (t/test-complete
     (t/assert (vector? files))
     (t/assert= #{(resource-path "test-data/dir/a")
                  (resource-path "test-data/dir/xyz")}
                (set files)))))

(defn test-read-dir-with-filter []
  (let [dir (resource-path "test-data/dir")
        files (fs/read-dir dir #"xyz")]
    (t/test-complete
     (t/assert (vector? files))
     (t/assert= [(resource-path "test-data/dir/xyz")] files))))

(defn test-read-file []
  (let [path (resource-path "test-data/file.txt")
        content (fs/read-file path)]
    (t/test-complete
     (t/assert= (slurp path) (str content)))))

(defn test-write-file []
  (let [path (str *tmp-dir* "output")
        data "ham biscuit"]
    (fs/write-file path data)
    (t/test-complete
     (t/assert= data (slurp path)))))

;; TODO: test kwargs
(defn test-open []
  (let [path (str *tmp-dir* "file")
        file (fs/open path)]
    (t/test-complete
     (t/assert-not-nil file)
     (t/assert (.exists (io/file path))))))

(defn test-create-file []
  (let [path (str *tmp-dir* "file")
        file (fs/create-file path)]
    (t/test-complete
     (t/assert (.exists (io/file path))))))

(defn test-create-file-with-perms []
  (let [path (str *tmp-dir* "file")
        file (fs/create-file path "rwx------")]
    (t/test-complete
     (t/assert (.exists (io/file path)))
     (t/assert= "rwx------" (get-perms path)))))

(defn test-exists? []
  (t/test-complete
   (t/assert (fs/exists? (resource-path "test-data/file.txt")))))

(defn test-exists?-with-non-existent-file []
  (t/test-complete
   (t/assert (not (fs/exists? "does-not-exist")))))

(defn test-file-system-properties []
  (let [props (fs/file-system-properties ".")]
    (t/test-complete
     (t/assert  (every? (complement nil?)
                        (-> props
                            (select-keys [:total-space
                                          :unallocated-space
                                          :usable-space])
                            vals))))))

(t/start-tests with-tmp-dir)
