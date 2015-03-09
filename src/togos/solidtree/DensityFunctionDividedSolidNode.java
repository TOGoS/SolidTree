package togos.solidtree;


public class DensityFunctionDividedSolidNode implements SolidNode
{
	protected final DensityFunction df;
	protected final SolidNode nodeA;
	protected final SolidNode nodeB;
	
	public DensityFunctionDividedSolidNode( DensityFunction df, SolidNode nodeA, SolidNode nodeB ) {
		this.df = df;
		this.nodeA = nodeA;
		this.nodeB = nodeB;
	}

	@Override public Type getType() { return Type.DENSITY_FUNCTION_SUBDIVIDED; }
	
	@Override public GeneralMaterial getHomogeneousMaterial() { throw new UnsupportedOperationException(); }
	
	@Override public int getDivX() { throw new UnsupportedOperationException(); }
	@Override public int getDivY() { throw new UnsupportedOperationException(); }
	@Override public int getDivZ() { throw new UnsupportedOperationException(); }

	@Override public SolidNode subNode(int idx) {
		switch( idx ) {
		case 0: return nodeA;
		case 1: return nodeB;
		default:
			throw new UnsupportedOperationException("DF subdivided sub-node index out of range: "+idx);
		}
	}

	@Override public SolidNode subNode(int x, int y, int z) {
		// actually could; but might never need to
		throw new UnsupportedOperationException();
	}

	@Override public DensityFunction getSubdivisionFunction() { return df; }
}
