(ns ux-analyzer.db-core
  (:require
    [monger.core :as mongo]
    [monger.collection :as coll]
    [monger.conversion :refer [from-db-object]]
    [monger.operators :refer :all]
    [monger.result :refer [acknowledged?]]
    [schema.core :as s]
    [proto-repl.saved-values :as pr])
  (:import [org.bson.types ObjectId]
           [java.util Date]))

(def conn (mongo/connect {:host "127.0.0.1" :port 27017}))

(def db (mongo/get-db conn "ux-analyzer-db"))

(def Click-schema
  {:app-id s/Any
   :click {:x s/Int
           :y s/Int}
   :screen {:width s/Int
            :height s/Int}
   :url-id s/Any})

(def User-schema
  {:username s/Str
   :password s/Str})

(def App-schema
  {:name s/Str
   :device-type s/Keyword
   :urls s/Any})

(def models
  {:click {:name "click-data"
                :validator #(s/validate Click-schema %)}
   :user {:name "user-data"
          :validator #(s/validate User-schema %)}
   :app {:name "app-data"
         :validator #(s/validate App-schema %)}})

(defn wrap-entry
  [data]
  (-> data 
      (assoc :_id (ObjectId.))
      (assoc :timestamp (Date.))))

(defn insert
  "Validates data and inserts into db"
  [model-key data]
  (if-let [model (get-in models [model-key :name])]
    (let [validate (get-in models [model-key :validator])]
      (validate data)
      (acknowledged? (coll/insert db model (wrap-entry data))))
    [nil "Invalid model used"]))

(defn get-by-id
  [model-key model-id]
  (if-let [model (get-in models [model-key :name])]
    (let [id (ObjectId. model-id)]
     (coll/find-one-as-map db model {:_id id}))
    [nil "Invalid model used"]))

(defn retrieve
  ([model-key] (retrieve model-key nil))
  ([model-key reqs]
   (if-let [model (get-in models [model-key :name])]
     (coll/find-maps db model reqs)
     [nil "Invalid model used"])))

(defn remove
  ([model-key] 
   (if-let [model (get-in models [model-key :name])]
    (coll/remove db model)))
  ([model-key reqs]
   (if-let [model (get-in models [model-key :name])]
    (coll/remove db model reqs)
    [nil "Invalid model used"])))

(defn wrap-operator
  [op data]
  (case op
    :push {$push data}
    :set {$set data}))

(defn update
 [model-key model-id operator payload]
 (if-let [model (get-in models [model-key :name])]
   (acknowledged? (coll/update-by-id db model 
                                     (ObjectId. model-id) 
                                     (wrap-operator operator payload)))
   [nil "Invalid model used"]))
 

; 
; (let [app (first (retrieve :app))
;       id (str (:_id app))]
;   (update :app id :push {:urls {:name "home"
;                                 :url "main/home"}}))
; 
; (insert :app {:name "test-app"
;               :device-type :desktop
;               :urls []})

; ; User schema
; {:id
;  :username "johna"
;  :password "pass123"}
;   
; ; App schema
; {:id
;  :user-id
;  :name "news"
;  :device-type :desktop|:mobile
;  :urls [{:id
;          :name "home"
;          :url "news/home"}
;         {:id
;          :name "about"
;          :url "news/about"}]}
; 
; ; Click data schema
; {:id
;  :app-id
;  :url-id
;  :page {:width 1440
;         :height 900}
;  :click {:x 20
;          :y 100}
;  :timestamp}

; TODO location of page render
; /resources/rendered_pages/app-id/url-id.png

; TODO location of page heatmaps
; /resource/heatmaps/app-id/url-id.png

