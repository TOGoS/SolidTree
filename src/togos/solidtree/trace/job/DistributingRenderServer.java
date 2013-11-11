package togos.solidtree.trace.job;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;

/**
 * Distributes tasks to clients that ask for them.
 */
public class DistributingRenderServer
	implements RenderServer, TaskServer
{
	public interface RenderResultListener {
		public void result( RenderResult res ) throws IOException, InterruptedException;
		public void end();
	}
	
	static class TaskInfo {
		public final RenderTask task;
		final HashSet<RenderResultListener> listeners = new HashSet<RenderResultListener>();
		
		public TaskInfo( RenderTask task ) {
			this.task = task;
		}
	}
	
	final HashMap<String,TaskInfo> taskResultListeners = new HashMap<String,TaskInfo>();
	
	protected void addListener( RenderTask task, RenderResultListener listener ) {
		synchronized( taskResultListeners ) {
			TaskInfo taskInfo = taskResultListeners.get(task.taskId);
			if( taskInfo == null ) {
				taskInfo = new TaskInfo( task );
				taskResultListeners.put(task.taskId, taskInfo);
			}
			taskInfo.listeners.add(listener);
			taskResultListeners.notifyAll();
		}
	}
	
	protected void removeListener( String taskId, RenderResultListener listener ) {
		synchronized( taskResultListeners ) {
			TaskInfo taskInfo = taskResultListeners.get(taskId);
			if( taskInfo == null ) return; // Nothing to do!
			taskInfo.listeners.remove(listener);
			if( taskInfo.listeners.size() == 0 ) taskResultListeners.remove(taskId);
		}		
	}
	
	class AsyncRenderIterator implements RenderResultIterator, RenderResultListener {
		final RenderTask task;
		private RenderResult result = null;
		private boolean closed = false; 
		
		public AsyncRenderIterator( RenderTask task ) {
			this.task = task;
		}
		
		@Override public void close() {
			if( closed ) return;
			synchronized( this ) {
				closed = true;
				notifyAll();
			}
			removeListener(task.taskId, this);
		}
		
		@Override public void result(RenderResult rr) {
			try {
				synchronized( this ) {
					while( !closed && result != null ) wait();
					if( !closed ) {
						result = rr;
						notifyAll();
					}
				}
			} catch( InterruptedException e ) {
				Thread.currentThread().interrupt();
			}
		}
		
		@Override public void end() {
			close();
		}
		
		@Override public RenderResult nextResult() {
			try {
				synchronized( this ) {
					while( !closed && result == null ) wait();
					RenderResult res = result;
					result = null;
					notifyAll();
					return res;
				}
			} catch( InterruptedException e ) {
				Thread.currentThread().interrupt();
				return null;
			}
		}
	}
	
	public void start( RenderTask task, RenderResultListener listener ) {
		addListener( task, listener );
	}
	
	@Override public RenderResultIterator start(RenderTask task) {
		AsyncRenderIterator worker = new AsyncRenderIterator(task);
		addListener( task, worker );
		return worker;
	}
	
	protected static <T> T getRandom( Collection<T> s ) {
		Random r = new Random();
		Iterator<T> iter = s.iterator();
		for( int i=r.nextInt(s.size()); i>0; --i, iter.next() );
		return iter.next();
	}
	
	@Override public RenderTask takeTask() {
		try {
			synchronized( taskResultListeners ) {
				while( taskResultListeners.size() == 0 ) taskResultListeners.wait();
				return getRandom(taskResultListeners.values()).task;
			}
		} catch( InterruptedException e ) {
			Thread.currentThread().interrupt();
			return null;
		}
	}
	
	@Override public void putTaskResult(String taskId, RenderResult result)
		throws IOException, InterruptedException
	{
		ArrayList<RenderResultListener> listeners;
		synchronized( taskResultListeners ) {
			TaskInfo taskInfo = taskResultListeners.get(taskId);
			if( taskInfo == null ) return;
			listeners = new ArrayList<RenderResultListener>( taskInfo.listeners );
		}
		for( RenderResultListener listener : listeners ) listener.result(result);
	}
}
