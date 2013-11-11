package togos.solidtree.trace.job.inet;

import java.io.Serializable;

import togos.solidtree.trace.job.RenderResult;

public class TaskResult implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	public final String taskId;
	public final RenderResult result;
	
	public TaskResult( String taskId, RenderResult res ) {
		this.taskId = taskId;
		this.result = res;
	}
}
