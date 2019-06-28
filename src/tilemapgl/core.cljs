(ns tilemapgl.core
  (:require [infinitelives.pixi.resources :as r]
            [infinitelives.pixi.texture :as t]
            [infinitelives.pixi.canvas :as c]
            [infinitelives.pixi.sprite :as s]
            [infinitelives.pixi.pixelfont :as pf]
            [infinitelives.utils.events :as e]
            [infinitelives.utils.gamepad :as gp]
            [infinitelives.utils.vec2 :as vec2]
            [infinitelives.utils.console :refer [log]]
            [cljs.core.match :refer-macros [match]]
            [clojure.string :as string]
            [cljs.core.async :refer [chan close! >! <! timeout]])
  (:require-macros [cljs.core.async.macros :refer [go]])
  )

(enable-console-print!)

(println "This text is printed from src/tilemapgl/core.cljs. Go ahead and edit it and see reloading in action.")

;; define your app data so that it doesn't get over-written on reload

(defonce app-state (atom {:text "Hello world!"}))


(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)

(defonce bg-colour 0x202000)

(defonce canvas
  (c/init {:layers [:bg :tilemap :stats :title :ui]
           :background bg-colour
           :expand true
           :origins {:stats :bottom-left
                     :title :top}
           :translate {:stats [40 -40]
                       :title [0 40]}}))

(s/set-default-scale! 1)

(defonce main
  (go
    (<! (r/load-resources canvas :ui ["img/tiles.png"]))

    (c/with-sprite canvas :tilemap
      [tiles (s/make-sprite (r/get-texture :tiles :nearest))]
      (<! (timeout 10000))
      )

    ))
