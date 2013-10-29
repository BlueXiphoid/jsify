(defproject jsify "0.1.4"
  :description "JS merge and minifying middleware for clojure. Based on dieter."
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :url "http://github.com/bluexiphoid/jsify"
  :dependencies [[ring "1.1.8"]
                 [fs "0.11.1"]
                 [clj-time "0.4.4"]
                 [com.google.javascript/closure-compiler "v20131014"]
                 [clj-v8 "0.1.4"]
                 [clj-v8-native "0.1.4"]
                 [org.mozilla/rhino "1.7R4"]]
  :dev-dependencies [[org.clojure/clojure "1.4.0"]])
