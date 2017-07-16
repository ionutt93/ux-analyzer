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

(defn valid?
  [[y x] height width]
  (and (>= y 0) (>= x 0)
       (< y height) (< x width)))

(defn area
  [[cy cx] radius]
  (for [y (range (- cy radius)
                 (+ cy radius 1))
        x (range (- cx radius)
                 (+ cx radius 1))
        :when (<= (dist y x cy cx) radius)]
   [y x]))


(defn generate-random-clicks
  [w count]
  (partition 2 (take count (repeatedly #(rand-int w)))))


(def coords (generate-random-clicks 100 500))
(def hm (init-heat-map 100 100))
(def complete-hm (->> coords
                      (map #(area % 3))
                      (reduce into)
                      (filter #(valid? % 100 100))
                      (reduce #(update-in-hm %1 %2 inc) hm)))

(defn normalize
  [v max]
  (/ v max))


(def hm-img (img-core/new-image 100 100))
(for [y (range 100)
      x (range 100)
      :let [v (get-in complete-hm [:points y x] 0)]]
  (img-core/set-pixel hm-img y x
                       (img-spectrum/heatmap (normalize v (:max-val complete-hm)))))

(img-core/show (img-core/filter-image hm-img (img-filters/blur)) :zoom 5)

