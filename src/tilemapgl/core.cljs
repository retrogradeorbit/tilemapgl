(ns tilemapgl.core
  (:require [infinitelives.pixi.resources :as r]
            [infinitelives.pixi.canvas :as c]
            [infinitelives.pixi.sprite :as s]
            [infinitelives.utils.events :as e]
            [cljs.core.async :refer [chan close! >! <! timeout]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defonce bg-colour 0x202000)

(defonce canvas
  (c/init {:layers [:bg :tilemap :stats :title :ui]
           :background bg-colour
           :expand true
           :origins {:stats :bottom-left
                     :title :top}
           :translate {:stats [40 -40]
                       :title [0 40]}}))

(def fragment-shader
  "precision highp float;

  uniform sampler2D map;
  uniform sampler2D tiles;
  uniform vec2 scroll;
  uniform vec2 fragsize;
  uniform vec2 tilesize;
  uniform vec2 mapsize;
  uniform vec2 tilesheetsize;

  varying vec2 vTextureCoord;

  void main() {
    vec2 fragpixelpos = vTextureCoord*fragsize+scroll;
    vec2 tilepixelpos = floor(fragpixelpos)/mapsize;
    vec4 tile = texture2D(map, tilepixelpos/tilesize);

    // if map pixel has alpha of 0, render nothing
    if(tile.a == 0.0) { discard;}

    // tile location on sprite sheet
    vec2 tileoffset = tile.xy * 255. * tilesize;

    // tile pixel location on sprite sheet *inside* the tile we are on
    vec2 innercoord = mod(fragpixelpos, tilesize);

    gl_FragColor = texture2D(tiles,  (tileoffset + innercoord)/tilesheetsize);
  }
")

(defn set-uniform [filter name value]
  (aset (.-uniforms filter) name value))

(defn make-shader [texture]
  (let [shader (js/PIXI.Filter.
                nil
                fragment-shader)]
    (set-uniform shader "mapsize" #js [512. 512.])
    (set-uniform shader "tilesheetsize" #js [448. 320.])
    (set-uniform shader "fragsize" #js [(.-innerWidth js/window)
                                        (.-innerHeight js/window)])
    (set-uniform shader "scroll" #js [-500 -500])
    (set-uniform shader "tilesize" #js [32. 32.])
    (set-uniform shader "map" texture)
    (set-uniform shader "tiles" (r/get-texture :tiles :nearest))

    shader))

(defn make-backg []
  (let [bg (js/PIXI.Graphics.)
        border-colour 0x000000
        width 32
        height 32
        full-colour 0xff0000
        ]
    (doto bg
      (.beginFill 0xff0000)
      (.lineStyle 0 border-colour)
      (.drawRect 0 0 width height)
      (.lineStyle 0 border-colour)
      (.beginFill full-colour)
      (.drawRect 0 0 8192 8192)
      .endFill)))

(defn print-gl-version []
  (when-let [gl (.getContext (:canvas canvas) "webgl2")]
    (js/console.log (.getParameter gl (.-SHADING_LANGUAGE_VERSION gl)))))

(defn set-texture-filter [texture filter]
  (set! (.-filters texture) (make-array filter)))

(defn plot [data x y r g]
  (aset data (+ 0 (* 4 x) (* y 512 4)) r)
  (aset data (+ 1 (* 4 x) (* y 512 4)) g)
  (aset data (+ 2 (* 4 x) (* y 512 4)) 0)
  (aset data (+ 3 (* 4 x) (* y 512 4)) 255)
  )

(defn room [data x y w h]
  (plot data x y 12 7)
  (plot data (+ w x) y 13 7)
  (plot data x (+ h y) 12 8)
  (plot data (+ w x) (+ h y) 13 8)
  (doseq [n (range (inc x) (+ x w))]
    (plot data n y 0 0))

  (plot data (inc x) y 0 1)
  (plot data (+ 2 x) y 1 1)
  (plot data (+ 3 x) y 2 1)

  (doseq [n (range (inc x) (+ x w))]
    (plot data n (+ h y) 0 3))

  (plot data (inc x) (+ h y) 0 4)
  (plot data (+ 2 x) (+ h y) 1 4)
  (plot data (+ 3 x) (+ h y) 2 4)

  (doseq [n (range (inc y) (+ y h))]
    (plot data x n 6 6))

  (plot data x (+ 1 y) 6 7)
  (plot data x (+ 2 y) 6 8)
  (plot data x (+ 3 y) 6 9)

  (doseq [n (range (inc y) (+ y h))]
    (plot data (+ w x) n 0 6))

  (plot data (+ w x) (+ 1 y) 0 7)
  (plot data (+ w x) (+ 2 y) 0 8)
  (plot data (+ w x) (+ 3 y) 0 9))

(defn make-map-image []
  (let [height 512
        width 512
        data (make-array Uint8 (* 4 width height))
        ]
    ;; 2500 random room frames
    (dotimes [n 2500]
      (room data
            (int (* 500 (rand)))
            (int (* 500 (rand)))
            (int (+ 4 (* 30 (rand))))
            (int (+ 4 (* 30 (rand))))))

    ;; transfer to gfx ram
    (js/PIXI.Texture.fromBuffer (js/Uint8Array. data) 512 512)))

(defonce main
  (go
    (<! (r/load-resources canvas :ui ["img/tiles.png"]))

    (let [texture (make-map-image)
          shader (make-shader texture)]
      (c/with-sprite canvas :tilemap
        [bg (make-backg)]
        (s/set-pos! bg -4096 -4096)
        (set-texture-filter bg shader)

        (loop [t 0]
          (set-uniform shader "scroll"
                       (clj->js
                        [(int (* 16000 (+ 0.5 (/ (Math/sin (* 1.5 0.00128 t)) 2))))
                         (int (* 16000 (+ 0.5 (/ (Math/sin (* 1.5 0.0006 t)) 2))))]))
          (set-uniform shader "fragsize" #js [(.-innerWidth js/window)
                                              (.-innerHeight js/window)])
          (<! (e/next-frame))
          (recur (inc t)))))))
