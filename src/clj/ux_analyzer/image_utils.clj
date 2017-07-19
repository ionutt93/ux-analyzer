(ns ux-analyzer.image-utils
 (:require [mikera.image.core :as img-core]
           [mikera.image.colours :as img-colours]
           [mikera.image.spectrum :as img-spectrum]
           [mikera.image.filters :as img-filters]
           [mikera.image.protocols :as img-protocols]))

(defn- blend
  [bg-c bg-a fg-c fg-a alpha]
  (+ (* fg-c (/ fg-a alpha))
     (* bg-c bg-a (/ (- 1 fg-a) alpha))))


(defn- blend-colours
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
; ;                                         

