package togos.solidtree.trace.job.inet;

import java.io.Serializable;

import togos.solidtree.trace.job.RenderTask;

public class StopTask implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	public final RenderTask task;
	
	public StopTask( RenderTask task ) {
		this.task = task;
	}
}
