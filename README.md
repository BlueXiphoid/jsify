JSify
=====

JS merge and minifier

## Installation

Add the following dependency to your `project.clj` file:

    [jsify "0.1.4"]

## Example

```clojure
(require '[jsify :as jsify])

(def jsify-settings {
      :engine       :v8
      :cache-mode   :development
      :compress     false
      :cache-root   "resources/public/js/" 
      :asset-roots  "resources/private/js/"
      :log-level    :quiet
    })
```
 
 For noir    
 
```clojure
(noir/add-middleware jsify/jsify-middleware jsify-settings)
```
      
For ring

```clojure
(-> app (jsify/jsify-middleware jsify-settings))
```

To get the link to the output simply use:

```clojure
(jsify/link-to-asset "app.jsify" jsify-settings)
```
    
resources/private/js/app.jsify will look like this:

```clojure
[
    "plugin.jquery.js"
    "init.js"
    "assets/"
    "more.js"
]
``` 

JSify will only rebuild the javascript when files are changed.
