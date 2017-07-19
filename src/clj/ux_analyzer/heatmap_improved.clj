(ns ux-analyzer.heatmap-improved
 (:require [mikera.image.core :as img-core]
           [mikera.image.colours :as img-colours]
           [mikera.image.spectrum :as img-spectrum]
           [mikera.image.filters :as img-filters]
           [mikera.image.protocols :as img-protocols]
           [clojure.java.io :refer [resource]]
  :use [mikera.image.protocols]))
          
(defn init-heat-map
  [width height]
  {:points {}
   :width width
   :height height
   :max-val 0})


(defn update-in-hm
  [hm [y x] fnc]
  (let [v (get-in hm [:points y x] 0)
        max-val (:max-val hm)]
   (-> (assoc-in hm [:points y x] (fnc v))
       (assoc :max-val (max max-val (fnc v))))))


(defn dist
  [x1 y1 x2 y2]
  (Math/sqrt (+ (* (- x1 x2)
                   (- x1 x2))
                (* (- y1 y2)
                   (- y1 y2)))))


(defn area
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


(defn populate-hm
  [hm coords radius]
  (->> coords
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

; (img-core/show (create-heatmap-img (generate-random-clicks 900 9000) 1440 900) :zoom 1)

(defn- blend
  [bg-c bg-a fg-c fg-a alpha]
  (+ (* fg-c (/ fg-a alpha))
     (* bg-c bg-a (/ (- 1 fg-a) alpha))))


(defn blend-colours
  [bg fg fg-a]
  (let [bg-a 1.0
        [bg-r bg-g bg-b] (img-colours/values-rgb bg)
        [fg-r fg-g fg-b] (img-colours/values-rgb fg)
        alpha (- 1 (* (- 1 fg-a)
                      (- 1 bg-a)))
        red (blend bg-r bg-a fg-r fg-a alpha)
        green (blend bg-g bg-a fg-g fg-a alpha)
        blue (blend bg-b bg-a fg-b fg-a alpha)]
   (img-colours/rgb red green blue)))


(defn blend-images
  [bg-img fg-img alpha]
  (let [blend-img (img-core/new-image (img-core/width bg-img)
                                      (img-core/height bg-img))]
   (doall (for [y (range (img-core/height blend-img))
                x (range (img-core/width blend-img))]
            (let [bg-px (img-core/get-pixel bg-img x y)
                  fg-px (img-core/get-pixel fg-img x y)
                  blend-px (blend-colours bg-px fg-px alpha)]
              (img-core/set-pixel blend-img x y blend-px))))
   blend-img))

; (let [bg-img (img-core/load-image-resource "rendered_pages/monkey.jpg")
;       fg-img (create-heatmap-img (generate-random-clicks (img-core/width bg-img)
;                                                          (img-core/height bg-img)
;                                                          1000)
;                                  (img-core/width bg-img)
;                                  (img-core/height bg-img))
;       blended-image (blend-images bg-img fg-img 0.7)]
;   (show blended-image :title "Blended image"))
;                                         


