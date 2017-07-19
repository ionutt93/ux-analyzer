(ns ux-analyzer.web-page-rendering
 (:require [clojure.java.shell :refer [sh]]))

(def default-ext-web "png")

(defn out-path-web
  ([url-id] (out-path-web url-id default-ext-web))
  ([url-id ext] (str "rendered_pages/" url-id "." ext)))

(defn render-page
  "Renders an image of the url and saves it locally"
  ([url url-id] 
   (render-page url url-id default-ext-web))
  ([url url-id ext]
   (let [out (str "resources/" (out-path-web url-id ext))]
     (sh "phantomjs" "src/js/render_website.js" url out))))
