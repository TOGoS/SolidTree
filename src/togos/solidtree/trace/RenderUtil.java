package togos.solidtree.trace;

import togos.hdrutil.HDRExposure;
import togos.solidtree.trace.job.RenderResult;

public class RenderUtil
{
	public static HDRExposure toHdrExposure( RenderResult renderResult, int imageWidth, int imageHeight ) {
		return new HDRExposure(
			imageWidth, imageHeight,
			(float[])renderResult.data.get(RenderResultChannel.RED),
			(float[])renderResult.data.get(RenderResultChannel.GREEN),
			(float[])renderResult.data.get(RenderResultChannel.BLUE),
			(float[])renderResult.data.get(RenderResultChannel.EXPOSURE)
		);
	}
}
