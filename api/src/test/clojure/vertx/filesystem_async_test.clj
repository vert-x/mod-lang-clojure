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

(ns vertx.filesystem-async-test
  (:require [vertx.filesystem :as fs]
            [vertx.filesystem.sync :as sfs]
            [vertx.buffer :as buf]
            [vertx.testtools :as t]
            [vertx.utils :as u]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is use-fixtures]])
  (:import [java.nio.file Files LinkOption Paths]
           java.nio.file.attribute.PosixFilePermissions))

(def ^:dynamic *tmp-dir* nil)

(defn with-tmp-dir [f]
  (let [tmp-dir (str "target/mod-lang-clojure-tests-" (u/uuid) "/")
        tmp-dir-file (io/file tmp-dir)]
    (sfs/mkdir tmp-dir)
    (t/on-complete
     #(if (sfs/exists? tmp-dir)
        (sfs/delete tmp-dir true)))
    (binding [*tmp-dir* tmp-dir]
      (f))))

(use-fixtures :each t/as-embedded with-tmp-dir)

(defn resource-path [name] (t/resource-path name))

(defn get-perms [path]
  (PosixFilePermissions/toString
   (Files/getPosixFilePermissions (Paths/get path (make-array String 0))
                                  (make-array LinkOption 0))))

(deftest asyc-copy
  (let [src (resource-path "test-data/file.txt")
        dest (str *tmp-dir* "file.txt")]
    (fs/copy src dest
             (fn [err]
               (t/test-complete
                (is (nil? err))
                (is (= (slurp src) (slurp dest))))))))

(deftest copy-non-existent-file
  (let [src "non-existent-file"
        dest (str *tmp-dir* "file.txt")]
    (fs/copy src dest
             (fn [err]
               (t/test-complete
                (is (not (nil? err))))))))

(deftest recursive-copy
  (let [src (resource-path "test-data/dir")
        dest (str *tmp-dir* "/dir")]
    (fs/copy src dest true
             (fn [err]
               (t/test-complete
                (is (nil? err))
                (is (= (slurp (str src "/a/b.txt")) (slurp (str dest "/a/b.txt")))))))))

(deftest move
  (let [src (resource-path "test-data/file.txt")
        interim (str *tmp-dir* "file1.txt")
        dest (str *tmp-dir* "file.txt")]
    (fs/copy src interim
             (fn [err]
               (is (nil? err))
               (fs/move interim dest
                        (fn [err]
                          (t/test-complete
                           (is (nil? err))
                           (is (= (slurp src) (slurp dest))))))))))

(deftest move-non-existent-file
  (let [src "non-existent-file"
        dest (str *tmp-dir* "file.txt")]
    (fs/move src dest
             (fn [err]
               (t/test-complete
                (is (not (nil? err))))))))

(deftest truncate
  (let [src (resource-path "test-data/file.txt")
        dest (str *tmp-dir* "file.txt")]
    (fs/copy src dest
             (fn [err]
               (is (nil? err))
               (fs/truncate dest 1
                            (fn [err]
                              (t/test-complete
                               (is (nil? err))
                               (is (= "b" (slurp dest))))))))))

(deftest truncate-non-existent-file
  (fs/truncate "non-existent-file" 0
               (fn [err]
                 (t/test-complete
                  (is (not (nil? err)))))))

(deftest chmod
  (let [file (str *tmp-dir* "some-file.txt")]
    (fs/create-file
     file "rw-------"
     (fn [err]
       (is (nil? err))
       (is (= "rw-------" (get-perms file)))
       (fs/chmod
        file "rwxrwxrwx"
        (fn [err]
          (t/test-complete
           (is (nil? err))
           (is (= "rwxrwxrwx" (get-perms file))))))))))

(deftest properties
  (let [src (resource-path "test-data/file.txt")]
    (fs/properties src
                   (fn [err props]
                     (t/test-complete
                      (is (nil? err))
                      (is (= 5 (:size props)))
                      (is (true? (:regular-file? props)))
                      (is (false? (:directory? props)))
                      (is (false? (:other? props)))
                      (is (false? (:symbolic-link? props))))))))

(deftest properties-with-dir
  (let [src (resource-path "test-data")]
    (fs/properties src
                   (fn [err props]
                     (t/test-complete
                      (is (nil?  err))
                      (is (true? (:directory? props)))
                      (is (false? (:regular-file? props)))
                      (is (false? (:other? props)))
                      (is (false? (:symbolic-link? props))))))))

(deftest link
  (let [src (resource-path "test-data/file.txt")
        dest (str *tmp-dir* "file.txt")]
    (fs/link dest src
             (fn [err]
               (is (nil? err))
               (fs/properties dest false
                              (fn [err props]
                                (t/test-complete
                                 (is (nil? err))
                                 (is (true? (:symbolic-link? props))))))))))

(deftest hard-link
  (let [src (resource-path "test-data/file.txt")
        dest (str *tmp-dir* "file.txt")]
    (fs/link dest src false
             (fn [err]
               (is (nil? err))
               (fs/properties dest false
                              (fn [err props]
                                (t/test-complete
                                 (is (nil? err))
                                 (is (true? (:regular-file? props)))
                                 (is (false? (:symbolic-link? props))))))))))

(deftest resolve-symlink
  (let [src (resource-path "test-data/file.txt")
        dest (str *tmp-dir* "file.txt")]
    (fs/link dest src
             (fn [err]
               (is (nil? err))
               (fs/resolve-symlink dest
                                   (fn [err actual]
                                     (t/test-complete
                                      (is (nil? err))
                                      (is (= src actual)))))))))

(deftest delete
  (let [src (resource-path "test-data/file.txt")
        dest (str *tmp-dir* "file.txt")]
    (fs/copy src dest
             (fn [err]
               (is (nil? err))
               (fs/delete dest
                          (fn [err]
                            (t/test-complete
                             (is (nil? err))
                             (is (false? (.exists (io/file dest)))))))))))

(deftest mkdir
  (let [dir (str *tmp-dir* "foo")]
    (fs/mkdir dir
              (fn [err]
                (t/test-complete
                 (is (nil? err))
                 (is (true? (.exists (io/file dir)))))))))

(deftest mkdir-with-parents
  (let [dir (str *tmp-dir* "foo/bar")]
    (fs/mkdir dir true
              (fn [err]
                (t/test-complete
                 (is (nil? err))
                 (is (true? (.exists (io/file dir)))))))))

(deftest read-dir
  (let [dir (resource-path "test-data/dir")]
    (fs/read-dir dir
                 (fn [err files]
                   (t/test-complete
                    (is (nil? err))
                    (is (true? (vector? files)))
                    (is (= #{(resource-path "test-data/dir/a")
                             (resource-path "test-data/dir/xyz")}
                           (set files))))))))

(deftest read-dir-with-filter
  (let [dir (resource-path "test-data/dir")]
    (fs/read-dir dir #"xyz"
                 (fn [err files]
                   (t/test-complete
                    (is (nil? err))
                    (is (true? (vector? files)))
                    (is (= [(resource-path "test-data/dir/xyz")] files)))))))

(deftest read-file
  (let [path (resource-path "test-data/file.txt")]
    (fs/read-file path
                  (fn [err content]
                    (t/test-complete
                     (is (nil? err))
                     (is (= (slurp path) (str content))))))))

(deftest write-file
  (let [path (str *tmp-dir* "output")
        data "ham biscuit"]
    (fs/write-file path data
                   (fn [err]
                     (t/test-complete
                      (is (nil? err))
                      (is (= data (slurp path))))))))

;; TODO: test kwargs
(deftest open
  (let [path (str *tmp-dir* "file")]
    (fs/open path
             (fn [err file]
               (t/test-complete
                (is (nil? err))
                (is (not (nil? file)))
                (is (true? (.exists (io/file path)))))))))

(deftest create-file
  (let [path (str *tmp-dir* "file")]
    (fs/create-file path
                    (fn [err]
                      (t/test-complete
                       (is (nil? err))
                       (is (true? (.exists (io/file path)))))))))

(deftest create-file-with-perms
  (let [path (str *tmp-dir* "file")]
    (fs/create-file path "rwx------"
                    (fn [err]
                      (t/test-complete
                       (is (nil? err))
                       (is (true? (.exists (io/file path))))
                       (is (= "rwx------" (get-perms path))))))))

(deftest exists?
  (fs/exists? (resource-path "test-data/file.txt")
              (fn [err exists?]
                (t/test-complete
                 (is (nil? err))
                 (is (true? exists?))))))

(deftest exists?-with-non-existent-file
  (fs/exists? "does-not-exist"
              (fn [err exists?]
                (t/test-complete
                 (is (nil? err))
                 (is (false? exists?))))))

(deftest file-system-properties
  (fs/file-system-properties
   "."
   (fn [err props]
     (t/test-complete
      (is (nil? err))
      (is (true?  (every? (complement nil?)
                          (-> props
                              (select-keys [:total-space
                                            :unallocated-space
                                            :usable-space])
                              vals))))))))

(deftest write
  (let [path (str *tmp-dir* "file")]
    (fs/open
     path
     (fn [err file]
       (is (nil? err))
       (fs/write
        file "hello" 0
        (fn [err]
          (t/test-complete
           (is (nil? err))
           (is (= "hello" (slurp path)))))))
     :flush true)))

(deftest asyc-read
  (let [path (resource-path "test-data/file.txt")]
    (fs/open
     path
     (fn [err file]
       (is (nil? err))
       (fs/read
        file 0 2
        (fn [err content]
          (t/test-complete
           (is (nil? err))
           (is (= "bo" (str content))))))))))

(deftest read-with-buffer
  (let [path (resource-path "test-data/file.txt")
        b! (buf/buffer)]
    (fs/open
     path
     (fn [err file]
       (is (nil? err))
       (fs/read
        file b! 0 0 2
        (fn [err content]
          (t/test-complete
           (is (nil? err))
           (is (identical? b! content))
           (is (= "bo" (str content))))))))))

(deftest close
  (let [path (resource-path "test-data/file.txt")]
    (fs/open
     path
     (fn [err file]
       (is (nil? err))
       (fs/close
        file
        (fn [err]
          (t/test-complete
           (is (nil? err)))))))))

(deftest async-flush
  (let [path (str *tmp-dir* "file")]
    (fs/open
     path
     (fn [err file]
       (is (nil? err))
       (fs/write
        file "hello" 0
        (fn [err]
          (is (nil? err))
          (fs/flush
           file
           (fn [err]
             (t/test-complete
              (is (nil? err))
              (is (= "hello" (slurp path)))))))))
     :flush false)))
