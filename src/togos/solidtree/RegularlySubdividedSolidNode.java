package togos.solidtree;

import java.io.Serializable;
import java.util.Arrays;

import togos.lazy.Ref;


public class RegularlySubdividedSolidNode implements Serializable, SolidNode
{
	private static final long serialVersionUID = 1L;
	
	@SuppressWarnings("unchecked")
	public static final Ref<SolidNode>[] EMPTY_SUBNODE_LIST = new Ref[0];
	
	protected final int divX, divY, divZ;
	private final Ref<SolidNode>[] subNodes;
	
	public RegularlySubdividedSolidNode( int divX, int divY, int divZ, Ref<SolidNode>[] subNodes ) {
		assert subNodes.length >= divX * divY * divZ;
		this.divX = divX;
		this.divY = divY;
		this.divZ = divZ;
		this.subNodes = (subNodes.length == 0) ? EMPTY_SUBNODE_LIST : Arrays.copyOf(subNodes, divX*divY*divZ);
	}
	
	public static RegularlySubdividedSolidNode build( int divX, int divY, int divZ, Ref<SolidNode>[] subNodes ) {
		final int size = divX*divY*divZ;
		assert subNodes.length >= size;
		return new RegularlySubdividedSolidNode( divX, divY, divZ, subNodes );
	}
	
	protected int subNodeIndex( int x, int y, int z ) {
		assert x >= 0; assert x < divX;
		assert y >= 0; assert y < divY;
		assert z >= 0; assert z < divZ;
		return z * divX * divY + y * divX + x;
	}
	
	@Override public Ref<SolidNode> subNode( int idx ) {
		return subNodes[idx];
	}
	
	@Override public Ref<SolidNode> subNode( int x, int y, int z ) {
		return subNodes[subNodeIndex(x,y,z)];
	}
	
	@Override public Type getType() { return Type.REGULARLY_SUBDIVIDED; }
	
	@Override public int getDivX() { return divX; }
	@Override public int getDivY() { return divY; }
	@Override public int getDivZ() { return divZ; }
	
	@Override public GeneralMaterial getHomogeneousMaterial() {
		throw new UnsupportedOperationException("Regularly subdivided solid nodes have no homogeneous material");
	}
	
	@Override public DensityFunction getSubdivisionFunction() { throw new UnsupportedOperationException(); }
}
