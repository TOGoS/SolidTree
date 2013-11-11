package togos.solidtree.trace.job;

import java.io.IOException;

public interface TaskServer
{
	public RenderTask takeTask() throws IOException, InterruptedException;
	public void putTaskResult( String taskId, RenderResult result ) throws IOException, InterruptedException;
}
