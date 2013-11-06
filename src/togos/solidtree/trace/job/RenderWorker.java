package togos.solidtree.trace.job;

import java.io.Closeable;
import java.util.Map;

import togos.solidtree.trace.RenderResultChannel;

public interface RenderWorker extends Closeable
{
	/** True if takeResult() will definitely not block */
	public boolean resultAvailable();
	/**
	 * Returns the next result.
	 * May block.
	 * Returns null if there is no more work to do.
	 */
	public Map<RenderResultChannel,?> takeResult();
}
