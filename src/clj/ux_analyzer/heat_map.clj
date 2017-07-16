(ns ux-analyzer.heat-map
  (:use [mikera.image.core]
        [mikera.image.colours]
        [mikera.image.spectrum]
        [clojure.java.shell :only [sh]])
  (:import [Math])
  (:require [clojure.core.reducers :as r]
            [clojure.pprint :as pp]))


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

(defn heat-map-vec
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
  "Renders an image of the url and saves it locally"
  ([url] 
   (render-website url (str url ".png")))
  ([url out]
   (let [fout (str "rendered_pages/" 
                   (-> out (clojure.string/replace #"https://" "")
                           (clojure.string/replace #"http://" "")
                           (clojure.string/replace #"/" ":")))]
     (prn "Rendered page location " fout)
     (sh "phantomjs" "src/js/render_website.js" url (str "resources/ "fout))
     fout)))

(defn render-heatmap
  "Renders a heatmap on top of the website rendering for clicks coming from specified url"
  [rendering-location clicks]
  (let [rendering (load-image-resource rendering-location)
        heatmap (heat-map-vec (width rendering) (height rendering) clicks 1)]
   [heatmap]))

(let [clicks [{:click {:x 102 :y 302}
               :screen {:width 500 :height 1000}}
              {:click {:y 302 :z 201}
               :screen {:width 500 :height 1000}}]]
 (render-heatmap "rendered_pages/atom.io:.png" clicks))

; NOTE x goes right as it increases
; NOTE y goes down as it increases

(defn gaussian
 [x k]
 (Math/exp (/ (- (* x x)) k)))

(defn dist
  [x1 y1 x2 y2]
  (Math/sqrt (+ (* (- x1 x2)
                   (- x1 x2))
                (* (- y1 y2)
                   (- y1 y2)))))

(defn mark-area
 [matrix [y x] dot-size]
 (let [coords (for [yy (range (- y dot-size)
                              (+ y dot-size))
                    xx (range (- x dot-size)
                              (+ x dot-size))
                    :when (and (>= xx 0) (>= yy 0) 
                               (< xx (count (first matrix)))
                               (< yy (count matrix)))]
                [yy xx])]
   (reduce (fn [m [yy xx]] 
             (update-in m [yy xx] inc))
           matrix
           coords)))
                    
(defn heatmap-quick
 [coords width height dot-size]
 (let [hm (init-vec-2d width height 0)]
  (reduce #(mark-area %1 %2 dot-size) hm coords)))

(defn generate-random-clicks
  [w count]
  (partition 2 (take count (repeatedly #(rand-int w)))))

(defn save-hm
  [hm]
  (let [w (count (first hm))
        h (count hm)
        hm-image (new-image w h)
        pixels (get-pixels hm-image)]
    (map-indexed (fn [i v] (aset pixels i (heatmap v))) 
                 (normalize-seq (flatten hm)))
    (set-pixels hm-image pixels)
    (clojure.java.io/make-parents (str (System/getProperty "user.dir")
                                       "/rendered-heatmaps/test.png"))
    (save hm-image 
          (str (System/getProperty "user.dir")
               "/rendered-heatmaps/test.png"))))
          
          

(-> (generate-random-clicks 100 20)
    (heatmap-quick 100 100 4)
    (save-hm))

