package togos.solidtree;

import java.util.WeakHashMap;

import togos.lang.ScriptError;
import togos.lang.SourceLocation;

// TODO: rename since it's not all about nodes.
public class NodeConverter
{
	protected static final WeakHashMap<Object,SolidNode> nodesFromOtherThings = new WeakHashMap<Object,SolidNode>();
	
	public static GeneralMaterial toVolumetricMaterial( Object thing, SourceLocation sLoc ) throws ScriptError {
		if( thing == null ) return null;
		if( thing instanceof GeneralMaterial ) {
			return (GeneralMaterial)thing;
		}
		if( thing instanceof SurfaceMaterial ) {
			return StandardMaterial.opaque( (SurfaceMaterial)thing );
		}
		throw new ScriptError("Needed a material, but found a "+thing.getClass()+", which cannot be converted to a a SolidNode", sLoc);
	}
	
	public static synchronized SolidNode from( Object thing, SourceLocation sLoc ) throws ScriptError {
		if( thing instanceof SolidNode ) return (SolidNode)thing;
		SolidNode n = nodesFromOtherThings.get(thing);
		if( n != null ) return n;
		if( thing instanceof SurfaceMaterial ) {
			thing = StandardMaterial.opaque( (SurfaceMaterial)thing );
		}
		if( thing instanceof GeneralMaterial ) {
			n = new HomogeneousSolidNode( (GeneralMaterial)thing );
		}
		if( n == null ) throw new ScriptError("Needed a SolidNode, but found a "+thing.getClass()+", which cannot be converted to a a SolidNode", sLoc);
		nodesFromOtherThings.put(thing, n);
		return n;
	}
}
