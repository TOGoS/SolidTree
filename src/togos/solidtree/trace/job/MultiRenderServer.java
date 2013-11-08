package togos.solidtree.trace.job;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
	
	class RenderPusherThread extends Thread {
		protected final RenderResultIterator wrxr;
		protected final BlockingQueue<RenderResult> resultQueue;
		
		public RenderPusherThread( RenderResultIterator wrxr, BlockingQueue<RenderResult> resultQueue ) {
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
	
	@Override public RenderResultIterator start(RenderTask t) {
		final SynchronousQueue<RenderResult> resultQueue = new SynchronousQueue<RenderResult>(true);
		final List<RenderServer> servers = getServerListSnapshot();
		final Set<RenderPusherThread> workerThreads = new HashSet<RenderPusherThread>();
		
		for( RenderServer rs : servers ) {
			RenderPusherThread wt = new RenderPusherThread( rs.start(t), resultQueue );
			wt.start();
			workerThreads.add(wt);
		}
		
		// It would be neat to!
		// occasionally start new workers using the most updated server list
		// this os actually important for recovering from dead workers
		// (or the worker thread count might just reach 0 and we get stuck)
		
		return new RenderResultIterator() {
			boolean closed = false;
			@Override public void close() throws IOException {
				closed = false;
				for( RenderPusherThread wrxrt : workerThreads ) wrxrt.close();
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
