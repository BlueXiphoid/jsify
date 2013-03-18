(ns leiningen.preify
	(:require [jsify.core :as jsify]))

(defn preify
	"Precompile JS"
  [project & args]
  (prn project)
	(let [[file engine mode compress cache-root asset-root] args] (jsify/pre-build file {
		:engine (keyword engine)
		:mode (keyword mode)
		:compress (keyword compress)
		:cache-root cache-root
		:asset-root asset-root
		:log-level :quiet
		})))

; Args 
; engine, mode, compress, cache-root, asset-root