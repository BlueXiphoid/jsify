(ns jsify.core
  (:require 
    [fs]
    [jsify.settings :as settings]
    [jsify.asset :as asset]
    [jsify.path :as path]
    [jsify.cache :as cache]
    [jsify.util :as util]
    [jsify.asset.javascript]
    [jsify.asset.manifest]
    [jsify.asset.static])
  (:use [ring.middleware.file      :only [wrap-file]]
        [ring.middleware.file-info :only [wrap-file-info]]
        [jsify.middleware.expires :only [wrap-file-expires-never]]
        [jsify.middleware.mime    :only [wrap-dieter-mime-types]]))

(def latest-update (atom nil))

(defn find-and-cache-asset [adrf]
  (when-let [file (path/find-asset adrf)]
    (-> file
        (asset/make-asset)
        (asset/read-asset)
        (#(if (settings/compress?)
            (asset/compress %)
            (:content %)))
        (cache/write-to-cache adrf))))

(defn root-folder-last-modified [& x] (-> (settings/cache-root) (java.io.File.) (.lastModified)))

(defn root-folder-changed? []
    (or 
      (nil? @latest-update)
      (not= @latest-update (root-folder-last-modified))))

(defn send-new-uri [app req uri]
  (prn "New uri: " uri)
  (app (assoc req :uri uri)))

(defn build-js [app req uri]
  (if (root-folder-changed?) ; No need to rebuild the javascript if nothing changed, just send the existing build
    (if-let [new-uri (-> uri path/uri->adrf find-and-cache-asset)]
      (let [new-uri (path/rewrite-uri new-uri)]
        (prn "Source changed, creating new build...")
        (swap! latest-update (fn [x] (root-folder-last-modified)))
        (send-new-uri app req new-uri))
      (app req))
    (send-new-uri app req 
      (-> 
        (clojure.string/replace uri #".jsify$" ".js")
        (path/uri->adrf)
        (path/find-lib-by-name)
        (path/rewrite-uri)))))

(defn jsify-builder [app & [options]]
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
  [app & [options]]
  (settings/with-options options
    (if-not (settings/production?)
      (-> app
          (wrap-file (settings/cache-root))
          (jsify-builder options)
          (wrap-file-info known-mime-types)
          (wrap-dieter-mime-types)
          (wrap-file-info known-mime-types)))))

(defn link-to-asset [lib & [options]]
  (settings/with-options options
    (if (settings/production?)
      (memoize (path/find-lib-by-name lib))
      (str lib))))

(defn jsify-init [options]
  (settings/with-options options
    (if (settings/precompile?)
      nil
      #_(find-and-cache-asset all-jsify-files-in-asset-root))))
