package togos.solidtree.trace.job;

import java.util.Map;

import togos.solidtree.trace.RenderResultChannel;

public class RenderResult
{
	public final Map<RenderResultChannel,Object> data;
	
	public RenderResult( Map<RenderResultChannel,Object> data ) {
		this.data = data;
	}
}
