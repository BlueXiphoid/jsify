(ns jsify.settings)

(defonce ^:dynamic *settings*
  {:engine     :v8
   :compress   false
   :asset-root "resources/private/js"
   :cache-root "resources/public/js"
   :cache-mode :development
   :log-level  :normal
   :precompile false})

(defmacro with-options [options & body]
  `(binding [*settings* (merge *settings* ~options)]
     (do ~@body)))

(defn asset-root []
  (:asset-root *settings*))

(defn cache-root []
  (:cache-root *settings*))

(defn log-level []
  (:log-level *settings*))

(defn compress? []
  (-> *settings* :compress boolean))

(defn production? []
  (-> *settings* :cache-mode (= :development) not))

(defn precompile? []
  (-> *settings* :precompile boolean))