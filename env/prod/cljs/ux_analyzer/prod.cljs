(ns ux-analyzer.prod
  (:require [ux-analyzer.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
