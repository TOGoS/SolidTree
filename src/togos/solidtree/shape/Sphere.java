package togos.solidtree.shape;

public class Sphere implements Shape
{
	final double cx, cy, cz;
	final double radius;
	
	public Sphere( double cx, double cy, double cz, double radius ) {
		this.cx = cx; this.cy = cy; this.cz = cz;
		this.radius = radius;
	}
	
	protected static double distSquared( double x0, double y0, double z0, double x1, double y1, double z1 ) {
		double dx = x1 - x0;
		double dy = y1 - y0;
		double dz = z1 - z0;
		return dx*dx + dy*dy + dz*dz;
	}
	
	protected static boolean sameSide( double x0, double c, double x1 ) {
		double d0 = x0 - c;
		double d1 = x1 - c;
		return
			(d0 == 0 || d1 == 0 ) ||
			!(d0 < 0 ^ d1 < 0 );
	}
	
	protected static double min( double v1, double v2 ) {
		return v1 < v2 ? v1 : v2;
	}
	
	protected static double max( double v1, double v2 ) {
		return v1 > v2 ? v1 : v2;
	}
	
	@Override
    public Containment contains( double minX, double minY, double minZ, double maxX, double maxY, double maxZ ) {
		// Completely outside bounding box
		if(
			maxX <= cx - radius ||
			maxY <= cy - radius ||
			maxZ <= cz - radius ||
			minX >= cx + radius ||
			minY >= cy + radius ||
			minZ >= cz + radius
		) return Containment.NONE;
		
		double[] squaredCornerDistances = new double[] {
			distSquared( minX, minY, minZ, cx, cy, cz ), 
			distSquared( minX, minY, maxZ, cx, cy, cz ),
			distSquared( minX, maxY, minZ, cx, cy, cz ),
			distSquared( minX, maxY, maxZ, cx, cy, cz ),
			distSquared( maxX, minY, minZ, cx, cy, cz ),
			distSquared( maxX, minY, maxZ, cx, cy, cz ),
			distSquared( maxX, maxY, minZ, cx, cy, cz ),
			distSquared( maxX, maxY, maxZ, cx, cy, cz ),
		};
		double maxCornerDistSquared = Double.MIN_VALUE;
		double minCornerDistSquared = Double.MAX_VALUE; 
		for( double distSquared : squaredCornerDistances ) {
			maxCornerDistSquared = max( distSquared, maxCornerDistSquared );
			minCornerDistSquared = min( distSquared, minCornerDistSquared );
		}

		double radiusSquared = radius * radius;
		
		// All corners inside sphere
		if(
			maxCornerDistSquared <= radiusSquared
		) return Containment.ALL; 
		
		boolean containedInSingleCorner =  
			sameSide(minX, cx, maxX) &&
			sameSide(minY, cy, maxY) &&
			sameSide(minZ, cz, maxZ);

		// All corners in a single corner of the sphere's bounding box outside sphere
		if(
			minCornerDistSquared >= radiusSquared &&
			containedInSingleCorner
		) return Containment.NONE;
		
		// TODO: crosses center in one dimension but is outside
		// the circle -> Containment.NONE
		
		return Containment.SOME;
    }
}
