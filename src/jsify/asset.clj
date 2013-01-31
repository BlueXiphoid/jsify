(ns jsify.asset
  (:require
    [jsify.path :as path]))

(defprotocol Asset
  (read-asset [this]))

(defprotocol Compressor
  (compress [this]))

(defprotocol Util
  (asset-last-modified [this]))

(def types (atom {}))
(defn register [ext constructor-fn] (swap! types assoc ext constructor-fn))
(defn make-asset [file] ((get @types (path/file-ext file) (:default @types)) {:file file}))