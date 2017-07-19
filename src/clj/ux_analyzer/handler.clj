(ns ux-analyzer.handler
  (:require [compojure.core :refer [GET POST defroutes]]
            [compojure.route :refer [not-found resources]]
            [hiccup.page :refer [include-js include-css html5]]
            [ux-analyzer.middleware :refer [wrap-middleware]]
            [config.core :refer [env]]
            [clj-json [core :as json]]
            [ux-analyzer.db-core :as db]
            [ux-analyzer.web-page-rendering :as web]
            [ux-analyzer.heatmap-improved :as hm]
            [clojure.java.io :as io]
            [ring.util.response :as response]
            [ring.util.mime-type :as mimes])
  (:import [org.bson.types ObjectId]))

(def mount-target
  [:div#app
      [:h3 "ClojureScript has not been compiled!"]
      [:p "please run "
       [:b "lein figwheel"]
       " in order to start the compiler"]])

(defn head []
  [:head
   [:meta {:charset "utf-8"}]
   [:meta {:name "viewport"
           :content "width=device-width, initial-scale=1"}]
   (include-css (if (env :dev) "/css/site.css" "/css/site.min.css"))])

(defn loading-page 
  []
  (html5
    (head)
    [:body {:class "body-container"}
     mount-target
     (include-js "/js/app.js")]))

(defn- matching-url
  [new-url app]
  (first (->> (:urls app)
              (filter #(clojure.string/starts-with? new-url (:url %))))))

(defn post-click-data
  "Saves click data if it matches an app and url"
  [app-id {params :params}]
  (let [app (db/get-by-id :app app-id)
        new-url (:url params)
        match (matching-url new-url app)]
    (prn match)
    (if-let [url-id (:_id match)]
      (do
       (db/insert :click
         {:app-id app-id
          :click (:click params)
          :screen (:screen params)
          :url-id url-id})
       {:status 200})
      {:status 500 :body "Could not find matching url registered"})))
                              
(defn get-click-data
  [app-id url-id]
  (db/retrieve :click
   {:app-id (ObjectId. app-id)
    :url-id (ObjectId. url-id)}))

(defn test-response [req]
  (print req))

(defn user-sign-up
  [username password]
  (db/insert :user {:username username
                    :password password}))
  
(defn register-url
  [app-id {params :params}]
  (let [url {:name (:name params)
             :url (:url params)}]
   (when (db/update :app app-id :push {:urls (db/wrap-entry url)})
     {:status 200})))

(defn first-matching-id
  [id array]
  (first (filter #(= (str (:_id %)) id) array)))

(defn get-rendered-page
  [app-id url-id]
  (let [app (db/get-by-id :app app-id)
        url-entry (first-matching-id url-id (:urls app))]
   (prn (web/render-page (:url url-entry) url-id))
   (-> (response/resource-response (web/out-path-web url-id))
       (response/content-type (mimes/default-mime-types web/default-ext-web)))))
    
(defn get-rendered-hm
  [app-id url-id]
  (let [app (db/get-by-id :app app-id)
        url (first-matching-id url-id (:urls app))
        coords (db/retrieve :click {:url-id (ObjectId. url-id)})]
   (prn (hm/render-hm coords url-id))
   (-> (response/resource-response (hm/out-path-hm url-id))
       (response/content-type (mimes/default-mime-types hm/default-ext-hm)))))

    
(defroutes routes
  (GET "/" [] (loading-page))
  (GET "/about" [] (loading-page))
  
  ; TODO Sign up as a user (:username :password) (low priority)
  ; (POST "users/" req "Not implemented yet")
  ; TODO Register a new app (:name :device-type) (low priority)
  ; (POST "users/:user-id/apps/" [user-id :as req] "Not implemented yet")
  (POST "/apps/:app-id/urls" [app-id :as req] (register-url app-id req))
  ; TODO Get all user content: apps, urls  (low priority)
  (GET "/users/:user-id/apps" [] "Not implemented yet")
  (GET "/apps/:app-id/urls/:url-id/click-data" [app-id url-id] (get-click-data app-id url-id))
  (POST "/apps/:app-id/click-data" [app-id :as req] (post-click-data app-id req))
  (GET "/apps/:app-id/urls/:url-id/rendered_page/" [app-id url-id] (get-rendered-page app-id url-id))
  ; TODO Get heatmap of website at specified url (optional: user can choose timespan)
  (GET "/apps/:app-id/urls/:url-id/heat_map/" [app-id url-id] (get-rendered-hm app-id url-id))
  
  (resources "/")
  (not-found "Not Found yet"))

(def app
  (wrap-middleware #'routes))

