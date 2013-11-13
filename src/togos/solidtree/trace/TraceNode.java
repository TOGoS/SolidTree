package togos.solidtree.trace;

import java.io.Serializable;

import togos.solidtree.PathTraceMaterial;
import togos.solidtree.StandardMaterial;

public final class TraceNode implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	public static final TraceNode EMPTY = new TraceNode( StandardMaterial.SPACE );
	
	public static final int DIV_NONE = 0; // Solid!
	public static final int DIV_X = 1;
	public static final int DIV_Y = 2;
	public static final int DIV_Z = 3;
	
	public final int division;
	public final PathTraceMaterial material;
	public final TraceNode subNodeA;
	public final double splitPoint;
	public final TraceNode subNodeB;
	public final int hashCode;
	
	private TraceNode( int div, PathTraceMaterial material, TraceNode subNodeA, double splitPoint, TraceNode subNodeB ) {
		// Make sure we never create a node of 2 identical homogeneous subnodes!
		if( div == DIV_NONE ) {
			assert material != null && subNodeA == null && subNodeB == null && Double.isNaN(splitPoint);
		} else {
			assert subNodeA.division != DIV_NONE || subNodeB.division != DIV_NONE || !subNodeA.material.equals(subNodeB.material);
			assert material == null && subNodeA != null && subNodeB != null && !Double.isNaN(splitPoint);
			assert splitPoint > 0;
			assert splitPoint < 1;
		}
		
		this.division = div;
		this.material = material;
		this.subNodeA = subNodeA;
		this.splitPoint = splitPoint;
		this.subNodeB = subNodeB;
		
		this.hashCode =
			(division << 16) ^
			(material == null ? 0 : material.hashCode() << 4) ^
			Float.floatToIntBits((float)splitPoint) ^
			(subNodeA == null ? 0 : subNodeA.hashCode() << 8) ^
			(subNodeB == null ? 0 : subNodeB.hashCode() >> 8);

	}
	
	public TraceNode( PathTraceMaterial material ) {
		this( DIV_NONE, material, null, Double.NaN, null );
	}
	
	public TraceNode( int div, TraceNode subNodeA, double splitPoint, TraceNode subNodeB ) {
		this( div, null, subNodeA, splitPoint, subNodeB );
	}
	
	public boolean isSubdivided() {
		return division != DIV_NONE; 
	}
	
	@Override public int hashCode() {
		return hashCode;
	}
	
	protected static boolean equalsOrBothNull( Object a, Object b ) {
		if( a == null && b == null ) return true;
		if( a == null || b == null ) return false;
		return a.equals(b);
	}
	
	@Override public boolean equals( Object oth ) {
		if( !(oth instanceof TraceNode) ) return false;
		
		TraceNode tn = (TraceNode)oth;
		return
			division == tn.division &&
			equalsOrBothNull(material, tn.material) &&
			splitPoint == tn.splitPoint &&
			equalsOrBothNull(subNodeA, tn.subNodeA) &&
			equalsOrBothNull(subNodeB, tn.subNodeB);
	}
}
