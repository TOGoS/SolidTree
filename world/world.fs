"defs.fs" include

"rough-mirror-surface" import
"light" import
"grass" import

grass make-solid-material-node "grass-node" def-value
light make-solid-material-node "light-node" def-value

rough-mirror-surface make-opaque-material make-solid-material-node "wally-node" def-value

  transparent-surface
  1.0
  0.997 0.994 0.990 make-color
  0.0004 0.0004 0.0008 make-color
  0.0
  rough-mirror-surface
make-volumetric-material "fog-material" def-value
fog-material make-solid-material-node "fog-node" def-value

fog-node "empty-node" def-value

wally-node wally-node wally-node empty-node
empty-node empty-node wally-node empty-node
wally-node empty-node empty-node wally-node
wally-node wally-node wally-node empty-node
4 1 4 make-composite-node "maze0-node" def-value

empty-node empty-node empty-node empty-node
empty-node empty-node wally-node empty-node
empty-node empty-node empty-node empty-node
wally-node empty-node empty-node empty-node
4 1 4 make-composite-node "maze1-node" def-value

empty-node empty-node empty-node empty-node
empty-node empty-node wally-node empty-node
empty-node empty-node empty-node empty-node
wally-node empty-node empty-node empty-node
4 1 4 make-composite-node "maze2-node" def-value

empty-node empty-node empty-node empty-node
empty-node empty-node empty-node empty-node
empty-node empty-node empty-node empty-node
light-node empty-node empty-node empty-node
4 1 4 make-composite-node "mazel-node" def-value

maze0-node
maze1-node
maze2-node
empty-node
1 4 1 make-composite-node "mazex-node" def-value

maze0-node
maze1-node
maze2-node
mazel-node
1 4 1 make-composite-node "mazey-node" def-value

mazex-node mazex-node mazex-node mazex-node
mazex-node mazex-node mazex-node mazex-node
mazex-node mazex-node mazex-node mazex-node
mazex-node mazex-node mazex-node mazey-node
4 1 4 make-composite-node "place-node" def-value

grass-node
place-node
empty-node
empty-node
1 4 1 make-composite-node "place-node" def-value

empty-node "-" def-value

empty-node "empty" ctx-put
grass-node "grass" ctx-put
wally-node "full" ctx-put
"bricks1" import

# empty-node "empty" ctx-put
# bricks1 "material" ctx-put
"zamebricks" ctx-get "zame-node" def-value

zame-node multiply64
4 4 4 make-composite-node "zame-node" def-value

zame-node
zame-node
zame-node
1 3 1 make-composite-node "zames-node" def-value

- - -
- light-node -
- - -
3 1 3 make-composite-node "light-column-node" def-value

light-column-node - -
- zame-node -
- - light-column-node
3 1 3 make-composite-node "zame-column+lights-node" def-value

- - -
- zame-node -
- - -
3 1 3 make-composite-node "zame-column-node" def-value


wally-node
wally-node
wally-node
wally-node
wally-node
wally-node
wally-node
wally-node
wally-node
wally-node
wally-node
wally-node
grass-node
zame-column-node
zame-column+lights-node
wally-node
wally-node
wally-node
wally-node
grass-node
1 20 1 make-composite-node "garden-tower-node" def-value

place-node place-node place-node garden-tower-node
wally-node place-node place-node place-node
place-node place-node zames-node place-node
place-node place-node place-node place-node
4 1 4 make-composite-node "place-node" def-value

place-node multiply64
8 1 8 make-composite-node "place-node" def-value

place-node multiply16
4 1 4 make-composite-node "place-node" def-value

place-node empty-node 3 3 pad
16384 256 16384 make-root
