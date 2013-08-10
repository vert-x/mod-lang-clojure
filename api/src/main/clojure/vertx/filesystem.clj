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

(ns vertx.filesystem
  "Provides a broad set of functions for manipulating files. Wraps the
   asynchronous methods from org.vertx.java.core.file.FileSystem."
  (:refer-clojure :exclude [read flush])
  (:require [vertx.core :as core]
            [vertx.common :as common]
            [vertx.buffer :as buf]))

(defonce ^{:dynamic true
           :doc "The currently active default vertx container instance.
                 If not bound, the FileSystem from vertx.core/*vertx* will be used.
                 You should only need to bind this for advanced usage."}
  *file-system* nil)

(defn get-file-system
  "Returns the currently active FileSystem instance."
  []
  (or *file-system* (.fileSystem (core/get-vertx))))

(defn copy
  "Copy a file from the src path to dest path, asynchronously.
   If recursive? is true and src represents a directory, then the
   directory and its contents will be copied recursively to
   dest. recursive? defaults to false. handler can either be a
   single-arity fn that will be passed the exception (if any) from the
   result of the copy call, or a Handler that will be called with the
   AsyncResult object that wraps the exception.

   The copy will fail if the destination already exists."
  ([src dest handler]
     (.copy (get-file-system) src dest
            (core/as-async-result-handler handler false)))
  ([src dest recursive? handler]
     (.copy (get-file-system) src dest (boolean recursive?)
            (core/as-async-result-handler handler false))))

(defn move
  "Move a file from the src path to dest path, asynchronously.
   handler can either be a single-arity fn that will be passed the
   exception (if any) from the result of the move call, or a Handler
   that will be called with the AsyncResult object that wraps the
   exception.

   The move will fail if the destination already exists."
  [src dest handler]
  (.move (get-file-system) src dest
         (core/as-async-result-handler handler false)))

(defn truncate
  "Truncate the file represented by path to length len in bytes, asynchronously.
   handler can either be a single-arity fn that will be passed the
   exception (if any) from the result of the truncate call, or a
   Handler that will be called with the AsyncResult object that wraps
   the exception.

   The truncate will fail if the file does not exist or len is less
   than zero."
  [path len handler]
  (.truncate (get-file-system) path len
             (core/as-async-result-handler handler false)))

(defn chmod
  "Change the permissions on the file represented path to perms, asynchronously.
   If dir-perms is provided and path is a directory, it will have its
   permisions changed recursively, with perms being applied to any
   files, and dir-perms being applied to directories. handler can
   either be a single-arity fn that will be passed the exception (if
   any) from the result of the call, or a Handler that will be called
   with the AsyncResult object that wraps the exception.

   The permission strings take the form rwxr-x--- as specified by
   http://download.oracle.com/javase/7/docs/api/java/nio/file/attribute/PosixFilePermissions.html"
  ([path perms handler]
     (.chmod (get-file-system) path perms
             (core/as-async-result-handler handler false)))
  ([path perms dir-perms handler]
     (.chmod (get-file-system) path perms dir-perms
             (core/as-async-result-handler handler false))))

(defn ^:internal ^:no-doc file-props->map [props]
  (hash-map
   :creation-time      (.creationTime props)
   :last-access-time   (.lastAccessTime props)
   :last-modified-time (.lastModifiedTime props)
   :directory?         (.isDirectory props)
   :other?             (.isOther props)
   :regular-file?      (.isRegularFile props)
   :symbolic-link?     (.isSymbolicLink props)
   :size               (.size props)))

(defn properties
  "Obtain properties for the file represented by path, asynchronously.
   handler can either be a two-arity fn that will be passed the
   exception (if any) and properties (as a map) from the result of the
   call, or a Handler that will be called with the AsyncResult object
   that wraps the exception and FileProps object.

   The properties map passed to the handler fn will
   contain :creation-time, :last-access-time, :last-modified-time,
   :directory?, :regular-file?, :symbolic-link?, :other?, and :size.

   If the follow-link? is true (the default) and file is a link, the
   link will be followed."
  ([path handler]
     (properties path true handler))
  ([path follow-link? handler]
     (if follow-link?
       (.props (get-file-system) path
               (core/as-async-result-handler handler file-props->map))
       (.lprops (get-file-system) path
               (core/as-async-result-handler handler file-props->map)))))

(defn link
  "Creates a link on the file system from path to existing, asynchronously.
   If symbolic? is true (the default), the resulting link is symbolic,
   otherwise a had link is created. handler can either be a
   single-arity fn that will be passed the exception (if any) from the
   result of the call, or a Handler that will be called with the
   AsyncResult object that wraps the exception."
  ([path existing handler]
     (link path existing true handler))
  ([path existing symbolic? handler]
     (if symbolic?
       (.symlink (get-file-system) path existing
                 (core/as-async-result-handler handler false))
       (.link (get-file-system) path existing
              (core/as-async-result-handler handler false)))))

(defn resolve-symlink
  "Returns the path representing the file that the symbolic link specified by path points to, asynchronously.
   handler can either be a two-arity fn that will be passed the
   exception (if any) and the String path from the result of the call,
   or a Handler that will be called with the AsyncResult object that
   wraps the exception and the String path."
  [path handler]
  (.readSymlink (get-file-system) path
                (core/as-async-result-handler handler)))

(defn delete
  "Deletes the file on the file system represented by path, asynchronously.
   handler can either be a single-arity fn that will be passed the
   exception (if any) from the result of the call, or a Handler that
   will be called with the AsyncResult object that wraps the
   exception.

   If the recursive? is true (default is false) and file is a
   directory, it will be deleted recursively."
  ([path handler]
     (delete path false handler))
  ([path recursive? handler]
     (.delete (get-file-system) path (boolean recursive?)
              (core/as-async-result-handler handler false))))
  
(defn mkdir
  "Create the directory represented by path, asynchronously.
   If create-parents? is true (default is false), any non-existent
   parent directories of the directory will also be created.  If perms
   are provided, they will override the default permissions for the
   created directory.  The permission String takes the form rwxr-x---
   as specified by:
   http://download.oracle.com/javase/7/docs/api/java/nio/file/attribute/PosixFilePermissions.html
   handler can either be a single-arity fn that will be passed the
   exception (if any) from the result of the call, or a Handler that
   will be called with the AsyncResult object that wraps the
   exception.

   The operation will fail if the directory already exists."
  ([path handler]
     (mkdir path false nil handler))
  ([path create-parents? handler]
     (mkdir path create-parents? nil handler))
  ([path create-parents? perms handler]
     (.mkdir (get-file-system) path perms (boolean create-parents?)
             (core/as-async-result-handler handler false))))

(defn read-dir
  "Read the contents of the directory specified by path, asynchronously.
   If a filter regex is specified then only the paths that match it
   will be returned. handler can either be a two-arity fn that will be
   passed the exception (if any) and a vector of String paths from the
   result of the call, or a Handler that will be called with the
   AsyncResult object that wraps the exception and a String[] of
   paths."
  ([path handler]
     (read-dir path nil handler))
  ([path filter handler]
     (.readDir (get-file-system) path (if filter (str filter))
               (core/as-async-result-handler handler (partial into [])))))

(defn read-file
  "Reads the entire file as represented by the path path as a Buffer, asynchronously.
   handler can either be a two-arity fn that will be passed the
   exception (if any) and the Buffer from the result of the call, or a
   Handler that will be called with the AsyncResult object that wraps
   the exception and the buffer.

   Do not user this method to read very large files or you
   risk running out of available RAM."
  [path handler]
  (.readFile (get-file-system) path
             (core/as-async-result-handler handler)))

(defn write-file
  "Creates the file, and writes the specified data to the file represented by path, asynchronously.
   data can anything bufferable (see vertx.buffer).  handler
   can either be a single-arity fn that will be passed the
   exception (if any) from the result of the call, or a Handler that
   will be called with the AsyncResult object that wraps the
   exception."
  [path data handler]
  (.writeFile (get-file-system) path (buf/as-buffer data)
              (core/as-async-result-handler handler false)))

(defn open
  "Open the file represented by path, asynchronously.
   handler can either be a two-arity fn that will be passed the
   exception (if any) and the AsyncFile instance from the result of
   the call, or a Handler that will be called with the AsyncResult
   object that wraps the exception and the buffer.

   The behavior of the open call is further controlled by a set of
   kwarg arguments [default]:

   * :create? - create the file if it does not already exist [true]
   * :read?   - open the file for reading [true]
   * :write?  - open the file for writing [true]
   * :flush?  - the opened file will auto-flush writes [false]
   * :perms   - the permissions used to create the file, if necessary
                (see create-file) [nil]"
  [path handler & {:keys [perms read? write? create? flush?]
                   :or {read? true write? true create? true}}]
  (.open (get-file-system) path perms
         (boolean read?)
         (boolean write?)
         (boolean create?)
         (boolean flush?)
         (core/as-async-result-handler handler)))

(defn create-file
  "Creates an empty file with the specified path, asynchronously.
   If perms are provided, they will override the default permissions
   for the created file. The permission String takes the form
   rwxr-x--- as specified by:
   http://download.oracle.com/javase/7/docs/api/java/nio/file/attribute/PosixFilePermissions.html
   handler can either be a single-arity fn that will be passed the
   exception (if any) from the result of the call, or a Handler that
   will be called with the AsyncResult object that wraps the
   exception."
  ([path handler]
     (create-file path nil handler))  
  ([path perms handler]
       (.createFile (get-file-system) path perms
           (core/as-async-result-handler handler false))))

(defn exists?
  "Determines whether the file as specified by the path {@code path} exists, asynchronously.
   handler can either be a two-arity fn that will be passed the
   exception (if any) and the boolean result of the call, or a Handler
   that will be called with the AsyncResult object that wraps the
   exception and the boolean result."
  [path handler]
  (.exists (get-file-system) path
           (core/as-async-result-handler handler)))

(defn ^:internal ^:no-doc file-system-props->map [props]
  (hash-map
   :total-space       (.totalSpace props)
   :unallocated-space (.unallocatedSpace props)
   :usable-space      (.usableSpace props)))

(defn file-system-properties
  "Returns properties of the file-system being used by the specified path, asynchronously.
   handler can either be a two-arity fn that will be passed the
   exception (if any) and properties (as a map) from the result of the
   call, or a Handler that will be called with the AsyncResult object
   that wraps the exception and FileSystemProps object."
  [path handler]
  (.fsProps (get-file-system) path
            (core/as-async-result-handler handler file-system-props->map)))


(defn write
  "Write data to the file at position pos in the file, asynchronously.
   data can anything bufferable (see vertx.buffer).  If pos
   lies outside of the current size of the file, the file will be
   enlarged to encompass it. handler can either be a single-arity fn
   that will be passed the exception (if any) from the result of the
   call, or a Handler that will be called with the AsyncResult object
   that wraps the exception.  

   When multiple writes are invoked on the same file there are no
   guarantees as to order in which those writes actually occur."
   [file data pos handler]
  (.write file (buf/as-buffer data) pos
          (core/as-async-result-handler handler false)))

(defn read
  "Reads length bytes of data from the file at position pos in the file, asynchronously.
   If provided, the read data will be written into buffer! at offset.
   handler can either be a two-arity fn that will be passed the
   exception (if any) and the Buffer from the result of the
   call, or a Handler that will be called with the AsyncResult object
   that wraps the exception and Buffer object.

   If data is read past the end of the file then zero bytes will be
   read. When multiple reads are invoked on the same file there are no
   guarantees as to order in which those reads actually occur."
  ([file pos length handler]
     (read file (buf/buffer) 0 pos length handler))
  ([file buffer! offset pos length handler]
     (.read file buffer! offset pos length 
            (core/as-async-result-handler handler))))

(defn close
  "Close the file, asynchronously.
   handler can either be a single-arity fn that will be passed the
   exception (if any) from the result of the call, or a Handler that
   will be called with the AsyncResult object that wraps the
   exception."
  ([file]
     (close file nil))
  ([file handler]
     (common/internal-close file handler)))

(defn flush
  "Flush any writes made to this file to underlying persistent storage, asynchronously.
   handler can either be a single-arity fn that will be passed the
   exception (if any) from the result of the call, or a Handler that
   will be called with the AsyncResult object that wraps the
   exception.

   If the file was opened with :flush? true, then calling this method
   will have no effect."
  ([file]
     (.flush file))
  ([file handler]
     (.flush file
             (core/as-async-result-handler handler false))))
