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

place-node cn64 cn64 cn64 cn64


