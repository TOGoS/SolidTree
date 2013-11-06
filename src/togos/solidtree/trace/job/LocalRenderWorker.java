package togos.solidtree.trace.job;

import java.util.HashMap;
import java.util.Map;

import togos.solidtree.trace.RenderResultChannel;
import togos.solidtree.trace.Tracer;

public class LocalRenderWorker implements RenderWorker
{
	@Override public boolean resultAvailable() { return false; }
	@Override public void close() { }
	
	final Tracer tracer;
	final RenderTask task;
	final int bufferSize;
	
	public LocalRenderWorker( Tracer tracer, RenderTask task, int bufferSize ) {
		this.tracer = tracer;
		this.task = task;
		this.bufferSize = bufferSize;
	}
	
	@Override public Map<RenderResultChannel, ?> takeResult() {
		if( !task.pixelRayIteratorIterator.hasNext() ) return null;
		
		final PixelRayIterator pri = task.pixelRayIteratorIterator.next();
		final PixelRayBuffer prb = new PixelRayBuffer(bufferSize);
		
		final float[] r = new float[prb.maxVectorSize];
		final float[] g = new float[prb.maxVectorSize];
		final float[] b = new float[prb.maxVectorSize];
		final float[] e = new float[prb.maxVectorSize];
		
		while( pri.next(prb) ) for( int j=prb.vectorSize-1; j>=0; --j ) {
			int i = prb.index[j];
			if( i < 0 || i > prb.vectorSize ) {
				throw new ArrayIndexOutOfBoundsException("Index given by PixelRayIterator ("+i+") is out of bounds (0.."+(bufferSize-1)+")");
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
