package togos.solidtree.forth.procedure;

import java.util.Map;

import togos.lang.ScriptError;
import togos.lang.SourceLocation;
import togos.solidtree.DColor;
import togos.solidtree.DensityFunctionDividedSolidNode;
import togos.solidtree.GeneralMaterial;
import togos.solidtree.HomogeneousSolidNode;
import togos.solidtree.NodeRoot;
import togos.solidtree.RegularlySubdividedSolidNode;
import togos.solidtree.SimplexNoise;
import togos.solidtree.SolidNode;
import togos.solidtree.StandardMaterial;
import togos.solidtree.SurfaceMaterial;
import togos.solidtree.SurfaceMaterialLayer;
import togos.solidtree.forth.Interpreter;
import togos.solidtree.forth.StandardWordDefinition;
import togos.solidtree.forth.WordDefinition;
import togos.solidtree.trace.TraceNode;

public class NodeProcedures
{
	static final StandardWordDefinition MAKE_COLOR = new StandardWordDefinition() {
		// red, green, blue -> DColor
		
		@Override public void run( Interpreter interp, SourceLocation sLoc ) throws ScriptError {
			double blue  = interp.stackPop( Number.class, sLoc ).doubleValue();
			double green = interp.stackPop( Number.class, sLoc ).doubleValue();
			double red   = interp.stackPop( Number.class, sLoc ).doubleValue();
			interp.stackPush( new DColor(red, green, blue) );
		}
	};
	
	static final StandardWordDefinition MAKE_SURFACE_MATERIAL_LAYER = new StandardWordDefinition() {
		// opacity
		// filter color
		// emission collor
		// mirror reflection factor
		// random reflection factor
		// normal reflection factor
		// forward reflection factor
		// -> SurfaceMaterial
		
		@Override public void run( Interpreter interp, SourceLocation sLoc ) throws ScriptError {
			double forwardRF = interp.stackPop( Number.class, sLoc ).doubleValue();
			double normalRF  = interp.stackPop( Number.class, sLoc ).doubleValue();
			double randomRF  = interp.stackPop( Number.class, sLoc ).doubleValue();
			double mirrorRF  = interp.stackPop( Number.class, sLoc ).doubleValue();
			DColor emissionColor = interp.stackPop( DColor.class, sLoc );
			DColor filterColor = interp.stackPop( DColor.class, sLoc );
			double opacity   = interp.stackPop( Number.class, sLoc ).doubleValue();
			interp.stackPush( new SurfaceMaterialLayer(
				opacity, filterColor, emissionColor,
				mirrorRF, randomRF, normalRF, forwardRF
			));
		}
	};
	
	static final StandardWordDefinition MAKE_SURFACE_MATERIAL = new StandardWordDefinition() {
		// layer0:SurfaceMaterialLayer, layer1...layerN,
		// layerCount:int -> SurfaceMaterial
		@Override public void run(Interpreter interp, SourceLocation sLoc) throws ScriptError {
			int layerCount = interp.stackPop( Number.class, sLoc ).intValue();
			SurfaceMaterialLayer[] layers = new SurfaceMaterialLayer[layerCount];
			for( int i=layerCount-1; i>=0; --i ) {
				layers[i] = interp.stackPop( SurfaceMaterialLayer.class, sLoc );
			}
			interp.stackPush( new SurfaceMaterial(layers) );
		}
	};
	
	static final StandardWordDefinition MAKE_VISUAL_MATERIAL = new StandardWordDefinition() {
		// surface material : SurfaceMaterial
		// index of refraction : double,
		// internal filter color : DClor
		// internal emission color : DColor
		// particle interaction chance : double
		// particle surface material : SurfaceMaterial
		// -> PathTraceMaterial
		
		@Override public void run(Interpreter interp, SourceLocation sLoc) throws ScriptError {
			SurfaceMaterial particleMaterial = interp.stackPop( SurfaceMaterial.class, sLoc );
			double particleInteractionChance = interp.stackPop( Number.class, sLoc ).doubleValue();
			DColor internalEmissionColor = interp.stackPop( DColor.class, sLoc );
			DColor internalFilterColor = interp.stackPop( DColor.class, sLoc );
			double indexOfRefraction = interp.stackPop( Number.class, sLoc ).doubleValue();
			SurfaceMaterial surfaceMaterial = interp.stackPop( SurfaceMaterial.class, sLoc );
			interp.stackPush( new StandardMaterial(
				surfaceMaterial, indexOfRefraction,
				internalFilterColor, internalEmissionColor,
				particleInteractionChance, particleMaterial
			) );
		}
	};
	
	static final StandardWordDefinition MAKE_SIMPLE_VISUAL_MATERIAL = new StandardWordDefinition() {
		// filter color
		// emission color
		// surface mirror-ness
		// opacity ( used for both surface and internal opacity )
		// index of refraction
		// -> GenericMaterial
		
		@Override public void run( Interpreter interp, SourceLocation sLoc ) throws ScriptError {
			double ior        = interp.stackPop( Number.class, sLoc ).doubleValue();
			double opacity    = interp.stackPop( Number.class, sLoc ).doubleValue();
			double mirrosity  = interp.stackPop( Number.class, sLoc ).doubleValue();
			DColor emission   = interp.stackPop( DColor.class, sLoc );
			DColor filter     = interp.stackPop( DColor.class, sLoc );
			
			SurfaceMaterial surfMat = SurfaceMaterial.TRANSPARENT;
			if( opacity > 0 ) {
				surfMat = SurfaceMaterial.combine( surfMat, new SurfaceMaterialLayer(opacity, filter, emission, 0, 1, 1, 0) );
			}
			if( mirrosity > 0 ) {
				surfMat = SurfaceMaterial.combine( surfMat, new SurfaceMaterialLayer(mirrosity, DColor.WHITE, DColor.BLACK, 1, 0, 0, 0) );
			}
			
			interp.stackPush( new StandardMaterial(surfMat, ior, filter, emission, 0, SurfaceMaterial.TRANSPARENT) );
		}
	};
	
	static final StandardWordDefinition MAKE_HOMOGENEOUS_NODE = new StandardWordDefinition() {
		// GenericMaterial -> SolidNode
		
		@Override public void run( Interpreter interp, SourceLocation sLoc ) throws ScriptError {
			interp.stackPush( new HomogeneousSolidNode(interp.stackPop( GeneralMaterial.class, sLoc )) );
        }
	};
	
	static final StandardWordDefinition MAKE_COMPOSITE_NODE = new StandardWordDefinition() {
		// subnode0 subnode1 ... subnodeN divX divY divZ -> SolidNode
		
		@Override public void run( Interpreter interp, SourceLocation sLoc ) throws ScriptError {
			int divZ = interp.stackPop( Number.class, sLoc ).intValue();
			int divY = interp.stackPop( Number.class, sLoc ).intValue();
			int divX = interp.stackPop( Number.class, sLoc ).intValue();
			SolidNode[] subNodes = new SolidNode[divX*divY*divZ];
			for( int i=divX*divY*divZ-1; i>=0; --i ) {
				subNodes[i] = interp.stackPop( SolidNode.class, sLoc );
			}
			
			interp.stackPush( RegularlySubdividedSolidNode.build(divX, divY, divZ, subNodes) );
        }		
	};
	
	static final StandardWordDefinition PAD = new StandardWordDefinition() {
		// core:SolidNode, pad:SolidNode, divisions:int, iterations:int
		
		public void run(Interpreter interp, SourceLocation sLoc) throws ScriptError {
			int spaceIterations = interp.stackPop( Number.class, sLoc).intValue();
			int spaceDivisions  = interp.stackPop( Number.class, sLoc).intValue();
			SolidNode pad  = interp.stackPop( SolidNode.class, sLoc );
			SolidNode core = interp.stackPop( SolidNode.class, sLoc );
			
			if( spaceDivisions < 1 ) {
				throw new ScriptError("padding divisions must be >= 1; "+spaceDivisions+" given", sLoc);
			}
			if( spaceDivisions > 16 ) {
				throw new ScriptError("padding divisions must be <= 16; "+spaceDivisions+" given", sLoc);
			}
			
			final int spaceDivisions3 = spaceDivisions*spaceDivisions*spaceDivisions;
			
			SolidNode space = core;
			for( int i=0; i<spaceIterations; ++i ) {
				SolidNode[] spaceSubNodes = new SolidNode[spaceDivisions3];
				for( int j=0; j<spaceDivisions3; ++j ) {
					spaceSubNodes[j] = pad;
				}
				
				spaceSubNodes[
				   spaceDivisions*spaceDivisions*(spaceDivisions/2) +
				   spaceDivisions*(spaceDivisions/2) +
				   (spaceDivisions/2)
				] = space;
				space = RegularlySubdividedSolidNode.build( spaceDivisions, spaceDivisions, spaceDivisions, spaceSubNodes );
			}
			
			interp.stackPush( space );
		}
	};
	
	static final StandardWordDefinition MAKE_ROOT = new StandardWordDefinition() {
		// SolidNode, w, h, d -> NodeRoot
		
		@Override public void run(Interpreter interp, SourceLocation sLoc) throws ScriptError {
			double d = interp.stackPop( Number.class, sLoc ).doubleValue();
			double h = interp.stackPop( Number.class, sLoc ).doubleValue();
			double w = interp.stackPop( Number.class, sLoc ).doubleValue();
			// TODO: SolidNode and TraceNode should both implement
			// some node interface which this would accept any implementation of
			SolidNode core = interp.stackPop( SolidNode.class, sLoc );
			interp.stackPush( new NodeRoot<SolidNode>(core, w, h, d) );
		}
	};
	
	protected static SolidNode toSolidNode( Object o ) {
		if( o instanceof SolidNode ) {
			return (SolidNode)o;
		} else if( o instanceof GeneralMaterial ) {
			return new HomogeneousSolidNode( (GeneralMaterial)o );
		} else {
			throw new RuntimeException("Don't know how to turn "+o+" into a solid node");
		}
	}
	
	static final StandardWordDefinition MAKE_DENSITY_FUNCTION_DIVIDED_NODE = new StandardWordDefinition() {
		// material1, material2, density function -> Node
		@Override public void run(Interpreter interp, SourceLocation sLoc) throws ScriptError {
			TraceNode.DensityFunction df = interp.stackPop(TraceNode.DensityFunction.class, sLoc);
			SolidNode nodeB = toSolidNode(interp.stackPop(Object.class, sLoc));
			SolidNode nodeA = toSolidNode(interp.stackPop(Object.class, sLoc));
			interp.stackPush(new DensityFunctionDividedSolidNode(df, nodeA, nodeB));
		}
	};
	
	// TODO: NOT THREAD SAFE PLZ FIX
	static final SimplexNoise sn = new SimplexNoise();
	
	static final StandardWordDefinition MAKE_POND_RIPPLE_DENSITY_FUNCTION = new StandardWordDefinition() {
		// -> DensityFunction
		@Override public void run(Interpreter interp, SourceLocation sLoc) throws ScriptError {
			final double surfaceY = interp.stackPop( Number.class, sLoc ).doubleValue();
			interp.stackPush(new TraceNode.DensityFunction() {
				@Override public double getMaxGradient() {
					return 0.5;
				}
				
				@Override public double apply(double x, double y, double z) {
					return (y - surfaceY) * 0.3 +
						0.02 * sn.apply((float)x, (float)0, (float)z) +
						0.01 * sn.apply((float)x * 10, (float)0, (float)z * 10);
				}
			});
		}
	};
	
	public static void register( Map<String,? super WordDefinition> ctx ) {
		ctx.put("empty-node", new ConstantValue(HomogeneousSolidNode.EMPTY) );
		ctx.put("make-homogeneous-node", MAKE_HOMOGENEOUS_NODE);
		ctx.put("make-composite-node", MAKE_COMPOSITE_NODE);
		ctx.put("make-density-function-divided-node", MAKE_DENSITY_FUNCTION_DIVIDED_NODE);
		ctx.put("make-simple-volumetric-material", MAKE_SIMPLE_VISUAL_MATERIAL);
		ctx.put("make-surface-material", MAKE_SURFACE_MATERIAL);
		ctx.put("make-surface-material-layer", MAKE_SURFACE_MATERIAL_LAYER);
		ctx.put("make-volumetric-material", MAKE_VISUAL_MATERIAL);
		ctx.put("make-color", MAKE_COLOR);
		ctx.put("pad", PAD);
		ctx.put("make-root", MAKE_ROOT);
		
		// until it's possible to define in scripts...
		ctx.put("make-pond-ripple-density-function", MAKE_POND_RIPPLE_DENSITY_FUNCTION);
	}
}
