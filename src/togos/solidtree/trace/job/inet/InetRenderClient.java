package togos.solidtree.trace.job.inet;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import togos.solidtree.trace.job.LocalRenderServer;
import togos.solidtree.trace.job.RenderResult;
import togos.solidtree.trace.job.RenderResultIterator;
import togos.solidtree.trace.job.RenderServer;
import togos.solidtree.trace.job.RenderTask;
import togos.solidtree.trace.job.TaskServer;

public class InetRenderClient extends Thread implements RenderServer, TaskServer
{
	final ObjectOutputStream oos;
	final ObjectInputStream ois;
	
	public InetRenderClient( Socket sock )  throws IOException {
		this.oos = new ObjectOutputStream(sock.getOutputStream());
		this.ois = new ObjectInputStream(sock.getInputStream());
	}
	
	private final Map<String,BlockingQueue<TaskResult>> taskResultQueues = new HashMap<String,BlockingQueue<TaskResult>>();
	final BlockingQueue<RenderTask> taskQueue = new ArrayBlockingQueue<RenderTask>(4);
	
	protected void debug( String text ) {
		// no-op
	}
	
	protected synchronized BlockingQueue<TaskResult> getQueue(String taskId) {
		BlockingQueue<TaskResult> q = taskResultQueues.get(taskId);
		if( q == null ) {
			taskResultQueues.put(taskId, q = new ArrayBlockingQueue<TaskResult>(2));
		}
		return q;
	}
	
	protected void send( Object msg ) throws IOException {
		synchronized( oos ) {
			debug("Sending "+msg.getClass()+" to server");
			oos.writeObject( msg );
			oos.reset();
			oos.flush();
		}
	}
	
	public void run() {
		try {
			Object msg;
			while( (msg = ois.readObject()) != null ) {
				debug("Received "+msg.getClass()+" from server");
				
				if( msg instanceof RenderTask ) {
					taskQueue.put( (RenderTask)msg );
				} else if( msg instanceof TaskResult ) {
					TaskResult tr = (TaskResult)msg;
					getQueue( tr.taskId ).put( tr );
				}
			}
		} catch( Exception e ) {
			debug("Oh noes");
			e.printStackTrace();
		}
	}
	
	@Override public RenderTask takeTask() throws IOException, InterruptedException {
		send( new TaskRequest() );
		return taskQueue.take();
	}
	
	@Override public void putTaskResult(String taskId, RenderResult result) throws IOException {
		send( new TaskResult(taskId, result) );
	}
	
	@Override public RenderResultIterator start( final RenderTask task ) throws IOException {
		send( new StartTask(task) );
		final BlockingQueue<TaskResult> resQ = getQueue( task.taskId );
		return new RenderResultIterator() {
			@Override public void close() throws IOException {
				send( new StopTask(task) );
			}
			
			@Override public RenderResult nextResult() throws InterruptedException {
				return resQ.take().result;
			}
		};
	}
	
	
	static Pattern IP6LITERALPATTERN = Pattern.compile("\\[([09a-fA-F:]+)\\]");
	static Pattern HOSTPORTPATTERN = Pattern.compile("(.+)(?::(\\d+))?");
	
	public static void main(String[] args) throws Exception {
		String serverName = null;
		int serverPort = InetRenderServer.DEFAULT_PORT;
		
		for( int i=0; i<args.length; ++i ) {
			String arg = args[i];
			if( arg.startsWith("-") ) {
				System.err.println("Error: Unrecognized argument: "+arg);
				System.exit(1);
			} else {
				Matcher m = HOSTPORTPATTERN.matcher(arg);
				if( !m.matches() ) {
					System.err.println("Error: Unrecognized argument: "+arg);
					System.exit(1);					
				}
				serverName = m.group(1);
				if( m.group(2) != null ) serverPort = Integer.parseInt(m.group(2));
			}
		}
		
		if( serverName == null ) {
			System.err.println("Error: No server address given");
			System.exit(1);
		}
		
		Socket s = new Socket(serverName, serverPort);
		final InetRenderClient renderClient = new InetRenderClient(s);
		renderClient.start();
		
		int threadCount = Runtime.getRuntime().availableProcessors();
		for( int i=threadCount; i>0; --i ) {
			Thread t = new Thread("Render worker "+i) {
				@Override public void run() {
					int maxTaskRepititions = 1;
					RenderTask task;
					LocalRenderServer lrs = new LocalRenderServer();
					try {
						while( (task = renderClient.takeTask()) != null ) {
							RenderResultIterator rri = lrs.start(task);
							RenderResult res;
							for( int r=0; r<maxTaskRepititions && (res = rri.nextResult()) != null; ++r ) {
								renderClient.putTaskResult(task.taskId, res);
							}
						}
					} catch( InterruptedException e ) {
						Thread.currentThread().interrupt();
					} catch( IOException e ) {
						System.err.println("Exiting task runner due to exception:");
						e.printStackTrace();
					}
				}
			};
			t.start();
		}
	}
}
