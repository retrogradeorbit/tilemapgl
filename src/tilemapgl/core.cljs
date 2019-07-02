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

(def fragment-shader
  "

  uniform sampler2D map;
  uniform sampler2D tiles;

  varying vec2 vTextureCoord;

  void main() {
    vec2 fragpixelpos = vTextureCoord*512.;
    vec2 tilepixelpos = vTextureCoord*32.;
    vec4 tile = texture2D(map, tilepixelpos/64.);

    // tile location on sprite sheet
    float x = (tile.x * 256.0);
    float y = (tile.y * 256.0);
    float a = tile.a;

    // tile pixel location on sprite sheet
    float tilexpixelpos = x * 32.;
    float tileypixelpos = y * 32.;

    float ipx = mod(fragpixelpos.x, 32.0);
    float ipy = mod(fragpixelpos.y, 32.0);

    vec2 finalpixelpos = vec2( (tilexpixelpos + ipx),
                               (tileypixelpos + ipy));


    vec4 final = texture2D(tiles, vec2(finalpixelpos.x/448.,
                                         finalpixelpos.y/320.));

    gl_FragColor = (a == 0.0?vec4(0.,0.,0.,0.):final);
    // vec4(0.9,0.3,0.3,1.0);
  }
")

(defn make-shader []
  (let [shader (js/PIXI.Filter.
                nil
                fragment-shader)]
    (set! (.-uniforms.time shader) 1.0)
    (set! (.-uniforms.map shader) (r/get-texture :map :nearest))
    (set! (.-uniforms.tiles shader) (r/get-texture :tiles :nearest))

    shader))

(defn make-background []
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
      (.drawRect 0 0 512 512)
      .endFill)
    #_ (.generateTexture bg false)))

(defn print-gl-version []
  (when-let [gl (.getContext (:canvas canvas) "webgl2")]
    (js/console.log (.getParameter gl (.-SHADING_LANGUAGE_VERSION gl)))))

(defn set-texture-filter [texture filter]
  (set! (.-filters texture) (make-array filter)))

(defonce main
  (go
    (print-gl-version)

    (<! (r/load-resources canvas :ui ["img/tiles.png" "img/map.png"]))

    (let [shader (make-shader)]
      (c/with-sprite canvas :tilemap
        [;;tiles (s/make-sprite (r/get-texture :tiles :nearest))
         bg (make-background) #_(s/make-sprite (make-background) :scale 1)
         ]
        (set-texture-filter bg shader)

        (while true
          (<! (timeout 1000)))
        ))

    ))
