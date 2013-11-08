package togos.solidtree.trace.job.inet;

import java.io.Serializable;

import togos.solidtree.trace.job.RenderResult;
import togos.solidtree.trace.job.RenderTask;

public class TaskResult implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	public final RenderTask task;
	public final RenderResult res;
	
	public TaskResult( RenderTask task, RenderResult res ) {
		this.task = task;
		this.res = res;
	}
}
