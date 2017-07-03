(ns ux-analyzer.heat-map
  (:use [mikera.image.core]
        [mikera.image.colours]
        [mikera.image.spectrum])
  (:import [Math])
  (:require [clojure.core.reducers :as r]))

(defn init-vec-2d
  "Creates a 2d vector with given size, populated with give value"
  [width height val]
  (vec (repeat height (vec (repeat width val)))))

(defn init-positions
  [vect coords]
  (reduce (fn [v [x y]] (assoc-in v [x y] 1))
          vect
          coords))

(defn get-neighbour-coords
  [x y width height]
  [[(- x 1) y]
   [(+ x 1) y]
   [x (- y 1)]
   [x (+ y 1)]])

(defn- all-coords-with-neighbours
  [width height]
  (for [x (range 0 height)
        y (range 0 width)]
    [x y (get-neighbour-coords x y width height)]))

(defn count-pos-coords
  [coords hm]
  (reduce (fn [count [x y]]
            (let [v (get-in hm [x y] 0)]
              (if (> v 0) (inc count) count)))
          0 coords))                              

(defn round
  [hm all-coords]
  (reduce (fn [acc [x y neighbours]]
            (let [n (count-pos-coords neighbours hm)]
              (update-in acc [x y] #(if (= % 0) (+ % n) (+ % n 1)))))
          hm
          all-coords))


(defn head-map-vec
  [width height coords rounds]
  (let [all-coords (all-coords-with-neighbours width height)]
    (loop [hm (init-positions (init-vec-2d width height 0) coords)
           r rounds]
      (if (= r 0) 
        hm
        (recur (round hm all-coords) (dec r))))))

(def coords
  [[1 1]
   [3 3]
   [8 8]
   [30 50]
   [35 45]
   [30 55]
   [30 50]
   [20 70]])

(defn normalize-seq
 [seq]
 (let [min-v (apply min seq)
       max-v (apply max seq)]
   (map #(/ (- % min-v) (- max-v min-v)) seq)))

(def rand-coords (partition 2 (take 500 (repeatedly #(rand-int 100)))))

(def my-heat-map-vec (head-map-vec 100 100 rand-coords 1))

(def my-flat-heat-map-vec (flatten my-heat-map-vec))

(def normalized-hm-vec (normalize-seq my-flat-heat-map-vec))

(def hm-img (new-image 100 100))
 
(def hm-pixels (get-pixels hm-img))
 
(for [i (range 0 (count normalized-hm-vec))]
  (aset hm-pixels i (heatmap (nth normalized-hm-vec i))))
 
(set-pixels hm-img hm-pixels)
 
(show hm-img :zoom 3.0 :title "Isn't it beautiful?")

; (defn test-hm-creation
;   [width height coords rounds]
;   (def hm-img (new-image width height))
;   (def pixels (get-pixels hm-img))
;   (let [hm (my-heat-map width height coords rounds)
;         normalized-hm (normalize-hm hm)]    
;     (for [i (range 0 (* width height))]
;       (aset pixels i (rgb 0 0 0)))
;     (set-pixels hm-img pixels)
;     hm-img))
;   
; (show (test-hm-creation 10 10 coords 1) :zoom 2.0 :title "HM")


