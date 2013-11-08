package togos.solidtree.trace.job;

import java.io.Closeable;

public interface RenderWorker extends Closeable
{
	/**
	 * Returns the next result.
	 * May block.
	 * Returns null if there is no more work to do.
	 */
	public RenderResult nextResult();
}
