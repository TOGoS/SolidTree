package togos.solidtree.trace.job;

import java.io.IOException;

public interface RenderServer
{
	public RenderResultIterator start( RenderTask t ) throws IOException, InterruptedException;
}
