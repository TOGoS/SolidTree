package togos.solidtree.trace.job;

import java.util.HashMap;

import togos.solidtree.trace.RenderResultChannel;
import togos.solidtree.trace.Tracer;

public class LocalRenderWorker implements RenderResultIterator
{
	protected final Tracer tracer;
	protected final RenderTask task;
	protected final int batchSize;
	protected boolean run = true;
	
	public LocalRenderWorker( RenderTask task, int batchSize ) {
		this.task = task;
		this.batchSize = batchSize;
		
		this.tracer = new Tracer();
		tracer.mode = task.traceMode;
		tracer.setScene(task.scene);
	}
	
	@Override public void close() {
		run = false;
	}
	
	@Override public RenderResult nextResult() {
		if( !run || !task.pixelRayIteratorIterator.hasNext() ) return null;
		
		final PixelRayIterator pri = task.pixelRayIteratorIterator.next();
		int imageDataSize = pri.getImageDataSize();
		final PixelRayBuffer prb = new PixelRayBuffer(batchSize);
		
		final float[] r = new float[imageDataSize];
		final float[] g = new float[imageDataSize];
		final float[] b = new float[imageDataSize];
		final float[] e = new float[imageDataSize];
		
		long totalSamples = 0;
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
			
			++totalSamples;
		}
		
		HashMap<RenderResultChannel,Object> rrcs = new HashMap<RenderResultChannel,Object>();
		rrcs.put(RenderResultChannel.RED, r);
		rrcs.put(RenderResultChannel.GREEN, g);
		rrcs.put(RenderResultChannel.BLUE, b);
		rrcs.put(RenderResultChannel.EXPOSURE, e);
		return new RenderResult(totalSamples, rrcs);
	}
}
