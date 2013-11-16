package togos.solidtree.trace.ui;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import togos.solidtree.matrix.Matrix;
import togos.solidtree.trace.Projection;
import togos.solidtree.trace.RenderResultChannel;
import togos.solidtree.trace.Scene;
import togos.solidtree.trace.Tracer;
import togos.solidtree.trace.job.InfiniteXYOrderedPixelRayIteratorIterator;
import togos.solidtree.trace.job.LocalRenderServer;
import togos.solidtree.trace.job.RenderResultIterator;
import togos.solidtree.trace.job.RenderServer;
import togos.solidtree.trace.job.RenderTask;

public class UIRenderThread extends Thread
{
	ExposureUpdateListener exposureUpdateListener;
	RenderTask renderTask;
	
	protected volatile boolean keepRunning = true;
	protected volatile View view;
	protected volatile int imageWidth, imageHeight;
	
	enum Mode {
		PREVIEW,
		FULL
	}
	
	/**
	 * Everything that affects the rendering except resolution. 
	 */
	static class View {
		public final Scene scene;
		public final Projection projection;
		public final Matrix cameraTransform;
		public final Tracer.Mode traceMode;
		
		public View( Scene scene, Projection projection, Matrix cameraTransform, Tracer.Mode traceMode ) {
			this.scene = scene;
			this.projection = projection;
			this.cameraTransform = cameraTransform;
			this.traceMode = traceMode;
		}
		
		@Override public boolean equals( Object oth ) {
			if( !(oth instanceof View) ) return false;
			
			View ov = (View)oth;
			return
				scene.equals(ov.scene) &&
				projection.equals(ov.projection) &&
				cameraTransform.equals(ov.cameraTransform) &&
				traceMode.equals(ov.traceMode);
		}
	}
	
	protected static boolean equalsOrBothNull( Object a, Object b ) {
		if( a == null && b == null ) return true;
		if( a == null || b == null ) return false;
		return a.equals(b);
	}
	
	public void setView( View v ) {
		if( equalsOrBothNull(view, v) ) return;
		
		this.view = v;
		interrupt();
	}
	
	/**
	 * Subject to various limitations, including that the aspect ratio can't be changed.
	 * **/
	public void setResolutionPreservingExposure( int x, int y ) {
		imageWidth = x;
		imageHeight = y;
	}
	
	protected View oldView;
	protected RenderResultIterator renderResultIterator;
	RenderServer primaryServer = new LocalRenderServer(); // TODO: replace with a distributy one
	LocalRenderServer localServer = new LocalRenderServer();
	
	static final Set<RenderResultChannel> channels = new HashSet<RenderResultChannel>();
	static {
		channels.add(RenderResultChannel.RED);
		channels.add(RenderResultChannel.GREEN);
		channels.add(RenderResultChannel.BLUE);
		channels.add(RenderResultChannel.EXPOSURE);
	}
	
	public void loop() throws IOException, InterruptedException {
		View view = this.view;
		int imageWidth = this.imageWidth;
		int imageHeight = this.imageHeight;
		
		if( !equalsOrBothNull(oldView, view) ) {
			if( renderResultIterator != null ) {
				renderResultIterator.close();
			}
			
			if( view == null ) {
				renderResultIterator = null;
			} else {
				RenderTask task = new RenderTask(
					view.traceMode, view.scene,
					new InfiniteXYOrderedPixelRayIteratorIterator(
						imageWidth, imageHeight, view.projection, view.cameraTransform,
						view.traceMode == Tracer.Mode.QUICK ? 1 : 10
					), channels);
				
				renderResultIterator = (view.traceMode == Tracer.Mode.QUICK ? localServer : primaryServer).start(task);
			}
		}
		
		// TODO: Get results, stuff into an HDRExposure, resize as necessary, send to listener
		
		if( view == null ) 	synchronized(this) { while(this.view == null) wait(); }
	}
	
	public void run() {
		while( keepRunning ) {
			try {
				try {
					loop();
				} catch( IOException e ) {
					System.err.println("Caught an IOException for some reason");
					System.err.println("I'll wait 5 seconds before resuming the loop");
					Thread.sleep(5000);
				}
			} catch( InterruptedException e ) {
				if( !keepRunning ) return;
			}
		}
	}
	
	public void halt() {
		keepRunning = false;
		interrupt();
	}
}
