package togos.solidtree.trace;

import togos.solidtree.NodeRoot;
import togos.solidtree.trace.sky.SkySphere;

public class Scene
{
	public final NodeRoot nodeRoot;
	public final SkySphere sky;
	
	public Scene( NodeRoot nodeRoot, SkySphere sky ) {
		this.nodeRoot = nodeRoot;
		this.sky = sky;
	}
}
