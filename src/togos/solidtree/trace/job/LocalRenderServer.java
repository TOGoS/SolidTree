package togos.solidtree.trace.job;

public class LocalRenderServer implements RenderServer
{
	final int batchSize = 1024;
	
	@Override public LocalRenderWorker start(RenderTask t) {
		return new LocalRenderWorker(t, batchSize);
	}
}
