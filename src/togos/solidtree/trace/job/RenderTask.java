package togos.solidtree.trace.job;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import togos.solidtree.trace.RenderResultChannel;
import togos.solidtree.trace.Scene;
import togos.solidtree.trace.Tracer;

public class RenderTask implements Serializable
{
	private static final long serialVersionUID = 2L;
	
	public final String taskId;
	public final Tracer.Mode traceMode; // Added in v2.  Maybe a 'tracer factory' would be more useful in the long term
	/** What are we drawing? */
	public final Scene scene;
	/**
	 * What order do we generate the data, and from what perspective?
	 * A separate result will be generated for each step of the outer iterator.
	 * No result data should be carried over between outer iterations.
	 * 
	 * It may be slightly hokey that a task includes an iterator directly
	 * and not a thing that creates a new iterator, but works for now. 
	 */
	public final Iterator<? extends PixelRayIterator> pixelRayIteratorIterator;
	/** What data do we want back? */
	public final Set<RenderResultChannel> channels;
	
	public RenderTask(
		Tracer.Mode traceMode,
		Scene scene,
		Iterator<? extends PixelRayIterator> pixelRayIteratorIterator,
		Set<RenderResultChannel> channels
	) {
		Random r = new Random();
		this.taskId = "task-"+System.currentTimeMillis()+"-"+r.nextLong()+"-"+r.nextLong();
		this.traceMode = traceMode;
		this.scene = scene;
		this.pixelRayIteratorIterator = pixelRayIteratorIterator;
		this.channels = channels;
	}
}
