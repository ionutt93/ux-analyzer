(ns ux-analyzer.db-core
  (:require
    [monger.core :as mongo]
    [monger.collection :as coll]
    [monger.conversion :refer [from-db-object]]
    [schema.core :as s])
  (:import [org.bson.types ObjectId]))

(def conn (mongo/connect {:host "127.0.0.1" :port 27017}))

(def db (mongo/get-db conn "ux-analyzer-db"))

(def Click-data-schema
  {:app-id s/Str
   :click {:x s/Int
           :y s/Int}
   :screen {:width s/Int
            :height s/Int}
   :timestamp s/Str
   :url s/Str})

(def models
  {:click-data {:name "click-data"
                :validator #(s/validate Click-data-schema %)}})

(defn insert
  [model-key data]

  (if-let [model (get-in models [model-key :name])]
    (let [validate (get-in models [model-key :validator])]
      (when (validate data)
        (coll/insert db model data)))
    [nil "Invalid model used"]))
