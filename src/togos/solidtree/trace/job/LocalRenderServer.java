package togos.solidtree.trace.job;

public class LocalRenderServer implements RenderServer
{
	@Override public LocalRenderWorker start(RenderTask t) {
		return new LocalRenderWorker(t, t.imageDataSize, 1024);
	}
}
