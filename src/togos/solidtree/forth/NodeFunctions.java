package togos.solidtree.forth;

import java.util.Map;

import togos.lang.ScriptError;
import togos.lang.SourceLocation;
import togos.solidtree.DColor;
import togos.solidtree.GeneralMaterial;
import togos.solidtree.SolidNode;
import togos.solidtree.StandardMaterial;
import togos.solidtree.SurfaceMaterial;
import togos.solidtree.SurfaceMaterialLayer;

public class NodeFunctions
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
	
	static final StandardWordDefinition MAKE_SIMPLE_VISUAL_MATERIAL = new StandardWordDefinition() {
		// filter color
		// emission color
		// surface mirror-ness
		// opacity ( used for both surface and internal opacity )
		// index of refraction
		// -> GenericMaterial
		
		@Override public void run( Interpreter interp, SourceLocation sLoc ) throws ScriptError {
			double ior       = interp.stackPop( Number.class, sLoc ).doubleValue();
			double opacity   = interp.stackPop( Number.class, sLoc ).doubleValue();
			double mirrosity = interp.stackPop( Number.class, sLoc ).doubleValue();
			DColor emission   = interp.stackPop( DColor.class, sLoc );
			DColor filter     = interp.stackPop( DColor.class, sLoc );
			
			SurfaceMaterial surfMat = SurfaceMaterial.TRANSPARENT;
			if( opacity > 0 ) {
				surfMat = SurfaceMaterial.combine( surfMat, new SurfaceMaterialLayer(opacity, filter, emission, 0, 1, 1, 0) );
			}
			if( mirrosity > 0 ) {
				surfMat = SurfaceMaterial.combine( surfMat, new SurfaceMaterialLayer(mirrosity, filter, emission, 1, 0, 0, 0) );
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
	
	public static void register( Map<String,WordDefinition> ctx ) {
		ctx.put("make-solid-material-node", MAKE_SOLID_MATERIAL_NODE);
		ctx.put("make-simple-visual-material", MAKE_SIMPLE_VISUAL_MATERIAL);
		ctx.put("make-color", MAKE_COLOR);
	}
}
