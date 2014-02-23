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
            [vertx.buffer :as buf]
            [vertx.testtools :as t]
            [vertx.utils :as u]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is use-fixtures]])
  (:import [java.nio.file Files LinkOption Paths]
           java.nio.file.attribute.PosixFilePermissions
           org.vertx.java.core.file.FileSystemException))

(def ^{:dynamic true :test t/test-complete*} *tmp-dir* nil)

(defn ^{:test t/test-complete*} with-tmp-dir [f]
  (let [tmp-dir (str "target/mod-lang-clojure-tests-" (u/uuid) "/")
        tmp-dir-file (io/file tmp-dir)]
    (fs/mkdir tmp-dir)
    (t/on-complete
     #(if (fs/exists? tmp-dir)
        (fs/delete tmp-dir true)))
    (binding [*tmp-dir* tmp-dir]
      (f))))

(use-fixtures :each t/as-embedded with-tmp-dir)

(defn ^{:test t/test-complete*} resource-path [name] (t/resource-path name))

(defn ^{:test t/test-complete*} get-perms [path]
  (PosixFilePermissions/toString
   (Files/getPosixFilePermissions (Paths/get path (make-array String 0))
                                  (make-array LinkOption 0))))

(deftest copy
  (let [src (resource-path "test-data/file.txt")
        dest (str *tmp-dir* "file.txt")]
    (fs/copy src dest)
    (t/test-complete
     (is (= (slurp src) (slurp dest))))))

(deftest copy-non-existent-file
  (let [src "non-existent-file"
        dest (str *tmp-dir* "file.txt")]
    (try
      (fs/copy src dest)
      (catch FileSystemException _
        (t/test-complete)))))

(deftest recursive-copy
  (let [src (resource-path "test-data/dir")
        dest (str *tmp-dir* "/dir")]
    (fs/copy src dest true)
    (t/test-complete
     (is (= (slurp (str src "/a/b.txt")) (slurp (str dest "/a/b.txt")))))))

(deftest move
  (let [src (resource-path "test-data/file.txt")
        interim (str *tmp-dir* "file1.txt")
        dest (str *tmp-dir* "file.txt")]
    (fs/copy src interim)
    (fs/move interim dest)
    (t/test-complete
     (is (= (slurp src) (slurp dest))))))

(deftest move-non-existent-file
  (let [src "non-existent-file"
        dest (str *tmp-dir* "file.txt")]
    (try
      (fs/move src dest)
      (catch FileSystemException _
        (t/test-complete)))))

(deftest truncate
  (let [src (resource-path "test-data/file.txt")
        dest (str *tmp-dir* "file.txt")]
    (fs/copy src dest)
    (fs/truncate dest 1)
    (t/test-complete
     (is (= "b" (slurp dest))))))

(deftest truncate-non-existent-file
  (try
    (fs/truncate "non-existent-file" 0)
    (catch FileSystemException _
      (t/test-complete))))

(deftest chmod
  (let [file (str *tmp-dir* "some-file.txt")]
    (fs/create-file file "rw-------")
    (is (= "rw-------" (get-perms file)))
    (fs/chmod file "rwxrwxrwx")
    (t/test-complete
     (is (= "rwxrwxrwx" (get-perms file))))))

(deftest properties
  (let [src (resource-path "test-data/file.txt")
        props (fs/properties src)]
    (t/test-complete
     (is (= 5 (:size props)))
     (is (true?  (:regular-file? props)))
     (is (false? (:directory? props)))
     (is (false? (:other? props)))
     (is (false? (:symbolic-link? props))))))

(deftest properties-with-dir
  (let [src (resource-path "test-data")
        props (fs/properties src)]
    (t/test-complete
     (is (true? (:directory? props)))
     (is (false? (:regular-file? props)))
     (is (false? (:other? props)))
     (is (false? (:symbolic-link? props))))))

(deftest link
  (let [src (resource-path "test-data/file.txt")
        dest (str *tmp-dir* "file.txt")]
    (fs/link dest src)
    (t/test-complete
     (is (true? (fs/exists? dest)))
     (is (true? (:symbolic-link? (fs/properties dest false)))))))

(deftest hard-link
  (let [src (resource-path "test-data/file.txt")
        dest (str *tmp-dir* "file.txt")]
    (fs/link dest src false)
    (let [props (fs/properties dest false)]
      (t/test-complete
       (is (true? (:regular-file? props)))
       (is (false? (:symbolic-link? props)))))))

(deftest resolve-symlink
  (let [src (resource-path "test-data/file.txt")
        dest (str *tmp-dir* "file.txt")]
    (fs/link dest src)
    (t/test-complete
     (is (= src (fs/resolve-symlink dest))))))

(deftest delete
  (let [src (resource-path "test-data/file.txt")
        dest (str *tmp-dir* "file.txt")]
    (fs/copy src dest)
    (fs/delete dest)
    (t/test-complete
     (is (false? (.exists (io/file dest)))))))

(deftest mkdir
  (let [dir (str *tmp-dir* "foo")]
    (fs/mkdir dir)
    (t/test-complete
     (is (true? (.exists (io/file dir)))))))

(deftest mkdir-with-parents
  (let [dir (str *tmp-dir* "foo/bar")]
    (fs/mkdir dir true)
    (t/test-complete
     (is (true? (.exists (io/file dir)))))))

(deftest read-dir
  (let [dir (resource-path "test-data/dir")
        files (fs/read-dir dir)]
    (t/test-complete
     (is (true? (vector? files)))
     (is (= #{(resource-path "test-data/dir/a")
              (resource-path "test-data/dir/xyz")}
            (set files))))))

(deftest read-dir-with-filter
  (let [dir (resource-path "test-data/dir")
        files (fs/read-dir dir #"xyz")]
    (t/test-complete
     (is (true? (vector? files)))
     (is (= [(resource-path "test-data/dir/xyz")] files)))))

(deftest read-file
  (let [path (resource-path "test-data/file.txt")
        content (fs/read-file path)]
    (t/test-complete
     (is (= (slurp path) (str content))))))

(deftest write-file
  (let [path (str *tmp-dir* "output")
        data "ham biscuit"]
    (fs/write-file path data)
    (t/test-complete
     (is (= data (slurp path))))))

;; TODO: test kwargs
(deftest open
  (let [path (str *tmp-dir* "file")
        file (fs/open path)]
    (t/test-complete
     (is (not (nil? file)))
     (is (true? (.exists (io/file path)))))))

(deftest create-file
  (let [path (str *tmp-dir* "file")
        file (fs/create-file path)]
    (t/test-complete
     (is (true? (.exists (io/file path)))))))

(deftest create-file-with-perms
  (let [path (str *tmp-dir* "file")
        file (fs/create-file path "rwx------")]
    (t/test-complete
     (is (true? (.exists (io/file path))))
     (is (= "rwx------" (get-perms path))))))

(deftest exists?
  (t/test-complete
   (is (true? (fs/exists? (resource-path "test-data/file.txt"))))))

(deftest exists?-with-non-existent-file
  (t/test-complete
   (is (false? (fs/exists? "does-not-exist")))))

(deftest file-system-properties
  (let [props (fs/file-system-properties ".")]
    (t/test-complete
     (is (true? (every? (complement nil?)
                        (-> props
                            (select-keys [:total-space
                                          :unallocated-space
                                          :usable-space])
                            vals)))))))
