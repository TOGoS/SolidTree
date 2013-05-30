package togos.solidtree.shape;

public class AACube implements Shape
{
	final double x0, y0, z0, x1, y1, z1;
	
	public AACube( double x0, double y0, double z0, double x1, double y1, double z1 ) {
		this.x0 = x0; this.y0 = y0; this.z0 = z0;
		this.x1 = x1; this.y1 = y1; this.z1 = z1;
	}
	
	public AACube( double cx, double cy, double cz, double radius ) {
		this( cx - radius, cy - radius, cz - radius, 
		      cx + radius, cy + radius, cz + radius );
	}
	
	@Override
    public Containment contains( double minX, double minY, double minZ, double maxX, double maxY, double maxZ ) {
		if(
			maxX <= x0 ||
			maxY <= y0 ||
			maxZ <= z0 ||
			minX >= x1 ||
			minY >= y1 ||
			minZ >= z1
		) return Containment.NONE;
		
		if(
			maxX <= x1 &&
			maxY <= y1 &&
			maxZ <= z1 &&
			minX >= x0 &&
			minY >= y0 &&
			minZ >= z0
		) return Containment.ALL;
		
		return Containment.SOME;
    }
}
