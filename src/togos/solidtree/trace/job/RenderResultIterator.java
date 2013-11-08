package togos.solidtree.trace.job;

import java.io.Closeable;

public interface RenderResultIterator extends Closeable
{
	/**
	 * Returns the next result.
	 * May block.
	 * Returns null if there is no more work to do.
	 * Behavior if called after 'close()' is undefined, though some implementations may return null.
	 */
	public RenderResult nextResult();
}
