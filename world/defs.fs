: import dup ctx-get swap def-value ;

: multiply4 dup dup dup dup ;
: multiply16 multiply4 multiply4 multiply4 multiply4 ;
: multiply64 multiply16 multiply16 multiply16 multiply16 ;

1 1 1 make-color "white" def-value
0 0 0 make-color "black" def-value

"transparent-surface" import

# surface-material -> volumetric-material
: make-opaque-material 2 black black 0 transparent-surface make-volumetric-material ;
