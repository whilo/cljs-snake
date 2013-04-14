(ns snake.core
  (:use [snake.repl :only [connect]]))

; This port uses HTML5 canvas and ClojureScript instead of
; the original version in Swing and Clojure. Adjust
; panel-canvas-id near the top to id of your canvas element.
;
; Original code from:
; from: http://java.ociweb.com/mark/programming/ClojureSnake.html
;
; This is a Swing-based game where the arrow keys to guide
; a snake to apples.  Each time the snake eats an apple it
; grows and a new apple appears in a random location.
; If the head of the snake hits its body, you lose.
; If the snake grows to a length of 10, you win.
; In either case the game starts over with a new, baby snake.
;
; This was originally written by Abhishek Reddy.
; Mark Volkmann rewrote it in an attempt to make it easier to understand.

; fire up a repl for the browser and eval namespace on top once connected
#_(do (ns snake.clojure.start)
      (require 'cljs.repl.browser)
      (cemerick.piggieback/cljs-repl
       :repl-env (doto (cljs.repl.browser/repl-env :port 9009)
                   cljs.repl/-setup)))



(defn ^:export say-hello []
  (js/alert "Hello from cljs-snake!"))

(def panel-canvas-id "snake")

(defrecord cell-record [x y])
(defrecord snake-record [body direction])
(defrecord game-record
    [panel cell-size length-to-win ms-per-move apple snake])

(defn board-dimensions [panel cell-size]
  (let [width (.-width (.-canvas (.getContext panel "2d")))
        height (.-height (.-canvas (.getContext panel "2d")))]
    [(quot width cell-size)
     (quot height cell-size)]))

(defn create-center-cell [width height]
  (cell-record. (quot width 2) (quot height 2)))

(defn create-random-cell [width height]
  (cell-record. (rand-int (- width 1)) (rand-int (- height 1))))

(defn create-snake [width height]
  (let [head (create-center-cell width height)
        body (list head)]
    (snake-record. body :right)))

(defn create-game [panel cell-size]
  (let [length-to-win 10
        ms-per-move 100
        [width height] (board-dimensions panel cell-size)
        apple (create-random-cell width height)
        snake (create-snake width height)]
    (game-record.
      panel cell-size length-to-win ms-per-move apple snake)))

(defn paint-cell [panel color cell-size {x :x y :y}]
  (let [context (.getContext panel "2d")]
    (set! (.-fillStyle context) (name color))
    (.fillRect context
               (* x cell-size) (* y cell-size) cell-size cell-size)))

(defn erase-cell [game {x :x y :y}]
  (let [panel (:panel game)
        context (.getContext panel "2d")
        cell-size (:cell-size game)]
    (.clearRect context
                (* x cell-size) (* y cell-size) cell-size cell-size)))

(defn erase-apple [game]
  (let [apple (:apple game)]
    (erase-cell game apple)))

(defn erase-snake [game]
  (doseq [cell (:body (:snake game))]
    (erase-cell game cell)))

(defn paint-apple [panel cell-size apple]
  (paint-cell panel :red cell-size apple))

(defn paint-snake [panel cell-size snake]
  ; We only need to paint the head because
  ; the rest will have been already painted.
  (let [head (first (:body snake))]
    (paint-cell panel :blue cell-size head)))

(defn paint-game [game]
  (let [panel (:panel game)
        cell-size (:cell-size game)]
  (paint-apple panel cell-size (:apple game))
  (paint-snake panel cell-size (:snake game))))

(defn new-apple [game]
  (let [panel (:panel game)
        cell-size (:cell-size game)
        [width height] (board-dimensions panel cell-size)]
    (erase-apple game)
    (create-random-cell width height)))

(defn delta
  "Gets a vector containing dx and dy values for a given direction."
  [direction]
  (direction {:left [-1 0], :right [1 0], :up [0 -1], :down [0 1]}))

(defn new-direction
  "Returns the snake's direction, either the current direction
   or a new one if a board edge was reached."
  [game]
  (let [snake (:snake game)
        direction (:direction snake)
        head (first (:body snake))
        x (:x head)
        y (:y head)
        panel (:panel game)
        cell-size (:cell-size game)
        [width height] (board-dimensions panel cell-size)
        at-left (== x 0)
        at-right (== x (- width 1))
        at-top (== y 0)
        at-bottom (== y (- height 1))
                                        ; Turn clockwise when a board edge is reached
                                        ; unless that would result in going off the board.
        (.log js/console "at position: " x)]
    (cond
      (and (= direction :up) at-top) (if at-right :left :right)
      (and (= direction :right) at-right) (if at-bottom :up :down)
      (and (= direction :down) at-bottom) (if at-left :right :left)
      (and (= direction :left) at-left) (if at-top :down :up)
      true direction)))

(defn same-or-adjacent-cell? [cell1 cell2]
  (let [dx (.abs js/Math (- (:x cell1) (:x cell2)))
        dy (.abs js/Math (- (:y cell1) (:y cell2)))]
    (and (<= dx 1) (<= dy 1))))

(defn eat-apple? [game]
  (let [apple (:apple game)
        snake (:snake game)
        head (first (:body snake))]
    (same-or-adjacent-cell? head apple)))

(defn remove-tail [game body]
  (let [tail (last body)]
    (erase-cell game tail)
    (butlast body)))

(defn move-snake [game grow]
  "Moves the snake and returns a new snake-record.
   The snake grows it by one cell if 'grow' is true."
  (let [direction (new-direction game)
        [dx dy] (delta direction)
        snake (:snake game)
        body (:body snake)
        head (first body)
        x (:x head)
        y (:y head)
        new-head (cell-record. (+ x dx) (+ y dy))
        body (cons new-head body)
        body (if grow body (remove-tail game body))]
    (snake-record. body direction)))

(defn get-key-direction
  "Gets a keyword that describes the direction
   associated with a given key code."
  [key-code]
  (cond
    (= key-code 37) :left
    (= key-code 39) :right
    (= key-code 38) :up
    (= key-code 40) :down
    true nil))

(defn snake-with-key-direction [snake key-code-atom]
  (let [key-code @key-code-atom
        key-direction (get-key-direction key-code)
        current (:direction snake)
        ; Don't let the snake double back on itself.
        valid-change (cond
          (= key-direction nil) false
          (= key-direction :left) (not= current :right)
          (= key-direction :right) (not= current :left)
          (= key-direction :up) (not= current :down)
          (= key-direction :down) (not= current :up)
          true true)]
    (if valid-change
      (do
        (compare-and-set! key-code-atom key-code nil)
        (assoc snake :direction key-direction))
      snake)))

(defn head-overlaps-body? [body]
  (let [head (first body)]
    (some #(= % head) (rest body))))

(defn restart-game [game]
  (erase-apple game)
  (erase-snake game)
  (create-game (:panel game) (:cell-size game)))

(defn new-game [game message]
  (let [panel (:panel game)]
    (js/alert message)
    (restart-game game)))

(defn win? [game]
  (let [snake (:snake game)
        body (:body snake)]
    (= (count body) (:length-to-win game))))

(defn lose? [game]
  (let [snake (:snake game)
        body (:body snake)]
    (head-overlaps-body? body)))

(defn step [game key-code-atom]
  (let [eat (eat-apple? game)
        snake (snake-with-key-direction (:snake game) key-code-atom)
        game (assoc game :snake snake)
        game (if eat (assoc game :apple (new-apple game)) game)
        snake (move-snake game eat)]
    (cond
      (lose? game) (new-game game "You killed the snake!")
      (win? game) (new-game game "You win!")
      :else (assoc game :snake snake))))

(defn create-panel [width height key-code-atom]
  (let [panel (.getElementById js/document panel-canvas-id)
        context (.getContext panel "2d")]
    (.addEventListener js/document "keyup"
           (fn [e]
             (compare-and-set! key-code-atom
                               @key-code-atom
                               (.-keyCode e))))
    panel)

  ; original Java/Swing code, TODO port to shared code
  #_(proxy [JPanel KeyListener]
    [] ; superclass conrecordor arguments
    (getPreferredSize [] (Dimension. width height))
    (keyPressed [e]
      (compare-and-set! key-code-atom @key-code-atom (.getKeyCode e)))
    (keyReleased [e]) ; do nothing
    (keyTyped [e]) ; do nothing
    ))

(defn configure-gui [panel]
  ; original Java/Swing code, TODO port to shared code
  #_(doto panel
      (.setFocusable true)      ; won't generate key events without this
      (.addKeyListener panel))
  #_(doto frame
      (.add panel)
      (.pack)
      (.setDefaultCloseOperation JFrame/DISPOSE_ON_CLOSE)
      (.setVisible true)))


(defn game-loop [game key-code-atom]
  (paint-game game)
  (js/setTimeout #(game-loop (step game key-code-atom) key-code-atom)
                 (:ms-per-move game)))


; was main entry point in Swing/Java version, TODO make portable
(let [width 20
      height 20
      cell-size 10
      key-code-atom (atom nil)
      panel-width (* width cell-size)
      panel-height (* height cell-size)
      panel (create-panel panel-width panel-height key-code-atom)
      first-game (create-game panel cell-size)]
  (configure-gui panel)
  (game-loop first-game key-code-atom))
