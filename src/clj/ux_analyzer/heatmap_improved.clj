(ns ux-analyzer.heatmap-improved
 (:require [mikera.image.core :as img-core]
           [mikera.image.colours :as img-colours]
           [mikera.image.spectrum :as img-spectrum]
           [mikera.image.filters :as img-filters]))


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
  [w count]
  (partition 2 (take count (repeatedly #(rand-int w)))))


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
      (populate-hm coords 10)
      (populate-img)))

; (img-core/show (create-heatmap-img (generate-random-clicks 900 9000) 1440 900) :zoom 1)
