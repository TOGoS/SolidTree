package togos.solidtree.trace.job;

import java.io.Serializable;
import java.util.Map;

import togos.solidtree.trace.RenderResultChannel;

public class RenderResult implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	/** Total number of samples taken */
	public final long sampleCount;
	public final Map<RenderResultChannel,Object> data;
	
	public RenderResult( long sampleCount, Map<RenderResultChannel,Object> data ) {
		this.sampleCount = sampleCount;
		this.data = data;
	}
}
