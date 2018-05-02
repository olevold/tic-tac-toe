(ns tictac.handler
  (:require [compojure.core :refer [GET POST defroutes]]
            [compojure.route :refer [not-found resources]]
            [hiccup.page :refer [include-js include-css html5]]
            [tictac.middleware :refer [wrap-middleware]]
            [config.core :refer [env]]))

(defonce bad-moves (atom {}))
(defonce winning-moves-moves (atom {}))

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

(defn loading-page []
  (html5
    (head)
    [:body {:class "body-container"}
     mount-target
     (include-js "/js/app.js")]))

(defn report-bad-move [req]
  (let [params (:multipart-params req)
        position (Integer/parseInt (get params "position"))
        move     (Integer/parseInt (get params "move"))
        ]
        (swap! bad-moves assoc position (bit-or (or (get @bad-moves position) 0) move))
    )
  )

(defroutes routes
  (GET "/" [] (loading-page))
  (GET "/about" [] (loading-page))
  (POST "/report-bad-move" req (report-bad-move req))

  (resources "/")
  (not-found "Not Found"))

(def app (wrap-middleware #'routes))
