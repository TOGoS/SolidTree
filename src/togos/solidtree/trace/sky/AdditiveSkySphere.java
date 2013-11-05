package togos.solidtree.trace.sky;

import togos.solidtree.matrix.Vector3D;

public class AdditiveSkySphere implements SkySphere
{
	public final SkySphere[] components;
	
	public AdditiveSkySphere( SkySphere...components ) {
		this.components = components;
	}
	
	@Override public void getSkyColor(Vector3D direction, Vector3D color) {
		double r = 0, g = 0, b = 0;
		for( SkySphere s : components ) {
			s.getSkyColor(direction, color);
			r += color.x; g += color.y; b += color.z;
		}
		color.x = r; color.y = g; color.z = b; 
	}
}
