"defs.fs" include

"rough-mirror-surface" import

  transparent-surface
  1.0
  white # 0.9997 0.9997 0.9997 make-color
  black # 0.0001 0.0001 0.0002 make-color
  0.2
  rough-mirror-surface
make-volumetric-material "fog" def-value

  transparent-surface
  1.0
  white
  black
  0.0
  transparent-surface
make-volumetric-material "space" def-value

space make-solid-material-node "space-node" def-value
fog make-solid-material-node "fog-node" def-value

                     fog "empty"       ctx-put
"bungalos"       ctx-get "material"    ctx-put
"bottomify8x8x8" ctx-get "ground"      ctx-put
"brick"          ctx-get "underground" ctx-put
"groundify8x8x8" ctx-get

262144 "world-size" def-value

space-node 3 3 pad
world-size dup dup make-root
