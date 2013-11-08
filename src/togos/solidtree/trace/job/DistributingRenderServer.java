package togos.solidtree.trace.job;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.SynchronousQueue;

/**
 * Distributes tasks to clients that ask for them.
 */
public class DistributingRenderServer
	implements RenderServer, TaskServer
{
	final HashMap<RenderTask,Set<AsyncRenderIterator>> taskResultListeners = new HashMap<RenderTask,Set<AsyncRenderIterator>>();
	
	protected void addListener( RenderTask task, AsyncRenderIterator worker ) {
		synchronized( taskResultListeners ) {
			Set<AsyncRenderIterator> listenerSet = taskResultListeners.get(task);
			if( listenerSet == null ) {
				listenerSet = new HashSet<AsyncRenderIterator>(1);
				taskResultListeners.put(task, listenerSet);
			}
			listenerSet.add(worker);
			taskResultListeners.notifyAll();
		}
	}
	
	protected void removeListener( RenderTask task, AsyncRenderIterator worker ) {
		synchronized( taskResultListeners ) {
			Set<AsyncRenderIterator> listenerSet = taskResultListeners.get(task);
			if( listenerSet == null ) return; // Nothing to do!
			listenerSet.remove(worker);
			if( listenerSet.size() == 0 ) taskResultListeners.remove(task);
		}		
	}
	
	class AsyncRenderIterator implements RenderResultIterator {
		final RenderTask task;
		final SynchronousQueue<RenderResult> resultQueue = new SynchronousQueue<RenderResult>();
		private boolean closed = false; 
		
		public AsyncRenderIterator( RenderTask task ) {
			this.task = task;
		}
		
		@Override public void close() {
			if( closed ) return;
			removeListener(task, this);
		}
		
		public void putResult(RenderResult rr) throws InterruptedException {
			resultQueue.put(rr);
		}
		
		@Override public RenderResult nextResult() {
			try {
				return resultQueue.take();
			} catch( InterruptedException e ) {
				close();
				Thread.currentThread().interrupt();
				return null;
			}
		}
	}
		
	@Override public RenderResultIterator start(RenderTask task) {
		AsyncRenderIterator worker = new AsyncRenderIterator(task);
		addListener( task, worker );
		return worker;
	}
	
	protected static <T> T getRandom( Set<T> s ) {
		Random r = new Random();
		Iterator<T> iter = s.iterator();
		for( int i=r.nextInt(s.size()); i>0; --i, iter.next() );
		return iter.next();
	}
	
	@Override public RenderTask takeTask() {
		try {
			synchronized( taskResultListeners ) {
				while( taskResultListeners.size() == 0 ) taskResultListeners.wait();
				return getRandom(taskResultListeners.keySet());
			}
		} catch( InterruptedException e ) {
			Thread.currentThread().interrupt();
			return null;
		}
	}
	
	@Override public void putTaskResult(RenderTask task, RenderResult result) {
		Set<AsyncRenderIterator> workers;
		synchronized( taskResultListeners ) {
			workers = taskResultListeners.get(task);
			if( workers == null ) return;
			workers = new HashSet<AsyncRenderIterator>( workers );
		}
		try {
			for( AsyncRenderIterator worker : workers ) worker.putResult(result);
		} catch( InterruptedException e ) {
			System.err.println(Thread.currentThread().getName()+" interrupted");
			Thread.currentThread().interrupt();
		}
	}
}
