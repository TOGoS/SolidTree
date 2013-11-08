package togos.solidtree.trace.job;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;

public class MultiRenderServer implements RenderServer
{
	Set<RenderServer> servers = new HashSet<RenderServer>();
	
	public synchronized void addServer(RenderServer rs) {
		servers.add(rs);
	}
	
	public synchronized void removeServer(RenderServer rs) {
		servers.remove(rs);
	}
	
	protected List<RenderServer> getServerListSnapshot() {
		return new ArrayList<RenderServer>(servers);
	}
	
	@Override public int getAvailableCapacity() {
		int capacity = 0;
		for( RenderServer rs : getServerListSnapshot() ) {
			capacity += rs.getAvailableCapacity();
		}
		return capacity;
	}
	
	class RenderWorkerThread extends Thread {
		protected final RenderWorker wrxr;
		protected final BlockingQueue<RenderResult> resultQueue;
		
		public RenderWorkerThread( RenderWorker wrxr, BlockingQueue<RenderResult> resultQueue ) {
			super("Render worker thread");
			this.wrxr = wrxr;
			this.resultQueue = resultQueue;
		}
		
		public void close() throws IOException {
			wrxr.close();
			this.interrupt();
		}
		
		@Override public void run() {
			try {
				RenderResult res;
				while( !interrupted() && (res = wrxr.nextResult()) != null  ) {
					resultQueue.put( res );
				}
			} catch( InterruptedException e ) {
				// Done!
			}
		}
	}
	
	@Override public RenderWorker start(RenderTask t) {
		final SynchronousQueue<RenderResult> resultQueue = new SynchronousQueue<RenderResult>(true);
		final List<RenderServer> servers = getServerListSnapshot();
		final Set<RenderWorkerThread> workerThreads = new HashSet<RenderWorkerThread>();
		
		int threadsStarted = 0;
		for( RenderServer rs : servers ) {
			for( int i=0; i<rs.getAvailableCapacity(); ++i, ++threadsStarted ) {
				RenderWorkerThread wt = new RenderWorkerThread( rs.start(t), resultQueue );
				wt.start();
				workerThreads.add(wt);
			}
		}
		
		if( threadsStarted == 0 && servers.size() > 0 ) {
			// Then just start one and return it
			return servers.get(new Random().nextInt(servers.size())).start(t);
		}
		
		// It would be neat to!
		// occasionally start new workers using the most updated server list
		// this os actually important for recovering from dead workers
		// (or the worker thread count might just reach 0 and we get stuck)
		
		return new RenderWorker() {
			boolean closed = false;
			@Override public void close() throws IOException {
				closed = false;
				for( RenderWorkerThread wrxrt : workerThreads ) wrxrt.close();  
			}
			@Override public RenderResult nextResult() {
				if( closed ) return null;
				try {
					return resultQueue.take();
				} catch( InterruptedException e ) {
					Thread.currentThread().interrupt();
					return null;
				}
			}
		};
	}
}
