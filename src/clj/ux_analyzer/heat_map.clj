(ns ux-analyzer.heat-map
  (:use [mikera.image.core]
        [mikera.image.colours])
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
  (for  [ny (range (- y 1) (+ y 2))
         nx (range (- x 1) (+ x 2))
         :when (and (not (and (= nx x) (= ny y))))]
      [nx ny]))

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
;   
; (defn improved-round
;   [hm all-coords]
;   (r/fold (fn [acc [x y neighbours]]
;             (let [n (count-pos-coords neighbours hm)]
;               (update-in acc [x y] #(if (= % 0) (+ % n) (+ % n 1)))))
;           hm
;           all-coords))
;           
          
(defn improved-round
  [hm all-coords]
  (reduce (fn [acc [x y neighbours]]
            (let [n (count-pos-coords neighbours hm)]
              (update-in acc [x y] #(if (= % 0) (+ % n) (+ % n 1)))))
          hm
          all-coords))


(defn improved-head-map
  [width height coords rounds]
  (let [all-coords (all-coords-with-neighbours width height)]
    (loop [hm (init-positions (init-vec-2d width height 0) coords)
           r rounds]
      (if (= r 0) 
        hm
        (recur (improved-round hm all-coords) (dec r)))))
  "done")

(def coords
  [[1 1]
   [3 3]
   [8 8]])
; 
; (defn normalize-hm
;   [img]
;   (let [min-v (min img)
;         max-v (max img)]
;     (map #(/ (- % min-v) (- max-v min-v)) img)))
;     
; (def hm-img (new-image 200 200))
; 
; (def hm-pixels (get-pixels hm-img))
; 
; (for [i (range 0 (count hm-pixels))]
;   (aset hm-pixels i (rgb (nth normalized-img i) 0 0)))
; 
; (set-pixels hm-img hm-pixels)
; 
; (show hm-img :zoom 2.0 :title "Isn't it beautiful?")


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


