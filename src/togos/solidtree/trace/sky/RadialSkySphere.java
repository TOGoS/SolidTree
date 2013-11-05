package togos.solidtree.trace.sky;

import togos.solidtree.matrix.Vector3D;

public class RadialSkySphere implements SkySphere
{
	final double cx, cy, cz;
	final double r, g, b;
	final double distPower;
	
	public RadialSkySphere( double cx, double cy, double cz, double distPower, double r, double g, double b ) {
		this.cx = cx; this.cy = cy; this.cz = cz;
		this.r = r; this.g = g; this.b = b;
		this.distPower = distPower;
	}
	
	@Override public void getSkyColor(Vector3D direction, Vector3D color) {
		double dx = (direction.x - cx)/2;
		double dy = (direction.y - cy)/2;
		double dz = (direction.z - cz)/2;
		double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
		double factor = Math.pow(1 - dist, distPower);
		color.x = factor * r;
		color.y = factor * g;
		color.z = factor * b;
	}
}
