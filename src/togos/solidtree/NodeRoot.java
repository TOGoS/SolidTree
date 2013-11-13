package togos.solidtree;

import java.io.Serializable;

public class NodeRoot<T> implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	public final T node;
	public final double x0, y0, z0, x1, y1, z1;
	
	public NodeRoot( T node, double x0, double y0, double z0, double x1, double y1, double z1 ) {
		this.node = node;
		this.x0 = x0; this.y0 = y0; this.z0 = z0;
		this.x1 = x1; this.y1 = y1; this.z1 = z1;
	}
	
	public NodeRoot( T node, double w, double h, double d ) {
		this( node, -w/2, -h/2, -d/2, +w/2, +h/2, +d/2 );
	}
	
	public NodeRoot( T node, double size ) {
		this( node, size, size, size );
	}
}
