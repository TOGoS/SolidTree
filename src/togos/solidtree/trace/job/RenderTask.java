package togos.solidtree.trace.job;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import togos.solidtree.trace.RenderResultChannel;
import togos.solidtree.trace.Scene;

public class RenderTask implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	public final String taskId;
	/** How many data points are we generating? */
	public final int imageDataSize;
	/** What are we drawing? */
	public final Scene scene;
	/**
	 * What order do we generate the data, and from what perspective?
	 * A separate result will be generated for each step of the outer iterator.
	 * No result data should be carried over between outer iterations.
	 */
	public final Iterator<? extends PixelRayIterator> pixelRayIteratorIterator;
	/** What data do we want back? */
	public final Set<RenderResultChannel> channels;
	
	public RenderTask(
		int imageDataSize,
		Scene scene,
		Iterator<? extends PixelRayIterator> pixelRayIteratorIterator,
		Set<RenderResultChannel> channels
	) {
		Random r = new Random();
		this.taskId = "task-"+System.currentTimeMillis()+"-"+r.nextLong()+"-"+r.nextLong();
		this.imageDataSize = imageDataSize;
		this.scene = scene;
		this.pixelRayIteratorIterator = pixelRayIteratorIterator;
		this.channels = channels;
	}
}
