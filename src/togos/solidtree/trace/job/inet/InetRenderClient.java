package togos.solidtree.trace.job.inet;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import togos.solidtree.trace.job.RenderResult;
import togos.solidtree.trace.job.RenderResultIterator;
import togos.solidtree.trace.job.RenderServer;
import togos.solidtree.trace.job.RenderTask;
import togos.solidtree.trace.job.TaskServer;

public class InetRenderClient extends Thread implements RenderServer, TaskServer
{
	final ObjectInputStream ois;
	final ObjectOutputStream oos;
	
	public InetRenderClient( Socket sock )  throws IOException {
		this.ois = new ObjectInputStream(sock.getInputStream());
		this.oos = new ObjectOutputStream(sock.getOutputStream());
	}
	
	private final Map<RenderTask,BlockingQueue<RenderResult>> taskResultQueues = new HashMap<RenderTask,BlockingQueue<RenderResult>>();
	final BlockingQueue<RenderTask> taskQueue = new ArrayBlockingQueue<RenderTask>(4);
	
	protected synchronized BlockingQueue<RenderResult> getQueue(RenderTask task) {
		BlockingQueue<RenderResult> q = taskResultQueues.get(task);
		if( q == null ) {
			taskResultQueues.put(task, q = new ArrayBlockingQueue<RenderResult>(2));
		}
		return q;
	}

	public void run() {
		try {
			Object obj;
			while( (obj = ois.readObject()) != null ) {
				if( obj instanceof RenderTask ) {
					taskQueue.put( (RenderTask)obj );
				} else if( obj instanceof TaskResult ) {
					TaskResult tr = (TaskResult)obj;
					getQueue( tr.task ).put( tr.res );
				}
			}
		} catch( Exception e ) {
			System.err.println("Oh noes");
			e.printStackTrace();
		}
	}
	
	@Override public RenderTask takeTask() {
		try {
			synchronized( oos ) {
				oos.writeObject( new TaskRequest() );
			}
			return taskQueue.take();
		} catch( InterruptedException e ) {
			Thread.currentThread().interrupt();
			e.printStackTrace();
			return null;
		} catch( IOException e ) {
			e.printStackTrace();
			return null;
		}
	}

	@Override public void putTaskResult(RenderTask task, RenderResult result) {
		try {
			synchronized( oos ) {
				// TODO: probably want separate 'start task' / 'end task' messages
				oos.writeObject( task );
			}
		} catch( IOException e ) {
			e.printStackTrace();
		}
	}

	@Override public RenderResultIterator start(RenderTask t) {
		
		// TODO Auto-generated method stub
		return null;
	}
	
}
