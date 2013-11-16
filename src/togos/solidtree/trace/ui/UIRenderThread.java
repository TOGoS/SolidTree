package togos.solidtree.trace.ui;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import togos.hdrutil.ExposureScaler;
import togos.hdrutil.HDRExposure;
import togos.hdrutil.IncompatibleImageException;
import togos.solidtree.matrix.Matrix;
import togos.solidtree.trace.Projection;
import togos.solidtree.trace.RenderResultChannel;
import togos.solidtree.trace.RenderUtil;
import togos.solidtree.trace.Scene;
import togos.solidtree.trace.Tracer;
import togos.solidtree.trace.job.InfiniteXYOrderedPixelRayIteratorIterator;
import togos.solidtree.trace.job.LocalRenderServer;
import togos.solidtree.trace.job.RenderResult;
import togos.solidtree.trace.job.RenderResultIterator;
import togos.solidtree.trace.job.RenderServer;
import togos.solidtree.trace.job.RenderTask;

public class UIRenderThread extends Thread
{
	protected static boolean equalsOrBothNull( Object a, Object b ) {
		if( a == b ) return true;
		if( a == null || b == null ) return false;
		return a.equals(b);
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
	
	static class RenderSettings {
		final View view;
		final int imageWidth;
		final int imageHeight;
		
		public RenderSettings( View view, int imageWidth, int imageHeight ) {
			this.view = view;
			this.imageWidth = imageWidth;
			this.imageHeight = imageHeight;
		}
		
		@Override public boolean equals(Object oth) {
			if( !(oth instanceof RenderSettings) ) return false;
			
			RenderSettings os = (RenderSettings)oth;
			return
				equalsOrBothNull(view, os.view) &&
				imageWidth == os.imageWidth &&
				imageHeight == os.imageHeight;
		}
	}
	
	public volatile ExposureUpdateListener exposureUpdateListener;
	
	protected volatile boolean keepRunning = true;
	protected volatile RenderSettings settings;
	
	enum Mode {
		PREVIEW,
		FULL
	}
	
	public void setExposureUpdateListener( ExposureUpdateListener l ) {
		this.exposureUpdateListener = l;
	}
	
	public void setRenderSettings( RenderSettings s ) {
		if( equalsOrBothNull(settings, s) ) return;
		
		synchronized(this) {
			this.settings = s;
			interrupt();
		}
	}
	
	RenderServer primaryServer = new LocalRenderServer(); // TODO: replace with a distributy one
	LocalRenderServer localServer = new LocalRenderServer();
	
	static final Set<RenderResultChannel> channels = new HashSet<RenderResultChannel>();
	static {
		channels.add(RenderResultChannel.RED);
		channels.add(RenderResultChannel.GREEN);
		channels.add(RenderResultChannel.BLUE);
		channels.add(RenderResultChannel.EXPOSURE);
	}
	
	protected RenderResultIterator start(View view, RenderTask task) throws IOException, InterruptedException {
		if( view == null ) return null;
		return (view.traceMode == Tracer.Mode.QUICK ? localServer : primaryServer).start(task);
	}
	
	protected void exposureUpdated( HDRExposure exp ) {
		ExposureUpdateListener l = this.exposureUpdateListener;
		if( l != null ) l.exposureUpdated(exp);
	}
	
	protected synchronized void waitForNewInput() throws InterruptedException {
		// Since a new view will interrupt(), simply waiting should be sufficient.
		wait();
		// Should never get here!
		System.err.println("Somehow the wait() in waitForNewInput() returned.\nThrowing an InterruptedException myself!");
		throw new InterruptedException();
	}
	
	protected static HDRExposure getExposure( HDRExposure oldExposure, int w, int h, boolean clear ) {
		if( oldExposure != null && oldExposure.width == w && oldExposure.height == h ) {
			// Can re-use the object!
			if( clear ) oldExposure.clear();
			return oldExposure;
		} else if( clear ) {
			// Then just make a new one.
			return new HDRExposure(w, h);
		} else {
			// Then scale up the old one.
			return ExposureScaler.scaleTo(oldExposure, w, h);
		}
	}
	
	// Only the render thread should be updating these:
	protected RenderSettings oldSettings;
	protected RenderResultIterator renderResultIterator;
	protected HDRExposure hdrExposure;
	public void loop() throws IOException, InterruptedException {
		RenderSettings s = this.settings;
		if( s == null ) waitForNewInput();
		
		// If settings have changed, restart the workers
		if( !equalsOrBothNull(oldSettings, s) ) {
			if( renderResultIterator != null ) {
				renderResultIterator.close();
			}
			
			if( s.view == null ) {
				renderResultIterator = null;
			} else {
				RenderTask task = new RenderTask(
					s.view.traceMode, s.view.scene,
					new InfiniteXYOrderedPixelRayIteratorIterator(
						s.imageWidth, s.imageHeight, s.view.projection, s.view.cameraTransform,
						s.view.traceMode == Tracer.Mode.QUICK ? 1 : 10
					), channels);
				
				renderResultIterator = start(s.view,task);
			}
		}
		
		if( renderResultIterator == null ) waitForNewInput();
		
		boolean settingsChanged = !equalsOrBothNull(oldSettings, s); 
		boolean viewChanged = oldSettings == null || !equalsOrBothNull(oldSettings.view, s.view);
		
		if( settingsChanged ) {
			hdrExposure = getExposure(hdrExposure, s.imageWidth, s.imageHeight, viewChanged);
			exposureUpdated(hdrExposure);
		}
		
		RenderResult res = renderResultIterator.nextResult();
		if( res == null ) waitForNewInput();
		
		try {
			hdrExposure.add( RenderUtil.toHdrExposure(res, s.imageWidth, s.imageHeight) );
		} catch( IncompatibleImageException e ) {
			System.err.println("Unexpectly (because sizes should have matched) got a IncompatibleImageException");
			e.printStackTrace();
			System.err.println("Will wait 5 seconds before resuming");
			Thread.sleep(5000);
		}
		
		exposureUpdated(hdrExposure);
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
