(ns jsify.cache
  (:require 
            [jsify.settings :as settings]
            [jsify.path :as path]
            [clojure.java.io :as io])
  (:import [java.security MessageDigest]))

(derive (class (make-array Byte/TYPE 0)) ::bytes)
(derive java.lang.String ::string-like)
(derive java.lang.StringBuilder ::string-like)

(defmulti write-file (fn [c f] (class c)))
(defmethod write-file ::string-like [content file]
  (spit file content))

(defmethod write-file ::bytes [content file]
  (with-open [out (java.io.FileOutputStream. file)]
    (.write out content)))

(defmulti md5 class)
(defmethod md5 ::bytes [bytes]
  (let [digest (.digest (MessageDigest/getInstance "MD5") bytes)]
    (format "%032x" (BigInteger. 1 digest))))

(defmethod md5 ::string-like [string]
  (md5 (.getBytes (str string) "UTF-8")))

(defn add-md5 [path content]
  (if-let [[match fname ext] (re-matches #"^(.+)\.(\w+)$" path)]
    (str fname "-" (md5 content) "." ext)
    (str path "-" (md5 content))))

(defn write-to-cache [content adrf]
  (let [
    final-name (path/build-final-name adrf (md5 content))
    dest (io/file (str (settings/cache-root) final-name))]
    (io/make-parents dest)
    (write-file content dest)
    final-name))

(defn remove-old-version [file filename]
  (while
    (let [lib-path (path/find-lib-by-name (clojure.string/replace filename #".jsify" ".js"))]
      (if-not (nil? lib-path)
        (do
          (prn "Deleting old version of " filename ": " lib-path)
          (io/delete-file (str (settings/cache-root) lib-path))))
      (not (nil? lib-path))
      )))
