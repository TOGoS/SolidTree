package togos.solidtree;

public class NodeRoot
{
	public final SolidNode node;
	public final double x0, y0, z0, x1, y1, z1;
	
	public NodeRoot( SolidNode node, double w, double h, double d ) {
		this.node = node;
		this.x0 = -w/2; this.y0 = -h/2; this.z0 = -d/2;
		this.x1 = +w/2; this.y1 = +h/2; this.z1 = +d/2;
	}
	
	public NodeRoot( SolidNode node, double size ) {
		this( node, size, size, size );
	}
}
