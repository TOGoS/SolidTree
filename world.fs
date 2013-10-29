1 1 1 make-color "white" def-value
0 0 0 make-color "black" def-value
0 make-surface-material "transparent-surface" def-value

# surface-material -> volumetric-material
: make-opaque-material 2 black black 0 transparent-surface make-volumetric-material ;

1 1 1 make-color 2 2 2 make-color 0 0.5 2 make-simple-volumetric-material make-solid-material-node "light-node" def-value

    1
    0.5 0.5 0.5 make-color
    black
    1 0.05 0 0
  make-surface-material-layer
    1
    0.5 0.5 0.5 make-color
    black
    0 1 1 0
  make-surface-material-layer
  2
make-surface-material "rough-mirror-surface" def-value

    1
    0.3 0.5 0.2 make-color
    black
    1 1 1 0
  make-surface-material-layer
  1
make-surface-material make-opaque-material make-solid-material-node "grass-node" def-value

rough-mirror-surface make-opaque-material make-solid-material-node "wally-node" def-value




# 0.6 0.5 0.4 make-color 0 0 0 make-color 0 1 2
# make-simple-visual-material

wally-node wally-node wally-node wally-node
wally-node empty-node empty-node wally-node
wally-node wally-node empty-node wally-node
wally-node wally-node wally-node wally-node

wally-node empty-node empty-node wally-node
empty-node empty-node empty-node wally-node
empty-node empty-node empty-node empty-node
wally-node empty-node wally-node wally-node

wally-node wally-node empty-node wally-node
empty-node empty-node empty-node empty-node
wally-node empty-node empty-node empty-node
wally-node empty-node empty-node wally-node

wally-node wally-node wally-node wally-node
wally-node empty-node empty-node wally-node
wally-node empty-node empty-node wally-node
wally-node wally-node wally-node wally-node

4 4 4 make-composite-node "cubey-node" def-value

wally-node cubey-node cubey-node wally-node
wally-node empty-node empty-node wally-node
wally-node cubey-node empty-node wally-node
wally-node cubey-node cubey-node wally-node

cubey-node empty-node empty-node cubey-node
empty-node cubey-node empty-node empty-node
empty-node light-node cubey-node empty-node
cubey-node empty-node empty-node cubey-node

cubey-node empty-node empty-node cubey-node
empty-node empty-node empty-node empty-node
empty-node cubey-node empty-node empty-node
cubey-node empty-node empty-node cubey-node

wally-node cubey-node cubey-node wally-node
wally-node cubey-node empty-node wally-node
wally-node cubey-node empty-node wally-node
wally-node cubey-node cubey-node wally-node

4 4 4 make-composite-node "lampy-node" def-value

grass-node grass-node grass-node grass-node
empty-node empty-node empty-node empty-node
empty-node empty-node empty-node empty-node
lampy-node lampy-node lampy-node lampy-node

grass-node grass-node grass-node grass-node
empty-node empty-node empty-node empty-node
empty-node empty-node empty-node empty-node
lampy-node wally-node wally-node lampy-node

grass-node grass-node grass-node grass-node
empty-node lampy-node empty-node empty-node
empty-node lampy-node empty-node empty-node
lampy-node wally-node wally-node lampy-node

grass-node grass-node grass-node grass-node
empty-node empty-node empty-node empty-node
empty-node empty-node empty-node empty-node
wally-node wally-node wally-node lampy-node

4 4 4 make-composite-node "place-node" def-value

: multiply4 dup dup dup dup ;
: multiply16 multiply4 multiply4 multiply4 multiply4 ;
: multiply64 multiply16 multiply16 multiply16 multiply16 ;

: cn64 multiply64 4 4 4 make-composite-node ;

# place-node cn64 cn64 cn64 cn64

#### Different

  transparent-surface
  1.0
  white
  0.0003 0.0002 0.0001 make-color
  0.001
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
wally-node "X" def-value

X X X - X X
- X X - X X
- - - - - X
X X X X - X
X X X X - X
X - - - - X

X X X - - -
X X X X X X
- - X X X X
X - - - X X
X X X - X -
X X X - X -

X - X X X X
X - X - - -
X X X - X X
X X X - X X
- - X - X -
X - X - X X

X X X - X X
X - X - X -
X X X X X -
- X X X X -
- X X X X X
- - X - X X

X X X X - -
X - - - X X
X X X - X X
- - X - X -
X X X - X X
X X X - X X

X X X - - -
- X X X X -
X X X X X X
X - X X X -
X - X X X -
X - X - - -

6 6 6 make-composite-node "zame-node" def-value

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
