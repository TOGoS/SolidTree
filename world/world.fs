"defs.fs" include

"rough-mirror-surface" import

  transparent-surface
  1.0
  white # 0.9997 0.9997 0.9997 make-color
  black # 0.0001 0.0001 0.0002 make-color
  0.2
  rough-mirror-surface
make-volumetric-material "thick-fog" def-value

  transparent-surface
  1.0
  0.999997 0.999997 0.999997 make-color
  0.000100 0.000100 0.000300 make-color
  0.0
  transparent-surface
make-volumetric-material "haze" def-value

  transparent-surface
  1.0
  0.5 0.5 0.5 make-color
  10.0 10.0 10.0 make-color
  0.0
  transparent-surface
make-volumetric-material "sun" def-value

# 1.7801960336557516 750.8770650821589 -9.949649823723387 set-camera-position -2.9452431127404317 set-camera-yaw -5.890486225480866 set-camera-pitch 0.0 set-camera-roll
# 1.7801960336557516 540.1 -9.949649823723387 set-camera-position

"space" import

sun       "sun"   ctx-put
space     "space" ctx-put
haze      "empty" ctx-put

"bungalos"       ctx-get "material"    ctx-put
"bottomify8x8x8" ctx-get "ground"      ctx-put
"bottomify8x8x8" ctx-get "above-ground" ctx-put
"brick"          ctx-get "below-ground" ctx-put
"world-s0" ctx-get

8 4 ** "world-size" def-value
world-size dup dup make-root
