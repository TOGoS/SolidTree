"defs.fs" include

"rough-mirror-surface" import
  transparent-surface
  1.0
  0.99997 0.99994 0.99990 make-color
  0.00001 0.00001 0.00002 make-color
  0.0
  rough-mirror-surface
make-volumetric-material "fog" def-value

fog make-solid-material-node "fog-node" def-value

fog "empty" ctx-put
"thingy" ctx-get

fog-node 3 3 pad
512 512 512 make-root
