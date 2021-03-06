# Data Model

A 'solid node' is a representation of distribution of matter filling a given
space.  The same node may be re-used in multiple spaces.  e.g. a node
may represent a brick, and that brick node may be used over and over
to build a wall.

A 'node material' defines:
- A volume ID.
- A volume material that fills the node.
- A surface material to describe surfaces between this node and nodes
  with a material with a DIFFERENT volume ID.

A 'volume material' defines the properties of a material that fills a
space.  It determines:
- Physical properties (such as viscosity) if needed by a simulation.
- Light conduction properties:
  - Amount of light emitted per meter.
  - Amount of light absorbed per meter.
  - Likelihood per meter that a light ray will hit a particle.
  - The way in which light is scattered if it hits a particle (this may be defined by a surface material).

A 'surface material' defines how light interacts with a surface, and
may include multiple layers.
- Physical properties (e.g. coefficient of friction)

A 'surface material layer' defines:
- How light bounces when it hits this layer
  new vector =
    incoming vector * incoming coefficient +
    normal unit vector * normal coefficient +
    random unit vector * random coefficient +
    reflected vector * reflected coefficient
- How light is filtered when it hits or passes through this layer
- How light is emitted from this layer (not affected by this layer's filtering)

A 'node root' provides a node and its X, Y, Z bounds

A 'camera setting' includes all information needed to map sensor coordinates to vectors:
- position
- orientation
- lens characteristics; for a relatively normal lens this might include:
  - projection (mapping of pixel X,Y -> vector)
  - aperture size and shape
  - size and shape of focal surface (plane, sphere, etc)

A 'render task' includes:
- A scene
- A function to provide camera settings at time t=0..1
- A canvas size (columns,rows of pixels)
- A strategy for selecting points on the canvas to trace

## URNs

### placeholder:<string>

A placeholder object.  The purpose of placeholder nodes is to be
replaced by something else after being loaded.

Generally, in the context of solid nodes, ```placeholder:0``` should
be used to mean 'undefined space'.  It will usually be replaced by
something representing empty space or some fluid or gas.

### minecraft:<block type ID>[:<data value>]

When used to reference a node, refers to 1x1x1m minecraft block of the
given type.

<block type ID> may be either an integer or the name as documented
under 'Name' at http://minecraft.gamepedia.com/Data_values or listed
at http://minecraft-ids.grahamedgecombe.com/.

e.g. minecraft:0 is the same as minecraft:air, minecraft:1:6 or
minecraft:stone:6 both refer to Polished Andesite.  When no data value
is given, it is treated as zero.  The canonical representation of a
block with zero data value is to leave off the data value part.  e.g.
'minecraft:stone' rather than 'minecraft:stone:0'.

### Blob-hash URNs

URNs referring to blobs (e.g. 'urn:sha1:...'), when used to name a
node, refer to the node encoded by the named blob.  The encoding must
be determined by examining the blob.

If the object encoded by the blob is a material, then it is coerced to
a node by creating a homogeneous node of that material.

## JSON Node format

```
{
  "classRef": "http://ns.nuke24.net/SolidTree/SolidNode",
  "axisDivisions": [2,2,2],
  "definitionTable": {
    0: 
  },
  "subNodes": [
    0,0,
    0,1,
    0,0,
    0,"
  ]
}
```

Properties:
- classRef - indicates that the thing encoded is a SolidNode
? function - if present, this is a 'lazily-generated' solid node, generated by the specified function
? functionParams - oprional additional named parameters to the function
? axisDivisions - a 3-element array giving the number of X, Y, and Z
  slices, if this node is regularly divided along the X,Y,Z axes.
? binaryDivision - some representation of how the thing is divided in 2 -
  mutually exclusive from axisDivisions
? definitionTable - a table of short -> long names (or inline representatios) of sub-nodes, usable by subNodeRefs
? subNodes - a list of sub-node references, each of which may be
  - a integer or string naming a node from definitionTable
  - a string providing a reference to a node
  - an inline node definition

If binary division is used, subNodes must be an array of length 2.
If axis division is used, subNodes must be an array of X divisions * Y divisions * Z divisions
