"defs.fs" include

"rough-mirror-surface" import
  transparent-surface
  1.0
  0.997 0.994 0.990 make-color
  0.0004 0.0004 0.0008 make-color
  0.0
  rough-mirror-surface
make-volumetric-material "fog" def-value

fog make-solid-material-node "fog-node" def-value

fog "empty" ctx-put
"mazemaze" ctx-get

fog-node 3 3 pad
1024 1024 1024 make-root
