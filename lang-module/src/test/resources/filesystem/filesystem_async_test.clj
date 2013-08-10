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
            [vertx.buffer :as buf]
            [vertx.testtools :as t]
            [vertx.utils :as u]
            [clojure.java.io :as io])
  (:import [java.nio.file Files LinkOption Paths]
           java.nio.file.attribute.PosixFilePermissions))

(def ^:dynamic *tmp-dir* nil)

(defn with-tmp-dir [f]
  (let [tmp-dir (str "target/mod-lang-clojure-tests-" (u/uuid) "/")]
    (fs/mkdir
     tmp-dir
     (fn [err]
       (t/assert-nil err)
       (t/on-complete
        (partial fs/delete tmp-dir true t/assert-nil))
       (binding [*tmp-dir* tmp-dir]
         (f))))))

(defn resource-path [name]
  (t/resource-path (str "filesystem/" name)))

(defn get-perms [path]
  (PosixFilePermissions/toString
   (Files/getPosixFilePermissions (Paths/get path (make-array String 0))
                                  (make-array LinkOption 0))))

(defn test-copy []
  (let [src (resource-path "test-data/file.txt")
        dest (str *tmp-dir* "file.txt")]
    (fs/copy src dest
             (fn [err]
               (t/test-complete
                (t/assert-nil err)
                (t/assert= (slurp src) (slurp dest)))))))

(defn test-copy-non-existent-file []
  (let [src "non-existent-file"
        dest (str *tmp-dir* "file.txt")]
    (fs/copy src dest
             (fn [err]
               (t/test-complete
                (t/assert-not-nil err))))))

(defn test-recursive-copy []
  (let [src (resource-path "test-data/dir")
        dest (str *tmp-dir* "/dir")]
    (fs/copy src dest true
             (fn [err]
               (t/test-complete
                (t/assert-nil err)
                (t/assert= (slurp (str src "/a/b.txt")) (slurp (str dest "/a/b.txt"))))))))

(defn test-move []
  (let [src (resource-path "test-data/file.txt")
        interim (str *tmp-dir* "file1.txt")
        dest (str *tmp-dir* "file.txt")]
    (fs/copy src interim
             (fn [err]
               (t/assert-nil err)
               (fs/move interim dest
                        (fn [err]
                          (t/test-complete
                           (t/assert-nil err)
                           (t/assert= (slurp src) (slurp dest)))))))))

(defn test-move-non-existent-file []
  (let [src "non-existent-file"
        dest (str *tmp-dir* "file.txt")]
    (fs/move src dest
             (fn [err]
               (t/test-complete
                (t/assert-not-nil err))))))

(defn test-truncate []
  (let [src (resource-path "test-data/file.txt")
        dest (str *tmp-dir* "file.txt")]
    (fs/copy src dest
             (fn [err]
               (t/assert-nil err)
               (fs/truncate dest 1
                        (fn [err]
                          (t/test-complete
                           (t/assert-nil err)
                           (t/assert= "b" (slurp dest)))))))))

(defn test-truncate-non-existent-file []
  (fs/truncate "non-existent-file" 0
               (fn [err]
                 (t/test-complete
                  (t/assert-not-nil err)))))

(defn test-chmod []
  (let [file (str *tmp-dir* "some-file.txt")]
    (fs/create-file
     file "rw-------"
     (fn [err]
       (t/assert-nil err)
       (t/assert= "rw-------" (get-perms file))
       (fs/chmod
        file "rwxrwxrwx"
        (fn [err]
          (t/test-complete
           (t/assert-nil err)
           (t/assert= "rwxrwxrwx" (get-perms file)))))))))

(defn test-properties []
  (let [src (resource-path "test-data/file.txt")]
    (fs/properties src
             (fn [err props]
               (t/test-complete
                (t/assert-nil err)
                (t/assert= 5 (:size props))
                (t/assert (:regular-file? props))
                (t/assert (not (:directory? props)))
                (t/assert (not (:other? props)))
                (t/assert (not (:symbolic-link? props))))))))

(defn test-properties-with-dir []
  (let [src (resource-path "test-data")]
    (fs/properties src
             (fn [err props]
               (t/test-complete
                (t/assert-nil err)
                (t/assert (:directory? props))
                (t/assert (not (:regular-file? props)))
                (t/assert (not (:other? props)))
                (t/assert (not (:symbolic-link? props))))))))

(defn test-link []
  (let [src (resource-path "test-data/file.txt")
        dest (str *tmp-dir* "file.txt")]
    (fs/link dest src
             (fn [err]
               (t/assert-nil err)
               (fs/properties dest false
                              (fn [err props]
                                (t/test-complete
                                 (t/assert-nil err)
                                 (t/assert (:symbolic-link? props)))))))))

(defn test-hard-link []
  (let [src (resource-path "test-data/file.txt")
        dest (str *tmp-dir* "file.txt")]
    (fs/link dest src false
             (fn [err]
               (t/assert-nil err)
               (fs/properties dest false
                              (fn [err props]
                                (t/test-complete
                                 (t/assert-nil err)
                                 (t/assert (:regular-file? props))
                                 (t/assert (not (:symbolic-link? props))))))))))

(defn test-resolve-symlink []
  (let [src (resource-path "test-data/file.txt")
        dest (str *tmp-dir* "file.txt")]
    (fs/link dest src
             (fn [err]
               (t/assert-nil err)
               (fs/resolve-symlink dest
                                   (fn [err actual]
                                     (t/test-complete
                                      (t/assert-nil err)
                                      (t/assert= src actual))))))))

(defn test-delete []
  (let [src (resource-path "test-data/file.txt")
        dest (str *tmp-dir* "file.txt")]
    (fs/copy src dest
             (fn [err]
               (t/assert-nil err)
               (fs/delete dest
                          (fn [err]
                            (t/test-complete
                             (t/assert-nil err)
                             (t/assert (not (.exists (io/file dest)))))))))))

(defn test-mkdir []
  (let [dir (str *tmp-dir* "foo")]
    (fs/mkdir dir
              (fn [err]
                (t/test-complete
                 (t/assert-nil err)
                 (t/assert (.exists (io/file dir))))))))

(defn test-mkdir-with-parents []
  (let [dir (str *tmp-dir* "foo/bar")]
    (fs/mkdir dir true
              (fn [err]
                (t/test-complete
                 (t/assert-nil err)
                 (t/assert (.exists (io/file dir))))))))

(defn test-read-dir []
  (let [dir (resource-path "test-data/dir")]
    (fs/read-dir dir
                 (fn [err files]
                   (t/test-complete
                    (t/assert-nil err)
                    (t/assert (vector? files))
                    (t/assert= #{(resource-path "test-data/dir/a")
                                 (resource-path "test-data/dir/xyz")}
                               (set files)))))))

(defn test-read-dir-with-filter []
  (let [dir (resource-path "test-data/dir")]
    (fs/read-dir dir #"xyz"
                 (fn [err files]
                   (t/test-complete
                    (t/assert-nil err)
                    (t/assert (vector? files))
                    (t/assert= [(resource-path "test-data/dir/xyz")] files))))))

(defn test-read-file []
  (let [path (resource-path "test-data/file.txt")]
    (fs/read-file path
                 (fn [err content]
                   (t/test-complete
                    (t/assert-nil err)
                    (t/assert= (slurp path) (str content)))))))

(defn test-write-file []
  (let [path (str *tmp-dir* "output")
        data "ham biscuit"]
    (fs/write-file path data
                 (fn [err]
                   (t/test-complete
                    (t/assert-nil err)
                    (t/assert= data (slurp path)))))))

;; TODO: test kwargs
(defn test-open []
  (let [path (str *tmp-dir* "file")]
    (fs/open path
             (fn [err file]
               (t/test-complete
                (t/assert-nil err)
                (t/assert-not-nil file)
                (t/assert (.exists (io/file path))))))))

(defn test-create-file []
  (let [path (str *tmp-dir* "file")]
    (fs/create-file path
                    (fn [err]
                      (t/test-complete
                       (t/assert-nil err)
                       (t/assert (.exists (io/file path))))))))

(defn test-create-file-with-perms []
  (let [path (str *tmp-dir* "file")]
    (fs/create-file path "rwx------"
                    (fn [err]
                      (t/test-complete
                       (t/assert-nil err)
                       (t/assert (.exists (io/file path)))
                       (t/assert= "rwx------" (get-perms path)))))))

(defn test-exists? []
  (fs/exists? (resource-path "test-data/file.txt")
              (fn [err exists?]
                (t/test-complete
                 (t/assert-nil err)
                 (t/assert exists?)))))

(defn test-exists?-with-non-existent-file []
  (fs/exists? "does-not-exist"
              (fn [err exists?]
                (t/test-complete
                 (t/assert-nil err)
                 (t/assert (not exists?))))))

(defn test-file-system-properties []
  (fs/file-system-properties
   "."
   (fn [err props]
     (t/test-complete
      (t/assert-nil err)
      (t/assert  (every? (complement nil?)
                         (-> props
                             (select-keys [:total-space
                                           :unallocated-space
                                           :usable-space])
                             vals)))))))

(defn test-write []
  (let [path (str *tmp-dir* "file")]
    (fs/open
     path
     (fn [err file]
       (t/assert-nil err)
       (fs/write
        file "hello" 0
        (fn [err]
          (t/test-complete
           (t/assert-nil err)
           (t/assert= "hello" (slurp path))))))
     :flush true)))

(defn test-read []
  (let [path (resource-path "test-data/file.txt")]
    (fs/open
     path
     (fn [err file]
       (t/assert-nil err)
       (fs/read
        file 0 2
        (fn [err content]
          (t/test-complete
           (t/assert-nil err)
           (t/assert= "bo" (str content)))))))))

(defn test-read-with-buffer []
  (let [path (resource-path "test-data/file.txt")
        b! (buf/buffer)]
    (fs/open
     path
     (fn [err file]
       (t/assert-nil err)
       (fs/read
        file b! 0 0 2
        (fn [err content]
          (t/test-complete
           (t/assert-nil err)
           (t/assert (identical? b! content))
           (t/assert= "bo" (str content)))))))))

(defn test-close []
  (let [path (resource-path "test-data/file.txt")]
    (fs/open
     path
     (fn [err file]
       (t/assert-nil err)
       (fs/close
        file
        (fn [err]
          (t/test-complete
           (t/assert-nil err))))))))

(defn test-flush []
  (let [path (str *tmp-dir* "file")]
    (fs/open
     path
     (fn [err file]
       (t/assert-nil err)
       (fs/write
        file "hello" 0
        (fn [err]
          (t/assert-nil err)
          (fs/flush
           file
           (fn [err]
             (t/test-complete
              (t/assert-nil err)
              (t/assert= "hello" (slurp path))))))))
     :flush false)))

(t/start-tests with-tmp-dir)
