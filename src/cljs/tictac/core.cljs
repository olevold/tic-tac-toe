(ns tictac.core
    (:require [reagent.core :as reagent :refer [atom]]
              [secretary.core :as secretary :include-macros true]
              [accountant.core :as accountant]
              [alandipert.storage-atom :refer [local-storage]]
              [ajax.core :refer [GET POST raw-response-format]]))

(declare game-board)

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

(defonce player-checked (atom 0))
(defonce computer-checked (atom 0))
(defonce result (atom 0))
(defonce winning-squares (atom 0))
(defonce state-before-last-computer-move (atom 0))
(defonce last-computer-move (atom 0))

(defonce bad-moves (local-storage (atom {}) :tictac-bad-moves))
(defonce winning-moves (local-storage  (atom {}) :tictac-winning-moves))

(GET "/get-bad-moves"
  {:handler #(reset! bad-moves (into {} (for [z %] [(js/parseInt (first z)) (second z)])))}
)

(GET "/get-winning-moves"
  {:handler #(reset! winning-moves (into {} (for [z %] [(js/parseInt (first z)) (second z)])))}
)

(defn mirror [x]
  (let [centre-ones (bit-and x 146)
        right-ones (bit-and x 292)
        left-ones (bit-and x 73)
        right-ones-shifted (bit-shift-right right-ones 2)
        left-ones-shifted (bit-shift-left left-ones 2)]
    (+ left-ones-shifted right-ones-shifted centre-ones))
  )

(defn flip-bits[x digits]
    (bit-and (bit-not x) (- (.pow js/Math 2 digits) 1))
)

(defn total-state []
  (+ (* 512 @player-checked) @computer-checked)
)

(defn mirrored-total-state [state]
  (let [player (quot state 512)
        computer (mod state 512)]
      (+ (* 512 (mirror player)) (mirror computer))
  )
)

(defn cell-content [mask]
  (cond
    (= (bit-and @player-checked mask) mask) [:svg {:class "svg-container"}
                                                  [:line {:x1 "5%" :x2 "95%" :y1 "5%" :y2 "95%"}]
                                                  [:line {:x1 "95%" :x2 "5%" :y1 "5%" :y2 "95%"}]]
    (= (bit-and @computer-checked mask) mask) [:svg {:class "svg-container"}
                                                  [:circle {:fill "none"}]]
    :else "")
  )

(defn cell-class [mask]
  (if (= (bit-and @winning-squares mask) mask)
    "winning"
    "normal"
  )
)

(defn i-lost []
  (swap! bad-moves assoc @state-before-last-computer-move (bit-or (get @bad-moves @state-before-last-computer-move) @last-computer-move))
  (swap! bad-moves assoc (mirrored-total-state @state-before-last-computer-move)
                          (bit-or  (get @bad-moves (mirrored-total-state @state-before-last-computer-move)) (mirror @last-computer-move)))
  (let [form-data (doto (js/FormData.)
                    (.set "position1" @state-before-last-computer-move)
                    (.set "move1" @last-computer-move)
                    (.set "position2" (mirrored-total-state @state-before-last-computer-move))
                    (.set "move2" (mirrored-total-state @last-computer-move)))]
    (POST "/report-bad-move" {:body form-data :format (raw-response-format)})
    )
  )

(defn player-won [move]
  (reset! result 3)
  (i-lost)
)

(defn i-won []
  (reset! result 2)
  (swap! winning-moves assoc @state-before-last-computer-move @last-computer-move)
  (let [form-data (doto (js/FormData.) (.set "position" @state-before-last-computer-move) (.set "move" @last-computer-move))]
    (POST "/report-winning-move" {:body form-data :format (raw-response-format)})
    )
)

(defn validate-move [move]
  (let [free (flip-bits (bit-or @player-checked @computer-checked) 9)]
    (and
      (= (bit-and free move) move)
      (or
        (when (= (get @bad-moves (total-state)) free)
          (.log js/console "I'm stuck!")
          (i-lost)
          true
        )
        (if (not (= (bit-and (get @bad-moves (total-state)) move) move))
          true
          (.log js/console (str "Skipped move " move))
        )
      )
    )
  )
)

(defn computer-move []
  (when (get @winning-moves (total-state)) (.log js/console "Aha!"))
  (loop [candidate-move (or (get @winning-moves (total-state)) (.pow js/Math 2 (rand-int 9)))]
    (if (validate-move candidate-move)
      (do
        (reset! state-before-last-computer-move (total-state))
        (swap! computer-checked + candidate-move)
        (reset! last-computer-move candidate-move)
        )
      (recur (.pow js/Math 2 (rand-int 9)))
    )
  )
)

(defn check-victory [state]
  (let [victory (some #(if (= (bit-and state %) %) % false) victory-states)]
    (when victory
      (reset! winning-squares victory)
      )
      victory
    )
  )

(defn update-result [move]
  (cond
    (check-victory @player-checked) , (player-won move)
    (check-victory @computer-checked) , (i-won)
    (= (bit-or @player-checked @computer-checked) 511) , (reset! result 4)
    :else (reset! result 0))
  )

(defn check-square [mask]
  (fn []
    (when (and (not= (bit-and (bit-or @player-checked @computer-checked) mask) mask) (= @result 0))
      (swap! player-checked + mask)
      (update-result mask)
      (when (= @result 0)
        (reset! result 1)
        (computer-move)
        (update-result mask)
      )
    )
  )
)

(defn start-over []
  (.log js/console "----------------------")
  (reset! player-checked 0)
  (reset! computer-checked 0)
  (reset! result 0)
  (reset! winning-squares 0)
  (reset! state-before-last-computer-move 0)
  (reset! last-computer-move 0)
  )

;; ------------------------
;; Views
(defn game-board []
  [:table
    [:tbody
      [:tr
        [:td {:on-click (check-square 1) :class (cell-class 1)} (cell-content 1)]
        [:td {:on-click (check-square 2) :class (cell-class 2)} (cell-content 2)]
        [:td {:on-click (check-square 4) :class (cell-class 4)} (cell-content 4)]]
      [:tr
        [:td {:on-click (check-square 8) :class (cell-class 8)} (cell-content 8)]
        [:td {:on-click (check-square 16) :class (cell-class 16)} (cell-content 16)]
        [:td {:on-click (check-square 32) :class (cell-class 32)} (cell-content 32)]]
      [:tr
        [:td {:on-click (check-square 64) :class (cell-class 64)} (cell-content 64)]
        [:td {:on-click (check-square 128) :class (cell-class 128)} (cell-content 128)]
        [:td {:on-click (check-square 256) :class (cell-class 256)} (cell-content 256)]]
    ]
  ]
)

(defn home-page []
  [:div [:h2 "Tic tac toe"]
   [:div [:a {:href "/about"} "go to about page"]]
   [:h3 (case @result
          0 "Your move"
          1 "Thinking..."
          2 "I win!"
          3 "You win!"
          4 "Draw...")]
   (game-board)
   [:a {:on-click start-over :href "#"} "Start over"]
   ]
   )

(defn about-page []
  [:div [:h2 "About tic tac toe"]
   [:div [:a {:href "/"} "go to the home page"]
         [:p "This tic tac toe implementation is supposed to learn from its mistakes."
          " State is maintained as two integers, one for each player. Each bit signifies a checked/unchecked square."]
          [:p "The game does not try to evaluate moves, it picks random moves that are valid (unchecked squares) and not blacklisted."
              "The game learns by blacklisting moves that lead to defeat. If all moves in a certain position "
              "lead to defeat, the move into that position is blacklisted. Winning moves are remembered and used rather than random moves."]
          [:p "Winning and losing moves will be reported to the backend and stored there. The game will try to fetch this data on page load. "
              "If the system variable DATABASE_URL is set to a valid postgres URL, those moves will be persisted in two tables."]]])

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
