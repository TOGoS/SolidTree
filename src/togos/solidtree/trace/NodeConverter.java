package togos.solidtree.trace;

import java.util.WeakHashMap;

import togos.solidtree.PathTraceMaterial;
import togos.solidtree.SolidNode;

public class NodeConverter
{
	protected final WeakHashMap<TraceNode,TraceNode> canonicalTraceNodes = new WeakHashMap<TraceNode,TraceNode>(); 
	protected final WeakHashMap<PathTraceMaterial,TraceNode> materialTraceNodeCache = new WeakHashMap<PathTraceMaterial,TraceNode>();
	protected final WeakHashMap<SolidNode,TraceNode> solidTraceNodeCache = new WeakHashMap<SolidNode,TraceNode>();
	/** Maps to FALSE when not homogeneous, material otherwise. */
	protected final WeakHashMap<SolidNode,Object> nodeHomogeneityCache = new WeakHashMap<SolidNode,Object>();
	
	public void reset() {
		materialTraceNodeCache.clear();
		solidTraceNodeCache.clear();
	}
	
	protected TraceNode intern( TraceNode tn ) {
		TraceNode i = canonicalTraceNodes.get(tn);
		if( i != null ) return i;
		
		canonicalTraceNodes.put(tn, tn);
		return tn;
	}
	
	protected TraceNode solidTraceNode( PathTraceMaterial mat ) {
		return intern(new TraceNode(mat));
	}
	protected TraceNode dividedTraceNode( int div, TraceNode subNodeA, double splitPoint, TraceNode subNodeB ) {
		return intern(new TraceNode(div, splitPoint, subNodeA, subNodeB));
	}
	
	protected TraceNode traceNodeForMaterial(PathTraceMaterial mat) {
		TraceNode tn = materialTraceNodeCache.get(mat);
		if( tn == null ) {
			materialTraceNodeCache.put(mat, tn = solidTraceNode(mat));
		}
		return tn;
	}
	
	protected PathTraceMaterial nodeIsHomogeneous( SolidNode sn ) {
		if( sn.getType() == SolidNode.Type.HOMOGENEOUS ) return sn.getHomogeneousMaterial();
		
		Object o = nodeHomogeneityCache.get(sn);
		if( o == null ) {
			PathTraceMaterial m = regionIsHomogeneous( sn, 0, 0, 0, sn.getDivX(), sn.getDivY(), sn.getDivZ() );
			nodeHomogeneityCache.put(sn, o = (m == null ? Boolean.FALSE : m));
		}
		return o == Boolean.FALSE ? null : (PathTraceMaterial)o;
	}
	
	protected PathTraceMaterial regionIsHomogeneous( SolidNode sn, int rx, int ry, int rz, int rw, int rh, int rd ) {
		SolidNode subNode = sn.subNode(rx, ry, rz);
		
		PathTraceMaterial mat0 = nodeIsHomogeneous(subNode);
		if( mat0 == null ) return null;
		
		for( int dz=0; dz<rd; ++dz ) for( int dy=0; dy<rh; ++dy ) for( int dx=0; dx<rw; ++dx ) {
			PathTraceMaterial matN = nodeIsHomogeneous(sn.subNode(rx+dx, ry+dy, rz+dz));
			if( matN != mat0 ) return null;
		}
		
		return mat0;
	}
	
	/*
	protected boolean subdividable( int dim ) {
		assert dim > 0;
		while( (dim & 1) == 0 ) dim >>= 1;
		return dim == 1;
	}
	protected void ensureSubdividable( int dim, String dimName ) {
		if( !subdividable(dim) ) throw new RuntimeException(
			"Cannot subdivide node because '"+dimName+"' division is non-power-of-2: "+dim);
	}
	protected void ensureSubdividable( SolidNode sn ) {
		if( sn.divX != 1 ) ensureSubdividable(sn.divX, "X");
		if( sn.divY != 1 ) ensureSubdividable(sn.divX, "Y");
		if( sn.divZ != 1 ) ensureSubdividable(sn.divX, "Z");
	}
	*/
	
	protected TraceNode regionToTraceNode( SolidNode sn, int rx, int ry, int rz, int rw, int rh, int rd ) {
		assert rw > 0;
		assert rh > 0;
		assert rd > 0;
		
		PathTraceMaterial mat;
		if( (mat = regionIsHomogeneous(sn, rx, ry, rz, rw, rh, rd)) != null ) {
			return traceNodeForMaterial(mat);
		}
		
		if( rw == 1 && rh == 1 && rd == 1 ) return toTraceNode(sn.subNode(rx, ry, rz));
		
		int maxDim = rw > rh ? rw : rh;
		maxDim = maxDim > rd ? maxDim : rd;
		
		int mostHomogeneousDim = 0;
		int maxHomogeneity = 0;
		// For each dimension, find number of layers that are internally homogeneous,
		// weighted by the number of cells they contain
		if( rw != 1 ) {
			int hg = 0;
			for( int x=0; x<rw; ++x ) 	if( regionIsHomogeneous(sn, rx+x, ry, rz, 1, rh, rd) != null ) {
				hg += rh*rd;
			}
			hg = hg * maxDim / rw;
			if( hg >= maxHomogeneity ) {
				mostHomogeneousDim = TraceNode.DIV_X;
				maxHomogeneity = hg;
			}
		}
		if( rd != 1 ) {
			int hg = 0;
			for( int z=0; z<rd; ++z ) 	if( regionIsHomogeneous(sn, rx, ry, rz+z, rw, rh, 1) != null ) {
				hg += rw*rh;
			}
			hg = hg * maxDim / rd;
			if( hg >= maxHomogeneity ) {
				mostHomogeneousDim = TraceNode.DIV_Z;
				maxHomogeneity = hg;
			}
		}
		if( rh != 1 ) {
			int hg = 0;
			for( int y=0; y<rh; ++y ) 	if( regionIsHomogeneous(sn, rx, ry+y, rz, rw, 1, rd) != null ) {
				hg += rw*rd;
			}
			hg = hg * maxDim / rh;
			if( hg >= maxHomogeneity ) {
				mostHomogeneousDim = TraceNode.DIV_Y;
				maxHomogeneity = hg;
			}
		}
		
		switch( mostHomogeneousDim ) {
		case TraceNode.DIV_X:
			assert rw >= 2;
			return dividedTraceNode(
				TraceNode.DIV_X,
				regionToTraceNode(sn, rx     , ry, rz,     rw/2 , rh, rd),
				(double)(rw/2)/rw,
				regionToTraceNode(sn, rx+rw/2, ry, rz, (rw-rw/2), rh, rd)
			);
		case TraceNode.DIV_Y:
			assert rh >= 2;
			return dividedTraceNode(
				TraceNode.DIV_Y,
				regionToTraceNode(sn, rx, ry     , rz, rw,     rh/2 , rd),
				(double)(rh/2)/rh,
				regionToTraceNode(sn, rx, ry+rh/2, rz, rw, (rh-rh/2), rd)
			);
		case TraceNode.DIV_Z:
			assert rd >= 2;
			return dividedTraceNode(
				TraceNode.DIV_Z,
				regionToTraceNode(sn, rx, ry, rz     , rw, rh,     rd/2 ),
				(double)(rd/2)/rd,
				regionToTraceNode(sn, rx, ry, rz+rd/2, rw, rh, (rd-rd/2))
			);
		default:
			throw new RuntimeException("Bad direction for most homogeneous division direction: "+mostHomogeneousDim);
		}
	}
	
	protected TraceNode _toTraceNode( SolidNode sn ) {
		PathTraceMaterial mat = nodeIsHomogeneous(sn);
		if( mat != null ) return traceNodeForMaterial(mat);
		
		//ensureSubdividable(sn);
		assert sn.getType() != SolidNode.Type.HOMOGENEOUS;
		
		return regionToTraceNode( sn, 0, 0, 0, sn.getDivX(), sn.getDivY(), sn.getDivZ() );
	}
	
	public TraceNode toTraceNode( SolidNode sn ) {
		TraceNode tn = solidTraceNodeCache.get(sn);
		if( tn == null ) {
			solidTraceNodeCache.put(sn, tn = _toTraceNode(sn));
		}
		return tn;
	}
}
