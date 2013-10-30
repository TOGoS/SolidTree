package togos.solidtree.trace.sky;

import togos.solidtree.trace.Vector3D;


public class CrappySkySphere
	implements SkySphere
{
	double intensity = 0.1;
	
	double clamp( double min, double v, double max ) {
		return min > v ? min : v < max ? v : max;
	}
	
	@Override public void getSkyColor(Vector3D direction, Vector3D color) {
		double r, g, b;
		if( direction.y < 0 ) {
			// r = g = b = 0;
			r = g = 0.1;
			b = 0.4;
		} else {
			r = g = Math.max( 0, 0.1 * (direction.x+1) / direction.y );
			b = 0.2 * (direction.x+1) / direction.y;
		}
		color.set(r*intensity, g*intensity, b*intensity);
	}
}