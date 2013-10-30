package togos.solidtree;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class SolidNode
{
	public static final byte[] EMPTY_SUBNODE_LIST = new byte[0];
	public static final SolidNode EMPTY = new SolidNode( StandardMaterial.SPACE );
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
	
	public static SolidNode build( GeneralMaterial material, int divX, int divY, int divZ, SolidNode[] subNodes ) {
		final int size = divX*divY*divZ;
		assert subNodes.length >= size;
		final HashMap<SolidNode,Integer> nodeIndexes = new HashMap<SolidNode,Integer>();
		byte[] data = new byte[divX*divY*divZ];
		int nextIndex = 0;
		for( int i=0; i<size; ++i ) {
			Integer index = nodeIndexes.get(subNodes[i]);
			if( index == null ) {
				index = nextIndex++;
				nodeIndexes.put(subNodes[i], index);
			}
			if( index.intValue() > 255 ) {
				throw new RuntimeException("Too many unique SolidNodes!");
			}
			data[i] = index.byteValue();
		}
		SolidNode[] paletteEntries = new SolidNode[nextIndex];
		for( Map.Entry<SolidNode, Integer> nodeIndex : nodeIndexes.entrySet() ) {
			paletteEntries[nodeIndex.getValue().intValue()] = nodeIndex.getKey();
		}
		return new SolidNode( material, divX, divY, divZ, new SolidNodePalette(paletteEntries), data );
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
