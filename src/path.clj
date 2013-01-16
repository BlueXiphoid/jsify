(ns jsify.path
  (:require [jsify.settings :as settings]
            [clojure.java.io :as io]))

(defn file-name [file]
  (clojure.string/join "." (butlast (clojure.string/split (str file) #"\."))))

(defn file-ext [file]
  (last (clojure.string/split (str file) #"\.")))

(defn find-lib-by-name [lib]
  {:post [(not (nil? %))]}
  (.getName
    (first
    (filter #(re-matches (re-pattern (str "^(" (file-name lib)  ")-([\\da-f]{32})\\.(" (file-ext lib) ")$")) (.getName %)) 
      (-> 
        (settings/cache-root) 
        (java.io.File.) 
        (.listFiles) 
        (seq))))))

;;;; TODO
;;; so manifests need to call find-file from their own start-dir, not the
;;; asset-root. So this needs to be flexible to support both. However, it should
;;; never be splitting things apart, and "./" isn't neceessary (it should be
;;; handled in the manifest).


(defn uncachify-path [path]
  (if-let [[match fname hash ext] (re-matches #"^(.+)-([\da-f]{32})\.(\w+)$" path)]
    (str fname "." ext)
    path))

(defn make-relative-to-cache [path]
  (clojure.string/replace-first path (re-pattern (str ".*" (settings/cache-root))) ""))

(defn relative-path [root file]
  (let [absroot (fs/abspath root)
        absfile (fs/abspath file)
        root-length (count absroot)]
    (.substring absfile (inc root-length))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; String-types used
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;; Dieter uses many different types of paths, and it can be confusing at times.
;;; We try to use a sort-of hungarian notation, where every path has a type
;;; included in its variable name somehow.

;;; A "path" refers to paths of any kind, including filenames, uris, adrfs, etc.

;;; An "Asset-directory-relative filename" (adrf) represents a path, relative
;;; to the asset directory. It is used as a canonical representation, and can
;;; easily by created from URIs and filenames from directory traversals. It can
;;; also easily be converted into any of these types of file. It does not
;;; include the string "/assets/".

;;; A URI represents the part of the URI that we use in dieter. If a whole URI
;;; is prototcol://hostname/path, then we use URI to represent just the "path"
;;; portion. In dieter, all URIs will start with "/assets/", as otherwise they
;;; won't be handled by dieter.

;;; A filename represents an actual file on the filesystem. We'll try and keep
;;; them as absolute strings names, because relative ones are easy to confuse
;;; with other types.

(defn is-jsify-uri? [uri]
  (prn "is jsify?:" uri)
  (re-matches #".*\.jsify" uri))

(defn is-asset-uri? [uri]
  (re-matches #".*/assets/.*" uri))

(defn uri->adrf [uri]
  "Get name of lib from uri [a/b/c/d.jsify => d.jsify]"
  ;{:pre [(is-asset-uri? uri)]} ;; uris start with "/assets"
  ;(.substring uri 8))
  (first (re-find #"([^/]+$)" uri)))

(defn build-final-name [adrf md5]
  (str (file-name adrf) "-" md5 ".js"))

(defn adrf->uri [adrf]
  {:post [(is-asset-uri? %)]} ;; uris start with "/assets"
  (str "/assets/" adrf))

(defn adrf->filename [root adrf]
  (str root "/assets/" adrf))

(defn find-file [filename & {:keys [root]}]
  {:post [(or (nil? %) (.exists %))]}
  (let [file (io/file root filename)]
    (when (.exists file)
      file)))

(defn find-asset [adrf]
  {:post [(or (nil? %) (-> % io/file .exists))]}
  (find-file (str (settings/asset-root) "/" adrf)))

(defn rewrite-uri [new-uri]
   (str "/" new-uri))
