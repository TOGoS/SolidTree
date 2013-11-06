package togos.solidtree.trace.job;

import java.util.HashMap;
import java.util.Map;

import togos.solidtree.trace.RenderResultChannel;
import togos.solidtree.trace.Tracer;

public class LocalRenderWorker implements RenderWorker
{
	@Override public void close() { }
	
	final Tracer tracer;
	final RenderTask task;
	final int imageDataSize;
	final int batchSize;
	
	public LocalRenderWorker( RenderTask task, int imageDataSize, int batchSize ) {
		this.task = task;
		this.imageDataSize = imageDataSize;
		this.batchSize = batchSize;
		
		this.tracer = new Tracer();
		tracer.setRoot(task.nodeRoot);
	}
	
	@Override public Map<RenderResultChannel, Object> nextResult() {
		if( !task.pixelRayIteratorIterator.hasNext() ) return null;
		
		final PixelRayIterator pri = task.pixelRayIteratorIterator.next();
		final PixelRayBuffer prb = new PixelRayBuffer(batchSize);
		
		final float[] r = new float[imageDataSize];
		final float[] g = new float[imageDataSize];
		final float[] b = new float[imageDataSize];
		final float[] e = new float[imageDataSize];
		
		while( pri.next(prb) ) for( int j=prb.vectorSize-1; j>=0; --j ) {
			int i = prb.index[j];
			if( i < 0 || i > imageDataSize ) {
				throw new ArrayIndexOutOfBoundsException("Index given by PixelRayIterator ("+i+") is out of bounds (0.."+(imageDataSize-1)+")");
			}
			tracer.trace( prb.ox[j], prb.oy[j], prb.oz[j], prb.dx[j], prb.dy[j], prb.dz[j] );
			r[i] += tracer.red;
			g[i] += tracer.green;
			b[i] += tracer.blue;
			e[i]++;
		}
		
		HashMap<RenderResultChannel,Object> result = new HashMap<RenderResultChannel,Object>();
		result.put(RenderResultChannel.RED, r);
		result.put(RenderResultChannel.GREEN, g);
		result.put(RenderResultChannel.BLUE, b);
		result.put(RenderResultChannel.EXPOSURE, e);
		return result;
	}
}
