package togos.solidtree.trace.sky;

import java.io.Serializable;

import togos.solidtree.DColor;
import togos.solidtree.matrix.Vector3D;

public interface SkySphere extends Serializable
{
	public void getSkyColor( Vector3D direction, DColor color );
}
