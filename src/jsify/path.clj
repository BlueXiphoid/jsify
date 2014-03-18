(ns jsify.path
  (:require [jsify.settings :as settings]
            [clojure.java.io :as io]
            [clojure.string :as cljstr]))

(defn file-name [file]
  (cljstr/join "." (butlast (cljstr/split (str file) #"\."))))

(defn file-ext [file]
  (last (cljstr/split (str file) #"\.")))

(defn find-lib-by-name [lib]
    (let [result (filter #(re-matches (re-pattern (str "^(" (file-name lib)  ")-([\\da-f]{32})\\.(" (file-ext lib) ")$")) (.getName %)) 
      (-> 
        (settings/cache-root) 
        (java.io.File.) 
        (.listFiles) 
        (seq)))]
    (if-not (empty? result)
      (.getName (first result))
      nil)))

(defn uncachify-path [path]
  (if-let [[match fname hash ext] (re-matches #"^(.+)-([\da-f]{32})\.(\w+)$" path)]
    (str fname "." ext)
    path))

(defn make-relative-to-cache [path]
  (clojure.string/replace-first path (re-pattern (str ".*" (settings/cache-root))) ""))

(defn is-jsify-uri? [uri]
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
  (find-file adrf :root (settings/asset-root)))

(defn rewrite-uri [new-uri]
   (str "/" new-uri))
