package togos.solidtree.trace.job;

public interface TaskServer
{
	public RenderTask takeTask();
	public void putTaskResult( RenderTask task, RenderResult result );
}
