package togos.solidtree.trace.sky;

import togos.solidtree.DColor;
import togos.solidtree.matrix.Vector3D;

public interface SkySphere
{
	public void getSkyColor( Vector3D direction, DColor color );
}
