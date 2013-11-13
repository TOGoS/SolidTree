package togos.solidtree.trace;

import java.io.Serializable;

import togos.solidtree.NodeRoot;
import togos.solidtree.trace.sky.SkySphere;

public class Scene implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	public final NodeRoot<TraceNode> nodeRoot;
	public final SkySphere sky;
	
	public Scene( NodeRoot<TraceNode> nodeRoot, SkySphere sky ) {
		this.nodeRoot = nodeRoot;
		this.sky = sky;
	}
}
