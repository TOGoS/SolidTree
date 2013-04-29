package togos.solidtree;

import java.util.Arrays;

public class SolidNodePalette
{
	public static final SolidNodePalette EMPTY = new SolidNodePalette(new SolidNode[0]); 
	
	private final SolidNode[] nodes;
	
	public SolidNodePalette( SolidNode[] nodes ) {
		for( int i=0; i<nodes.length; ++i ) {
			assert nodes[i] != null;
		}
		this.nodes = Arrays.copyOf(nodes, nodes.length);
	}
	
	public SolidNode get( int idx ) {
		assert idx >= 0;
		assert idx < nodes.length;
		return nodes[idx];
	}

	public boolean containsAll( byte[] nodeIndexes, int count ) {
		for( int i=0; i<count; ++i ) {
			if( (nodeIndexes[i]&0xFF) >= nodes.length ) return false;
		}
		return true;
	}
}
