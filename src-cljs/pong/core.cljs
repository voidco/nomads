(ns pong.core
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [dommy.macros :refer [sel1]])
  (:require [big-bang.core :refer [big-bang!]]
            [big-bang.events.browser :refer [client-coords]]
            [monet.canvas :as canvas]
            [dommy.core :as dommy]))

(enable-console-print!)

;; Globals ----------------------------------------------------------
(def canvas-size {:w 646 :h 400})

(def paddle-offset 12)

(def paddle-size {:w (* 0.05 (:w canvas-size))
                  :h (* 0.3  (:h canvas-size))})

(def ball-size (* 0.04 (:w canvas-size)))

(def init-paddle-y
  "Start the paddles centered vertically."
  (let [scr-half-y    (/ (:h canvas-size) 2)
        paddle-half-y (/ (:h paddle-size) 2)]
    (- scr-half-y paddle-half-y)))

(def colours {:bg     "#699D32"
              :paddle "#2A7E77"
              :ball   "#281B50"})
;; ==================================================================

;; Helper fns -------------------------------------------------------
(defn init-canvas
  "Sets the canvas size and returns a 2D context."
  [canvas {:keys [w h]}]
    (-> canvas
        (dommy/set-attr! :width w :height h)
        (canvas/get-context "2d")))

(defn draw-rect [colour ctx dim]
  (-> ctx
      (canvas/fill-style colour)
      (canvas/fill-rect dim)))

(defn create-paddle [update-fn [x y]]
  (canvas/entity {:x x
                  :y y
                  :w (:w paddle-size)
                  :h (:h paddle-size)}
                 update-fn
                 (partial draw-rect (:paddle colours))))

(defn center-ball [{:keys [h w] :as ball} {canvas-w :w canvas-h :h}]
  (-> ball
      (assoc :x (- (/ canvas-w 2) (/ w 2)))
      (assoc :y (- (/ canvas-h 2) (/ h 2)))))

(defn client->canvas-coords
  "Transforms mouse coords from client space to canvas space.
  Does not perform bounds checking."
  [canvas [x y]]
  (let [rect (.getBoundingClientRect canvas)
        left (.-left rect)
        top  (.-top rect)]
    [(- x left) (- y top)]))

(defn clamp [x min-val max-val]
  (max min-val (min max-val x)))
;; ==================================================================

;; Entities ---------------------------------------------------------
(def background
  (canvas/entity {:x 0
                  :y 0
                  :w (:w canvas-size)
                  :h (:h canvas-size)}
                 nil
                 (partial draw-rect (:bg colours))))

(def ball
  (let [ball-dim {:x 0
                  :y 0
                  :w ball-size
                  :h ball-size}]
    (canvas/entity (center-ball ball-dim canvas-size)
                   nil
                   (partial draw-rect (:ball colours)))))

(defn update-player-paddle
  "Center the paddle on the mouse. Also clamp the paddle y to the canvas."
  [event world-state ent]
  (let [mouse-y   (second (:mouse-coords world-state))
        paddle-h  (get-in ent [:value :h])
        paddle-hh (/ paddle-h 2)
        new-y     (- mouse-y paddle-hh)]
    (assoc-in ent [:value :y] (clamp new-y 0 (- (:h canvas-size) paddle-h)))))

(def player-paddle
  (create-paddle update-player-paddle
                 [(- (- (:w canvas-size) (:w paddle-size))
                     paddle-offset)
                  init-paddle-y]))

(def ai-paddle
  (create-paddle nil [paddle-offset init-paddle-y]))
;; ==================================================================

(defn update-state
  "Update each entity if it has an update fn."
  [event world-state]
  (update-in world-state [:entities]
             #(mapv (fn [ent]
                      (if-let [update (:update ent)]
                        (update event world-state ent)
                        ent))
                    %)))

(defn update-mouse-coords
  "Update the mouse coords when a mouse-move event happens on the canvas."
  [canvas event world-state]
  (assoc world-state
    :mouse-coords (client->canvas-coords canvas (client-coords event))))

(defn render-scene
  "Loop over the entities calling each ones draw fn."
  [ctx world-state]
  (doseq [ent (:entities world-state)]
    (let [{:keys [value draw]} ent]
      (try
        (draw ctx value)
        (catch js/Error e
          (println e))))))

(defn ^:export init
  "The main entry point for the game. The big-bang process is launched from here."
  []
  (let [canvas (sel1 :#pong-canvas)
        ctx    (init-canvas canvas canvas-size)]
    (go
     (big-bang!
      :initial-state {:entities     [background player-paddle ai-paddle ball]
                      :mouse-coords [0 0]}

      :on-tick      update-state
      :on-mousemove (partial update-mouse-coords canvas)
      :event-target canvas
      :to-draw      (partial render-scene ctx)))))