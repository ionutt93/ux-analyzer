(ns ux-analyzer.heatmap-improved
 (:require [mikera.image.core :as img-core]
           [mikera.image.colours :as img-colours]
           [mikera.image.spectrum :as img-spectrum]
           [mikera.image.filters :as img-filters]
           [mikera.image.protocols :as img-protocols]
           [clojure.java.io :refer [resource]]
           [ux-analyzer.web-page-rendering :as web]
           [ux-analyzer.image-utils :as img-utils]
  :use [mikera.image.protocols]))

(def default-ext-hm "png")

(defn- init-heat-map
  [width height]
  {:points {}
   :width width
   :height height
   :max-val 0})


(defn- update-in-hm
  [hm [y x] fnc]
  (let [v (get-in hm [:points y x] 0)
        max-val (:max-val hm)]
   (-> (assoc-in hm [:points y x] (fnc v))
       (assoc :max-val (max max-val (fnc v))))))


(defn- dist
  [x1 y1 x2 y2]
  (Math/sqrt (+ (* (- x1 x2)
                   (- x1 x2))
                (* (- y1 y2)
                   (- y1 y2)))))


(defn- area
  [[cy cx] radius width height]
  (let [min-y (max (- cy radius) 0)
        min-x (max (- cx radius) 0)
        max-y (min (+ cy radius 1) height)
        max-x (min (+ cx radius 1) width)]
   (for [y (range min-y max-y)                  
         x (range min-x max-x) 
         :when (<= (dist y x cy cx) radius)]
    [y x])))


(defn- generate-random-clicks
  [width height count]
  (take count (repeatedly #(let [x (rand-int width) 
                                 y (rand-int height)]
                             [y x]))))


(defn- normalize
  [v max]
  (/ v max))

(defn preprocess-coords
  [coords]
  (map #(let [y (get-in % [:click :y])
              x (get-in % [:click :x])]
          [y x])
       coords))

(defn populate-hm
  [hm coords radius]
  (->> coords
       (preprocess-coords)
       (map #(area % radius (:width hm) (:height hm)))
       (reduce into)
       (reduce #(update-in-hm %1 %2 inc) hm)))

(defn populate-img
  [hm]
  (let [hm-img (img-core/new-image (:width hm) (:height hm))]
   (doall (for [y (range (:height hm))
                x (range (:width hm))
                :let [v (get-in hm [:points y x] 0)]]
            (img-core/set-pixel hm-img x y
                                 (img-spectrum/heatmap (normalize v (:max-val hm))))))
   (img-core/filter-image hm-img (img-filters/box-blur 3 3 :iterations 5))))


(defn create-heatmap-img
  [coords width height]
  (-> (init-heat-map width height)
      (populate-hm coords 25)
      (populate-img)))


(defn out-path-hm
  ([url-id] (out-path-hm url-id default-ext-hm))
  ([url-id ext] (str "rendered_heatmaps/" url-id "." ext)))

(defn render-hm
  ([coords url-id]
   (let [web-out (web/out-path-web url-id)
         page-img (img-core/load-image-resource web-out)]
    (if (not (nil? page-img))
      (render-hm coords url-id page-img)
      [nil "Did not find web page"])))
  ([coords url-id page-img]
   (let [hm-img (create-heatmap-img coords 
                                    (img-core/width page-img) 
                                    (img-core/height page-img))
         blend-img (img-utils/blend-images page-img hm-img 0.3)]
     (img-core/save blend-img (str "resources/" (out-path-hm url-id))))))
; (img-core/show (create-heatmap-img (generate-random-clicks 900 9000) 1440 900) :zoom 1)
