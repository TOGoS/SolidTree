package togos.solidtree;

import togos.lazy.Ref;


public class DensityFunctionDividedSolidNode implements SolidNode
{
	protected final DensityFunction df;
	protected final Ref<SolidNode> nodeA;
	protected final Ref<SolidNode> nodeB;
	
	public DensityFunctionDividedSolidNode( DensityFunction df, Ref<SolidNode> nodeA, Ref<SolidNode> nodeB ) {
		this.df = df;
		this.nodeA = nodeA;
		this.nodeB = nodeB;
	}

	@Override public Type getType() { return Type.DENSITY_FUNCTION_SUBDIVIDED; }
	
	@Override public GeneralMaterial getHomogeneousMaterial() { throw new UnsupportedOperationException(); }
	
	@Override public int getDivX() { throw new UnsupportedOperationException(); }
	@Override public int getDivY() { throw new UnsupportedOperationException(); }
	@Override public int getDivZ() { throw new UnsupportedOperationException(); }

	@Override public Ref<SolidNode> subNode(int idx) {
		switch( idx ) {
		case 0: return nodeA;
		case 1: return nodeB;
		default:
			throw new UnsupportedOperationException("DF subdivided sub-node index out of range: "+idx);
		}
	}

	@Override public Ref<SolidNode> subNode(int x, int y, int z) {
		// actually could; but might never need to
		throw new UnsupportedOperationException();
	}

	@Override public DensityFunction getSubdivisionFunction() { return df; }
}
