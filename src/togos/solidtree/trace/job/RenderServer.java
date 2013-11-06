package togos.solidtree.trace.job;

public interface RenderServer
{
	/**
	 * Number of workers we can start before we start losing sample rate.
	 * Probably corresponds the number of unused CPU cores on the server.
	 */
	public int getAvailableCapacity();
	public RenderWorker start( RenderTask t );
}
