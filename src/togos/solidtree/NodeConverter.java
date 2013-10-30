package togos.solidtree;

import java.util.WeakHashMap;

import togos.lang.ScriptError;
import togos.lang.SourceLocation;

public class NodeConverter
{
	protected static final WeakHashMap<Object,SolidNode> nodesFromOtherThings = new WeakHashMap<Object,SolidNode>();
	
	public static synchronized SolidNode from( Object thing, SourceLocation sLoc ) throws ScriptError {
		if( thing instanceof SolidNode ) return (SolidNode)thing;
		SolidNode n = nodesFromOtherThings.get(thing);
		if( n != null ) return n;
		if( thing instanceof SurfaceMaterial ) {
			thing = StandardMaterial.opaque( (SurfaceMaterial)thing );
		}
		if( thing instanceof GeneralMaterial ) {
			n = new SolidNode( (GeneralMaterial)thing );
		}
		if( n == null ) throw new ScriptError("Needed a SolidNode, but found a "+thing.getClass()+", which cannot be converted to a a SolidNode", sLoc);
		nodesFromOtherThings.put(thing, n);
		return n;

	}
}
