(ns tictac.handler
  (:require [compojure.core :refer [GET POST defroutes]]
            [compojure.route :refer [not-found resources]]
            [hiccup.page :refer [include-js include-css html5]]
            [tictac.middleware :refer [wrap-middleware]]
            [config.core :refer [env]]
            [duratom.core :refer [duratom]]
            [cheshire.core :refer [generate-string]]))

(try
  (defonce bad-moves (duratom :postgres-db :db-config (System/getenv "DATABASE_URL") :table-name "tictac_bad" :row-id 0 :init {}))
  (catch java.io.IOException e (defonce bad-moves (atom {})) (prn "caught IOException"))
  (catch java.sql.SQLException e (defonce bad-moves (atom {})) (prn "caught SQLException"))
  (catch java.lang.IllegalArgumentException e (defonce bad-moves (atom {})) (prn "caught IAException"))
  )

(try
  (defonce winning-moves (duratom :postgres-db :db-config (System/getenv "DATABASE_URL") :table-name "tictac_winning" :row-id 0 :init {}))
  (catch java.io.IOException e (defonce winning-moves (atom {})))
  (catch java.sql.SQLException e (defonce winning-moves (atom {})))
  (catch java.lang.IllegalArgumentException e (defonce winning-moves (atom {})) (prn "caught IAException"))
  )




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
        position1 (Integer/parseInt (get params "position1"))
        move1     (Integer/parseInt (get params "move1"))
        position2 (Integer/parseInt (get params "position2"))
        move2     (Integer/parseInt (get params "move2"))
        ]
        (swap! bad-moves assoc position1 (bit-or (or (get @bad-moves position1) 0) move1))
        (swap! bad-moves assoc position2 (bit-or (or (get @bad-moves position2) 0) move2))
    )
  )

(defn report-winning-move [req]
  (let [params (:multipart-params req)
        position1 (Integer/parseInt (get params "position1"))
        move1     (Integer/parseInt (get params "move1"))
        position2 (Integer/parseInt (get params "position2"))
        move2     (Integer/parseInt (get params "move2"))
        ]
        (swap! winning-moves assoc position1 move1)
        (swap! winning-moves assoc position2 move2)
    )
  )

(defn get-bad-moves []
  {:headers {"Content-type" "application/json"} :body (generate-string @bad-moves)}
  )

(defn get-winning-moves []
  {:headers {"Content-type" "application/json"} :body (generate-string @winning-moves)}
  )

(defroutes routes
  (GET "/" [] (loading-page))
  (GET "/about" [] (loading-page))
  (POST "/report-bad-move" req (report-bad-move req))
  (POST "/report-winning-move" req (report-winning-move req))
  (GET "/get-bad-moves" [] (get-bad-moves))
  (GET "/get-winning-moves" [] (get-winning-moves))
  (resources "/")
  (not-found "Not Found"))

(def app (wrap-middleware #'routes))
