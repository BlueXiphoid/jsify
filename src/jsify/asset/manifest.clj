(ns jsify.asset.manifest
  (:require [clojure.java.io :as io]
            [clojure.string :as s]
            [jsify.asset :as asset]
            [jsify.path :as path]
            [fs.core :as fs])
  (:use [jsify.util :only [slurp-into string-builder]])
  (:import [java.io File FileReader PushbackReader FileNotFoundException]))

(def *separator* File/separator)

(defn join
"Join parts of path.\n\t(join [\"a\" \"b\"]) -> \"a/b\"."
[& parts]
  (apply str (interpose *separator* parts)))

(defn load-manifest
  "a manifest file must be a valid clojure data structure,
namely a vector or list of file names or directory paths."
  [file]
  (let [stream (PushbackReader. (FileReader. file))]
    (read stream)))

(defn recursive-files [dir]
  (->> dir
       fs/iterate-dir
       (map (fn [[root _ files]]
              (doall (map #(join root %)
                          (sort files))))))) ;; sort because of file-ordering bugs

(defn manifest-files
  "return a sequence of files specified by the given manifest."
  [manifest-file]
  (->> (load-manifest manifest-file)
       (map (fn [filename]
              (let [dir (.getParent manifest-file)
                    file (path/find-file filename :root dir)]
                (when (nil? file)
                  (throw (FileNotFoundException. (str "Could not find " filename " from " manifest-file))))
                (if (-> file io/file .isDirectory)
                  (recursive-files file)
                  file))))
       doall
       flatten
       (map io/file)
       (remove #(or (re-matches #".*\.swp$" (.getCanonicalPath %))
                    (re-matches #"/.*\.#.*$" (.getCanonicalPath %))))))

(defn compile-manifest [file]
  (let [builder (string-builder)
        target-name (s/replace file #".jsify$" ".js")
        result (asset/make-asset (io/file target-name))]
    (doseq [mf (manifest-files file)]
      (->> mf
           asset/make-asset
           asset/read-asset
           :content
           (.append builder)))
    (assoc result :content builder)))

(defn manifest-last-modified [file]
  (apply max (map #(.lastModified %) (conj (manifest-files file) file))))

(defrecord JSify [file]
  jsify.asset.Util
    (asset-last-modified [this] (manifest-last-modified (:file this)))

  jsify.asset.Asset
    (read-asset [this] (compile-manifest (:file this))))

(asset/register "jsify" map->JSify)