(ns jsify.core
  (:require
    [jsify.settings   :as settings]
    [jsify.asset      :as asset]
    [jsify.path       :as path]
    [jsify.cache      :as cache]
    [jsify.util       :as util]
    [jsify.asset.javascript]
    [jsify.asset.manifest]
    [jsify.asset.static])
  (:use [ring.middleware.file      :only [wrap-file]]
        [ring.middleware.file-info :only [wrap-file-info]]
        [ring.util.response]))

(defonce latest-update (atom nil))
(defonce latest-build (atom nil))

(defn find-and-cache-asset [adrf]
  "Finds the asset and creates the file."
  (when-let [file (path/find-asset adrf)]
    (if (settings/remove-old-version?)
      (cache/remove-old-version file adrf))
    (-> file
        (asset/make-asset)
        (asset/read-asset)
        (#(if (settings/compress?)
            (asset/compress %)
            (:content %)))
        (cache/write-to-cache adrf))))

(defn root-folder-last-modified [& [uri y]]
  "Gets the last-modified date of the latest updated file used by this asset."
  (asset/asset-last-modified (asset/make-asset (path/find-asset (path/uri->adrf uri)))))

(defn root-folder-changed? [uri]
  "Checks if the root folder has changed."
    (or
      (nil? @latest-update)
      (not= @latest-update (root-folder-last-modified uri))))

(defn send-new-uri [app req uri]
  "Will update the uri according to the real file's name."
  (file-response uri {:root (settings/cache-root)}))

(defn build-js [app req uri]
  "Builds the js. Or if nothing changed will return the current js file."
  (if (root-folder-changed? uri)
    (if-let [new-uri (-> uri path/uri->adrf find-and-cache-asset)]
      (do
        (swap! latest-build assoc (keyword (path/uri->adrf uri)) new-uri)
        (swap! latest-update #(root-folder-last-modified uri %))
        (send-new-uri app req new-uri))
      (app req))
    (send-new-uri app req (get @latest-build (keyword (path/uri->adrf uri))))))

(defn jsify-builder [app & [options]]
  "Delegates the jsify middleware"
  (fn [req]
    (settings/with-options options
      (let [uri (-> req :uri)]
        (if (and
              (path/is-jsify-uri? uri)
              (not (settings/production?)))
          (build-js app req uri)
        (app req))))))

(def known-mime-types {:hbs "text/javascript"})

(defn jsify-middleware
  "The actual middleware"
  [app & [options]]
  (settings/with-options options
    (if-not (settings/production?)
      (-> app
          (wrap-file (settings/cache-root))
          (jsify-builder options)
          (wrap-file-info known-mime-types)))))

(defn get-latest-cache [lib]
  (path/find-lib-by-name (clojure.string/replace lib #".jsify" ".js")))

(def latest-cache (memoize get-latest-cache))

(defn link-to-asset [lib & [options]]
  "Returns a path to the middleware"
  (settings/with-options options
    (if (settings/production?)
      (latest-cache lib)
      lib)))

(defn pre-build [lib options]
  (settings/with-options options
    (find-and-cache-asset (-> lib path/uri->adrf find-and-cache-asset))))
