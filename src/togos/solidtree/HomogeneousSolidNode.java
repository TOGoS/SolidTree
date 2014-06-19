package togos.solidtree;

import togos.solidtree.trace.TraceNode.DensityFunction;

public class HomogeneousSolidNode implements SolidNode
{
	public static final HomogeneousSolidNode EMPTY = new HomogeneousSolidNode( StandardMaterial.SPACE );
	
	protected final GeneralMaterial material;
	
	public HomogeneousSolidNode( GeneralMaterial m ) {
		this.material = m;
	}
	
	@Override public Type getType() { return Type.HOMOGENEOUS; }
	
	@Override public GeneralMaterial getHomogeneousMaterial() { return material; }
	
	protected static final String noDivsText = "Homogeneous nodes do not have divisions.";
	@Override public int getDivX() { throw new UnsupportedOperationException(noDivsText); }
	@Override public int getDivY() { throw new UnsupportedOperationException(noDivsText); }
	@Override public int getDivZ() { throw new UnsupportedOperationException(noDivsText); }
	@Override public SolidNode subNode(int idx) { throw new UnsupportedOperationException(noDivsText); }
	@Override public SolidNode subNode(int x, int y, int z) { throw new UnsupportedOperationException(noDivsText); }
	@Override public DensityFunction getDensityFunction() { throw new UnsupportedOperationException(noDivsText); }
}
