# tilemapgl

A high speed retro tilemap game engine in clojurescript based on a sole fragment shader.

[TRY IT NOW!](https://retrogradeorbit.github.io/tilemapgl/)

## Overview

 * fast
 * large tilemap
 * rendered soley on the GPU
 * uses infinitelives.pixi

## Setup

Install infinitelives.utils

  * checkout infinitelives.utils repo
  * `lein install` inside the project

Install infinitelives.pixi

  * checkout infinitelives.pixi repo
  * `lein install` inside the project

To get an interactive development environment run:

    lein figwheel

and open your browser at [localhost:3449](http://localhost:3449/).
This will auto compile and send all changes to the browser without the
need to reload. After the compilation process is complete, you will
get a Browser Connected REPL. An easy way to try it is:

    (js/alert "Am I connected?")

and you should see an alert in the browser window.

To clean all compiled files:

    lein clean

To create a production build run:

    lein do clean, cljsbuild once min

And open your browser in `resources/public/index.html`. You will not
get live reloading, nor a REPL.

## License

Copyright © 2019 Crispin Wellington

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
