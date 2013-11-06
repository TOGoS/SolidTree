package togos.solidtree.trace.job;

import java.util.Iterator;
import java.util.Set;

import togos.solidtree.NodeRoot;
import togos.solidtree.trace.RenderResultChannel;

public class RenderTask
{
	/** How many data points are we generating? */
	public final int vectorSize;
	/** What are we drawing? */
	public final NodeRoot nodeRoot;
	/**
	 * What order do we generate the data, and from what perspective?
	 * A separate result will be generated for each step of the outer iterator.
	 * No result data should be carried over between outer iterations.
	 */
	public final Iterator<PixelRayIterator> pixelRayIteratorIterator;
	/** What data do we want back? */
	public final Set<RenderResultChannel> channels;
	
	public RenderTask(
		int vectorSize,
		NodeRoot nodeRoot,
		Iterator<PixelRayIterator> pixelRayIteratorIterator,
		Set<RenderResultChannel> channels
	) {
		this.vectorSize = vectorSize;
		this.nodeRoot = nodeRoot;
		this.pixelRayIteratorIterator = pixelRayIteratorIterator;
		this.channels = channels;
	}
}