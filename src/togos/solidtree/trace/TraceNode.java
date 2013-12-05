package togos.solidtree.trace;

import java.io.Serializable;

import togos.solidtree.PathTraceMaterial;
import togos.solidtree.StandardMaterial;

public final class TraceNode implements Serializable
{
	interface DensityFunction {
		public double getMaxGradient();
		public double apply(
			double x, double y, double z
		);
	}
	
	private static final long serialVersionUID = 2L;
	
	public static final TraceNode EMPTY = new TraceNode( StandardMaterial.SPACE );
	
	public static final int DIV_NONE = 0; // Solid!
	public static final int DIV_X = 1;
	public static final int DIV_Y = 2;
	public static final int DIV_Z = 3;
	public static final int DIV_FUNC_GLOBAL = 4;
	public static final int DIV_FUNC_LOCAL = 5;
	
	public final int division;
	public final int hashCode;
	
	// A union would be nice, here.
	
	// For when div = DIV_NONE:
	public final PathTraceMaterial material;
	
	// For when div = DIV_FUNC:
	public final DensityFunction splitFunc;
	
	// For when div = DIV_X/Y/Z:
	public final double splitPoint;
	
	// For when div != DIV_NONE:
	public final TraceNode subNodeA;
	public final TraceNode subNodeB;
	
	private TraceNode( int div, PathTraceMaterial material, DensityFunction func, double splitPoint, TraceNode subNodeA, TraceNode subNodeB ) {
		// Make sure we never create a node of 2 identical homogeneous subnodes!
		switch( div ) {
		case DIV_NONE:
			assert material != null;
			assert func     == null;
			assert subNodeA == null;
			assert subNodeB == null;
			assert Double.isNaN(splitPoint);
			break;
		case DIV_FUNC_GLOBAL: case DIV_FUNC_LOCAL:
			assert material == null;
			assert func     != null;
			assert subNodeA != null;
			assert subNodeB != null;
			assert Double.isNaN(splitPoint);
			break;
		case DIV_X: case DIV_Y: case DIV_Z:
			// Make sure the split is necessary:
			assert subNodeA.division != DIV_NONE || subNodeB.division != DIV_NONE || !subNodeA.material.equals(subNodeB.material);
			
			assert material == null;
			assert subNodeA != null;
			assert subNodeB != null;
			assert !Double.isNaN(splitPoint);
			assert splitPoint > 0;
			assert splitPoint < 1;
		}
		
		this.division = div;
		this.splitFunc = func;
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
	
	public TraceNode( int div, DensityFunction func, TraceNode subNodeA, TraceNode subNodeB ) {
		this( div, null, func, Double.NaN, subNodeA, subNodeB );
	}
	
	public TraceNode( PathTraceMaterial material ) {
		this( DIV_NONE, material, null, Double.NaN, null, null );
	}
	
	public TraceNode( int div, double splitPoint, TraceNode subNodeA, TraceNode subNodeB ) {
		this( div, null, null, splitPoint, subNodeA, subNodeB );
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
