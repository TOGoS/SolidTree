package togos.solidtree.trace.sky;

import togos.solidtree.DColor;
import togos.solidtree.matrix.Vector3D;

public class AdditiveSkySphere implements SkySphere
{
	private static final long serialVersionUID = 1L;
	
	public final SkySphere[] components;
	
	public AdditiveSkySphere( SkySphere...components ) {
		this.components = components;
	}
	
	@Override public void getSkyColor(Vector3D direction, DColor color) {
		double r = 0, g = 0, b = 0;
		for( SkySphere s : components ) {
			s.getSkyColor(direction, color);
			r += color.r; g += color.g; b += color.b;
		}
		color.r = r; color.g = g; color.b = b;
	}
}
