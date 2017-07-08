(ns ux-analyzer.heat-map
  (:use [mikera.image.core]
        [mikera.image.colours]
        [mikera.image.spectrum]
        [clojure.java.shell :only [sh]])
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

(defn normalize-seq
 [seq]
 (let [min-v (apply min seq)
       max-v (apply max seq)]
   (map #(/ (- % min-v) (- max-v min-v)) seq)))

(defn render-website
  ([url] 
   (render-website url (str url ".png")))
  ([url out]
   (let [fout (str "resources/rendered_pages/" 
                   (-> out (clojure.string/replace #"https://" "")
                           (clojure.string/replace #"http://" "")
                           (clojure.string/replace #"/" ":")))]
    (sh "phantomjs" "src/js/render_website.js" url fout))))
        