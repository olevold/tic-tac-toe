(ns tictac.core
    (:require [reagent.core :as reagent :refer [atom]]
              [secretary.core :as secretary :include-macros true]
              [accountant.core :as accountant]))

(def victory-states
  [(+ 1 2 4)
   (+ 8 16 32)
   (+ 64 128 256)
   (+ 1 8 64)
   (+ 2 16 128)
   (+ 4 32 256)
   (+ 1 16 256)
   (+ 4 16 64)]
  )

(defn flip-bits[x digits]
    (bit-and (bit-not x) (- (.pow js/Math 2 digits) 1))
)

(defn cell-content [mask]
  (cond
    (= (bit-and @player-checked mask) mask) "X"
    (= (bit-and @computer-checked mask) mask) "O"
    :else "")
  )

(defn computer-move []
  (let [free (flip-bits (bit-or @player-checked @computer-checked) 9)]
    (loop [candidate-move (.pow js/Math 2 (rand-int 9))]
      (if (= (bit-and free candidate-move) candidate-move)
        (swap! computer-checked + candidate-move)
        (recur (.pow js/Math 2 (rand-int 9)))
      )
    )
  )
)

(defn update-result []
  (cond
    (= (bit-or @player-checked @computer-checked) 511) , (reset! result 4)

    :else (reset! result 0))
  )

(defn check-square [mask]
  (fn []
    (if (not= (bit-and (bit-or @player-checked @computer-checked) mask) mask)
      (do
        (swap! player-checked + mask)
        (update-result)
        (if (= @result 0)
          (do
            (reset! result 1)
            (computer-move)
            (update-result)
            )
          nil
          )
        )
      nil)
    (.log js/console (str @player-checked " " @computer-checked))
    )
  )

(def player-checked (atom 0))
(def computer-checked (atom 0))
(def result (atom 0))

;; ------------------------
;; Views
(defn game-board []
  [:table
    [:tbody
      [:tr
        [:td {:on-click (check-square 1)} (cell-content 1)]
        [:td {:on-click (check-square 2)} (cell-content 2)]
        [:td {:on-click (check-square 4)} (cell-content 4)]]
      [:tr
        [:td {:on-click (check-square 8)} (cell-content 8)]
        [:td {:on-click (check-square 16)} (cell-content 16)]
        [:td {:on-click (check-square 32)} (cell-content 32)]]
      [:tr
        [:td {:on-click (check-square 64)} (cell-content 64)]
        [:td {:on-click (check-square 128)} (cell-content 128)]
        [:td {:on-click (check-square 256)} (cell-content 256)]]
    ]
  ]
)

(defn home-page []
  [:div [:h2 "Welcome to tictac"]
   [:div [:a {:href "/about"} "go to about page"]]
   [:h1 (case @result
          0 "Your move"
          1 "Thinking..."
          2 "I win!"
          3 "You win!"
          4 "Draw...")]
   (game-board)
   ]
   )

(defn about-page []
  [:div [:h2 "About tictac"]
   [:div [:a {:href "/"} "go to the home page"]
         [:p "This tic tac toe implementation is supposed to learn from its mistakes!"]]])

;; -------------------------
;; Routes

(defonce page (atom #'home-page))

(defn current-page []
  [:div [@page]])

(secretary/defroute "/" []
  (reset! page #'home-page))

(secretary/defroute "/about" []
  (reset! page #'about-page))

;; -------------------------
;; Initialize app

(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (accountant/configure-navigation!
    {:nav-handler
     (fn [path]
       (secretary/dispatch! path))
     :path-exists?
     (fn [path]
       (secretary/locate-route path))})
  (accountant/dispatch-current!)
  (mount-root))
