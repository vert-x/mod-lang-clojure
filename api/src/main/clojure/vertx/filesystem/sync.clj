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

(ns vertx.filesystem.sync
  "Provides a broad set of functions for manipulating files. Wraps the
   synchronous methods from org.vertx.java.core.file.FileSystem."
  (:refer-clojure :exclude [read flush])
  (:require [vertx.filesystem :as fs]
            [vertx.core :as core]
            [vertx.buffer :as buf]))

(defn copy
  "Copy a file from the src path to dest path, synchronously.
   If recursive? is true and src represents a directory, then the
   directory and its contents will be copied recursively to
   dest. recursive? defaults to false. 

   The copy will fail if the destination already exists."
  ([src dest]
     (.copySync (fs/get-file-system) src dest))
  ([src dest recursive?]
     (.copySync (fs/get-file-system) src dest (boolean recursive?))))

(defn move
  "Move a file from the src path to dest path, synchronously.
   
   The move will fail if the destination already exists."
  [src dest]
  (.moveSync (fs/get-file-system) src dest))

(defn truncate
  "Truncate the file represented by path to length len in bytes, synchronously.
   
   The truncate will fail if the file does not exist or len is less
   than zero."
  [path len]
  (.truncateSync (fs/get-file-system) path len))

(defn chmod
  "Change the permissions on the file represented path to perms, synchronously.
   If dir-perms is provided and path is a directory, it will have its
   permisions changed recursively, with perms being applied to any
   files, and dir-perms being applied to directories.

   The permission strings take the form rwxr-x--- as specified by
   http://download.oracle.com/javase/7/docs/api/java/nio/file/attribute/PosixFilePermissions.html"
  ([path perms]
     (.chmodSync (fs/get-file-system) path perms))
  ([path perms dir-perms]
     (.chmodSync (fs/get-file-system) path perms dir-perms)))

(defn properties
  "Obtain properties for the file represented by path, synchronously.

   The returned properties map will contain :creation-time,
   :last-access-time, :last-modified-time, :directory?, :regular-file?,
   :symbolic-link?, :other?, and :size.

   If the follow-link? is true (the default) and file is a link, the
   link will be followed."
  ([path]
     (properties path true))
  ([path follow-link?]
     (fs/file-props->map
      (if follow-link?
        (.propsSync (fs/get-file-system) path)
        (.lpropsSync (fs/get-file-system) path)))))

(defn link
  "Creates a link on the file system from path to existing, synchronously.
   If symbolic? is true (the default), the resulting link is symbolic,
   otherwise a had link is created."
  ([path existing]
     (link path existing true))
  ([path existing symbolic?]
     (if symbolic?
       (.symlinkSync (fs/get-file-system) path existing)
       (.linkSync (fs/get-file-system) path existing))))

(defn resolve-symlink
  "Returns the path representing the file that the symbolic link specified by path points to, synchronously."
  [path]
  (.readSymlinkSync (fs/get-file-system) path))

(defn delete
  "Deletes the file on the file system represented by path, synchronously.
   
   If the recursive? is true (default is false) and file is a
   directory, it will be deleted recursively."
  ([path]
     (delete path false))
  ([path recursive?]
     (.deleteSync (fs/get-file-system) path (boolean recursive?))))
  
(defn mkdir
  "Create the directory represented by path, synchronously.
   If create-parents? is true (default is false), any non-existent
   parent directories of the directory will also be created.  If perms
   are provided, they will override the default permissions for the
   created directory. The permission String takes the form rwxr-x---
   as specified by:
   http://download.oracle.com/javase/7/docs/api/java/nio/file/attribute/PosixFilePermissions.html
   
   The operation will fail if the directory already exists."
  ([path]
     (mkdir path false nil))
  ([path create-parents?]
     (mkdir path create-parents? nil))
  ([path create-parents? perms]
     (.mkdirSync (fs/get-file-system) path perms (boolean create-parents?))))

(defn read-dir
  "Read the contents of the directory specified by path, synchronously.
   If a filter regex is specified then only the paths that match it
   will be returned."
  ([path]
     (read-dir path nil))
  ([path filter]
     (into []
           (.readDirSync (fs/get-file-system) path (if filter (str filter))))))

(defn read-file
  "Reads the entire file as represented by the path path as a Buffer, synchronously.
   
   Do not user this method to read very large files or you
   risk running out of available RAM."
  [path]
  (.readFileSync (fs/get-file-system) path))

(defn write-file
  "Creates the file, and writes the specified data to the file represented by path, synchronously."
  [path data]
  (.writeFileSync (fs/get-file-system) path (buf/buffer data)))

(defn open
  "Open the file represented by path, synchronously.
   Returns an AsyncFile instance.

   The behavior of the open call is further controlled by a set of
   kwarg arguments [default]:

   * create? - create the file if it does not already exist [true]
   * read?   - open the file for reading [true]
   * write?  - open the file for writing [true]
   * flush?  - the opened file will auto-flush writes [false]
   * perms   - the permissions used to create the file, if necessary
               (see create-file) [nil]"
  [path & {:keys [perms read? write? create? flush?]
           :or {read? true write? true create? true}}]
  (.openSync (fs/get-file-system) path perms
             (boolean read?)
             (boolean write?)
             (boolean create?)
             (boolean flush?)))

(defn create-file
  "Creates an empty file with the specified path, synchronously.
   If perms are provided, they will override the default permissions
   for the created file. The permission String takes the form
   rwxr-x--- as specified by:
   http://download.oracle.com/javase/7/docs/api/java/nio/file/attribute/PosixFilePermissions.html"
  ([path]
     (create-file path nil))  
  ([path perms]
       (.createFileSync (fs/get-file-system) path perms)))

(defn exists?
  "Determines whether the file as specified by the path {@code path} exists, synchronously."
  [path]
  (.existsSync (fs/get-file-system) path))

(defn file-system-properties
  "Returns properties of the file-system being used by the specified path, synchronously.
   can either be a two-arity fn that will be passed the
   exception (if any) and properties (as a map) from the result of the
   call, or a that will be called with the AsyncResult object
   that wraps the exception and FileSystemProps object."
  [path]
  (fs/file-system-props->map
   (.fsPropsSync (fs/get-file-system) path)))


