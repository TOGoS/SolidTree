package togos.solidtree.forth.procedure;

import java.util.List;
import java.util.Map;

import togos.lang.CompileError;
import togos.lang.ScriptError;
import togos.lang.SourceLocation;
import togos.solidtree.DColor;
import togos.solidtree.GeneralMaterial;
import togos.solidtree.NodeRoot;
import togos.solidtree.SolidNode;
import togos.solidtree.StandardMaterial;
import togos.solidtree.SurfaceMaterial;
import togos.solidtree.SurfaceMaterialLayer;
import togos.solidtree.forth.Handler;
import togos.solidtree.forth.Interpreter;
import togos.solidtree.forth.Procedure;
import togos.solidtree.forth.StandardWordDefinition;
import togos.solidtree.forth.Token;
import togos.solidtree.forth.WordDefinition;

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
	
	static final StandardWordDefinition MAKE_SOLID_MATERIAL_NODE = new StandardWordDefinition() {
		// GenericMaterial -> SolidNode
		
		@Override public void run( Interpreter interp, SourceLocation sLoc ) throws ScriptError {
			interp.stackPush( new SolidNode(interp.stackPop( GeneralMaterial.class, sLoc )) );
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
			
			interp.stackPush( SolidNode.build(StandardMaterial.SPACE, divX, divY, divZ, subNodes) );
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
				space = SolidNode.build( StandardMaterial.SPACE, spaceDivisions, spaceDivisions, spaceDivisions, spaceSubNodes );
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
			SolidNode core = interp.stackPop( SolidNode.class, sLoc );
			interp.stackPush( new NodeRoot(core, w, h, d) );
		}
	};
	
	public static void register( Map<String,? super WordDefinition> ctx ) {
		ctx.put("empty-node", new ConstantValue(SolidNode.EMPTY) );
		ctx.put("make-solid-material-node", MAKE_SOLID_MATERIAL_NODE);
		ctx.put("make-composite-node", MAKE_COMPOSITE_NODE);
		ctx.put("make-simple-volumetric-material", MAKE_SIMPLE_VISUAL_MATERIAL);
		ctx.put("make-surface-material", MAKE_SURFACE_MATERIAL);
		ctx.put("make-surface-material-layer", MAKE_SURFACE_MATERIAL_LAYER);
		ctx.put("make-volumetric-material", MAKE_VISUAL_MATERIAL);
		ctx.put("make-color", MAKE_COLOR);
		ctx.put("pad", PAD);
		ctx.put("make-root", MAKE_ROOT);
		
		// These should be defined in a more generic function library
		ctx.put(":", new StandardWordDefinition() {
			@Override
			public void run( final Interpreter interp, SourceLocation sLoc ) throws ScriptError {
				interp.tokenHandler = new Handler<Token,ScriptError>() {
					String newWordName;
					public void handle( Token t ) {
						newWordName = t.text;
						interp.tokenHandler = interp.compileModeTokenHandler;
						interp.wordDefinitions.put(";", new WordDefinition() {
							@Override
							public void compile( Interpreter interp, SourceLocation sLoc ) throws ScriptError {
								List<Procedure> wordList = interp.flushCompiledProcedure();
								interp.wordDefinitions.put(newWordName, new UserDefinedProcedure(wordList));
								interp.tokenHandler = interp.interpretModeTokenHandler;
							}

							@Override
							public void run( Interpreter interp, SourceLocation sLoc ) throws ScriptError {
								throw new CompileError("';' cannot be used in run-mode context", sLoc);
							}
						});
					}
				};
			}
			
		});
		ctx.put("def-value", new StandardWordDefinition() {
			@Override
            public void run( Interpreter interp, SourceLocation sLoc ) throws ScriptError {
				String name = interp.stackPop( String.class, sLoc );
				Object value = interp.stackPop( Object.class, sLoc );
				interp.wordDefinitions.put( name, new ConstantValue(value) ); 
            }
		});
		ctx.put("dup", new StandardWordDefinition() {
			@Override
            public void run( Interpreter interp, SourceLocation sLoc ) throws ScriptError {
				Object o = interp.stackPop(Object.class, sLoc);
				interp.stackPush(o);
				interp.stackPush(o);
            }
		});
		ctx.put("swap", new StandardWordDefinition() {
			@Override
            public void run( Interpreter interp, SourceLocation sLoc ) throws ScriptError {
				Object a = interp.stackPop(Object.class, sLoc);
				Object b = interp.stackPop(Object.class, sLoc);
				interp.stackPush(a);
				interp.stackPush(b);
            }
		});
	}
}
