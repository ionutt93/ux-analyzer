(ns ux-analyzer.tracker-script
  (:require [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]))

(def app-id 1)

(def coordinates-url
  (str "save_coordinates/" app-id "/"))

(defn add-click-listener [handler]
  (.addEventListener js/document "click" handler))

(defn build-click-payload
  [click-event]
  {:click {:x (.-screenX click-event)
           :y (.-screenY click-event)}
   :screen {:width (.-innerWidth js/window)
            :height (.-innerHeight js/window)}
   :timestamp (.toISOString (js/Date.))
   :url (.-location.href js/window)})

(add-click-listener
  (fn [event]
    (js/console.log event)
    (http/post coordinates-url
               {:json-params (build-click-payload event)})))
