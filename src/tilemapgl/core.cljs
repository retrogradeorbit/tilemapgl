(ns tilemapgl.core
  (:require [infinitelives.pixi.resources :as r]
            [infinitelives.pixi.canvas :as c]
            [infinitelives.pixi.sprite :as s]
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

(def vertex-shader
  "#version 300 es
  in vec2 aVertexPosition;
  in vec2 aTextureCoord;

  uniform mat3 projectionMatrix;

  out vec2 vTextureCoord;

  void main(void){
     gl_Position = vec4((projectionMatrix * vec3(aVertexPosition, 1.0)).xy, 0.0, 1.0);

     vTextureCoord = aTextureCoord;
  }"
  )

(def fragment-shader
  "#version 300 es
  precision highp float;

  uniform sampler2D map;
  uniform sampler2D tiles;
  uniform ivec2 fragsize;
  uniform ivec2 tilesize;

  in vec2 vTextureCoord;

  out vec4 diffuseColor;

  void main() {
    highp ivec2 mapsize = textureSize(map, 0);
    highp ivec2 tilesheetsize = textureSize(tiles, 0);

    vec2 fragpixelpos = vTextureCoord*float(fragsize);
    vec2 tilepixelpos = floor(fragpixelpos)/float(mapsize);
    vec4 tile = texture(map, tilepixelpos/float(tilesize));

    if(tile.a == 0.0) { discard;}

    // tile location on sprite sheet
    vec2 tileoffset = tile.xy * 255. * float(tilesize);

    // tile pixel location on sprite sheet
    vec2 innercoord = mod(fragpixelpos, float(tilesize));

    vec2 finalpixelpos = tileoffset + innercoord;
    diffuseColor = texture(tiles, vec2(finalpixelpos.x/448.,
                                       finalpixelpos.y/320.));
  }
")

(defn set-uniform [filter name value]
  (aset (.-uniforms filter) name value))

(defn make-shader [texture]
  (let [shader (js/PIXI.Filter.
                vertex-shader
                fragment-shader)]
    (set-uniform shader "time" 1.0)
    (set-uniform shader "fragsize" #js [1024 1024])
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
      (.drawRect 0 0 1024 1024)
      .endFill)))

(defn print-gl-version []
  (when-let [gl (.getContext (:canvas canvas) "webgl2")]
    (js/console.log (.getParameter gl (.-SHADING_LANGUAGE_VERSION gl)))))

(defn set-texture-filter [texture filter]
  (set! (.-filters texture) (make-array filter)))

(defn plot [data x y r g]
  (aset data (+ 0 (* 4 x) (* y 64 4)) r)
  (aset data (+ 1 (* 4 x) (* y 64 4)) g)
  (aset data (+ 2 (* 4 x) (* y 64 4)) 0)
  (aset data (+ 3 (* 4 x) (* y 64 4)) 255)
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
  (plot data (+ w x) (+ 3 y) 0 9)



  )

(defn make-map-image []
  (let [height 64
        width 64
        data (make-array Uint8 (* 4 width height))
        ]
    (js/console.log data)
    (room data 0 0 12 7)
    (room data 13 0 5 12)
    (room data 0 8 4 4)
    (room data 5 8 7 10)
    (room data 0 13 4 10)

    (js/PIXI.Texture.fromBuffer (js/Uint8Array. data) 64 64)
    )
  )


(defonce main
  (go
    (print-gl-version)

    (<! (r/load-resources canvas :ui ["img/tiles.png" "img/map.png"]))

    (let [texture (make-map-image)
          shader (make-shader texture)]
      (c/with-sprite canvas :tilemap

        [;;tiles (s/make-sprite (r/get-texture :tiles :nearest))
         ;;map-obj (s/make-sprite texture)
         bg (make-backg)
         ]
        (s/set-pos! bg -256 -256)
        (set-texture-filter bg shader)

        (while true
          (<! (timeout 1000)))))))
