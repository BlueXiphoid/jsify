(ns jsify.asset.static
  (:require [jsify.asset :as asset]))

(defrecord Static [file content]
  jsify.asset.Util
  (asset-last-modified [this]
    (.lastModified (:file this)))

  jsify.asset.Asset
  (read-asset [this]
    (assoc this :content
           (with-open [in (java.io.BufferedInputStream. (java.io.FileInputStream. (:file this)))]
             (let [buf (make-array Byte/TYPE (.length (:file this)))]
               (.read in buf)
               buf))))

  jsify.asset.Compressor
  (compress [this]
    (:content this)))

(asset/register :default map->Static)