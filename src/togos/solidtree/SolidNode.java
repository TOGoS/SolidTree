package togos.solidtree;

import java.util.Arrays;

public class SolidNode
{
	public static final byte[] EMPTY_SUBNODE_LIST = new byte[0];
	public static final SolidNode EMPTY = new SolidNode( VolumetricMaterial.SPACE );
	private static final byte[] INC = new byte[256];
	static {
		for( int i=0; i<256; ++i ) INC[i] = (byte)i;
	}
	
	public final int divX, divY, divZ;
	public final SolidNodePalette palette;
	private final byte[] subNodes;
	public final GeneralMaterial material; 
	
	public SolidNode( GeneralMaterial material, int divX, int divY, int divZ, SolidNodePalette palette, byte[] subNodes ) {
		assert subNodes.length >= divX * divY * divZ;
		assert palette.containsAll(subNodes, divX*divY*divZ);
		this.material = material;
		this.divX = divX;
		this.divY = divY;
		this.divZ = divZ;
		this.palette = palette;
		this.subNodes = (subNodes.length == 0 || subNodes == INC) ? subNodes : Arrays.copyOf(subNodes, subNodes.length);
	}
	
	public SolidNode( GeneralMaterial material, int divX, int divY, int divZ, SolidNode[] subNodes ) {
		this( material, divX, divY, divZ, new SolidNodePalette(subNodes), INC );
	}
	
	public SolidNode( GeneralMaterial m ) {
		this( m, 0, 0, 0, SolidNodePalette.EMPTY, EMPTY_SUBNODE_LIST );
	}
	
	protected int subNodeIndex( int x, int y, int z ) {
		assert x >= 0; assert x < divX;
		assert y >= 0; assert y < divY;
		assert z >= 0; assert z < divZ;
		return z * divX * divY + y * divX + x;
	}
	
	public SolidNode subNode( int idx ) {
		return palette.get(subNodes[idx]&0xFF); 
	}
	
	public SolidNode subNode( int x, int y, int z ) {
		return palette.get(subNodes[subNodeIndex(x,y,z)]&0xFF); 
	}
	
	public boolean isSubdivided() {
		return divX * divY * divZ > 1;
	}
}
