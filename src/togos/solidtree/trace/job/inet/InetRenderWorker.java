package togos.solidtree.trace.job.inet;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import togos.solidtree.trace.job.LocalRenderServer;
import togos.solidtree.trace.job.LocalRenderWorker;
import togos.solidtree.trace.job.PerformanceCounter;
import togos.solidtree.trace.job.RenderResult;
import togos.solidtree.trace.job.RenderTask;

public class InetRenderWorker
{
	static final int VERBOSITY_NONE = 0;
	static final int VERBOSITY_WARNINGS = 1;
	static final int VERBOSITY_NORMAL = 2;
	static final int VERBOSITY_MUCH = 3;
	
	String serverHost;
	int serverPort;
	
	Socket sock;
	ObjectOutputStream oos;
	ObjectInputStream ois;
	public int verbosity = VERBOSITY_WARNINGS;
	
	public InetRenderWorker( String serverHost, int serverPort )  throws IOException {
		this.serverHost = serverHost;
		this.serverPort = serverPort;
	}
	
	ArrayBlockingQueue<RenderTask> taskQueue = new ArrayBlockingQueue<RenderTask>(1);
	ArrayBlockingQueue<TaskResult> resultQueue = new ArrayBlockingQueue<TaskResult>(3);
	
	protected void forceClose( Closeable c ) {
		if( c == null ) return;
		try {
			c.close();
		} catch( IOException e ) {
			// Don't care!
		}
	}
	
	protected void closeConnection() {
		forceClose(ois);
		forceClose(oos);
		forceClose(sock);
		synchronized(this) {
			ois = null;
			oos = null;
			sock = null;
			notifyAll();
		}
	}
	
	public void runReader() throws InterruptedException {
		int reconnectAfter = 5;
		while( !Thread.interrupted() ) {
			try {
				if( verbosity >= VERBOSITY_NORMAL ) System.err.println("Connecting to "+serverHost+" port "+serverPort+"...");
				sock = new Socket(serverHost, serverPort);
				ObjectOutputStream _oos = new ObjectOutputStream(sock.getOutputStream());
				_oos.flush();
				ObjectInputStream _ois = new ObjectInputStream(sock.getInputStream());
				reconnectAfter = 5;
				synchronized(this) {
					this.oos = _oos;
					this.ois = _ois;
					notifyAll(); // There's an outputstream available!
				}
				if( verbosity >= VERBOSITY_NORMAL ) System.err.println("Connected!");
				Object msg;
				while( true ) {
					try {
						msg = _ois.readObject();
						if( msg instanceof RenderTask ) {
							taskIncoming( (RenderTask)msg );
						} else {
							if( verbosity >= VERBOSITY_WARNINGS ) {
								System.err.println("Received a "+msg.getClass()+", which I don't know what to do with.  Ignoring it.");
							}
						}
					} catch( ClassNotFoundException e ) {
						if( verbosity >= VERBOSITY_WARNINGS ) {
							e.printStackTrace();
							System.err.println("Ignoring that ClassNotFoundException");
						}
					}
				}
			} catch( IOException e ) {
				System.err.println();
				e.printStackTrace();
			} finally {
				closeConnection();
			}
			if( verbosity >= VERBOSITY_NORMAL ) System.err.println("Will attempt reconnect in "+reconnectAfter+" seconds.");
			Thread.sleep(reconnectAfter * 1000);
			if( reconnectAfter < 60 ) reconnectAfter += 5;
		}
	}
	
	protected synchronized ObjectOutputStream getWriter() throws InterruptedException {
		while( oos == null ) wait();
		return oos;
	}
	
	public void runWriter() throws InterruptedException {
		while( true ) {
			try {
				ObjectOutputStream oos = getWriter();
				oos.writeObject(new TaskRequest());
				oos.writeObject(resultQueue.take());
			} catch( IOException e ) {
				if( verbosity >= VERBOSITY_NORMAL ) System.err.println("Error writing; forcing close");
				closeConnection();
			}
		}
	}
	
	final PerformanceCounter perfCount = new PerformanceCounter();
	protected final LocalRenderServer lrs = new LocalRenderServer();
	int taskRepititions = 1;
	
	RenderTask lastTaskGotten = null;
	protected void taskIncoming( RenderTask task ) throws InterruptedException {
		taskQueue.offer(task);
		synchronized( this ) {
			if( lastTaskGotten == null ) {
				lastTaskGotten = task;
				notifyAll();
			}
		}
	}
	
	protected RenderTask getSomeTask() throws InterruptedException {
		RenderTask t = taskQueue.poll();
		synchronized( this ) {
			if( t == null ) {
				while( lastTaskGotten == null ) {
					wait();
				}
			} else {
				lastTaskGotten = t;
			}
			return lastTaskGotten;
		}
	}
	
	public void runWorker() throws InterruptedException {
		while( true ) {
			RenderTask task = getSomeTask();
			
			LocalRenderWorker rri = lrs.start(task);
			RenderResult res;
			for( int r=0; r<taskRepititions && (res = rri.nextResult()) != null; ++r ) {
				perfCount.samplesCompleted( res.sampleCount );
				if( verbosity >= VERBOSITY_NORMAL ) System.err.print(perfCount.toString()+"    \r");
				resultQueue.put(new TaskResult(task.taskId, res));
			}
			rri.close();
		}
	}
	
	abstract class AutoRestartingThread extends Thread {
		public AutoRestartingThread(String name) {
			super(name);
		}
		
		protected abstract void _run() throws InterruptedException;
		
		public void run() {
			while(true) {
				try {
					_run();
					System.err.println(getName()+" unexpectedly returned.");
				} catch( InterruptedException e ) {
					if( verbosity >= VERBOSITY_WARNINGS ) {
						System.err.println(getName()+" interrupted!  Quitting!");
					}
					Thread.currentThread().interrupt();
				} catch( Exception e ) {
					System.err.println("Caught unexpected exception in "+getName());
					e.printStackTrace();
				}
				System.err.println("Restarting "+getName()+" in 15 seconds");
				try {
					Thread.sleep(15*1000);
				} catch( InterruptedException e ) {
					if( verbosity >= VERBOSITY_WARNINGS ) {
						System.err.println(getName()+" interrupted while waiting to restart!  Quitting!");
					}
					Thread.currentThread().interrupt();
				}
			}
		}
	}
	
	public void run( int workerThreadCount ) {
		if( verbosity >= VERBOSITY_NORMAL ) {
			final DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
			System.err.println("Starting "+workerThreadCount+" threads at "+df.format(new Date()));
			System.err.println(PerformanceCounter.HEADER_STRING);
			System.err.print(perfCount.toString()+"    \r");
		}
		perfCount.samplesCompleted(0);
		new AutoRestartingThread("Writer") { public void _run() throws InterruptedException { runWriter(); } }.start();
		for( int i=0; i<workerThreadCount; ++i ) {
			new AutoRestartingThread("Worker "+i) { public void _run() throws InterruptedException { runWorker(); } }.start();
		}
		new AutoRestartingThread("Reader") { public void _run() throws InterruptedException { runReader(); } }.run();
	}
	
	static Pattern IP6LITERALPATTERN = Pattern.compile("\\[([09a-fA-F:]+)\\]");
	static Pattern HOSTPORTPATTERN = Pattern.compile("(.+)(?::(\\d+))?");
	
	public static void main(String[] args) throws Exception {
		String serverName = null;
		int serverPort = InetRenderServer.DEFAULT_PORT;
		int verbosity = VERBOSITY_WARNINGS;
		int threadCount = -1;
		
		for( int i=0; i<args.length; ++i ) {
			String arg = args[i];
			if( "-threads".equals(arg) ) {
				threadCount = Integer.parseInt(args[++i]);
			} else if( "-q".equals(arg) ) {
				verbosity = VERBOSITY_NONE;
			} else if( "-v".equals(arg) ) {
				verbosity = VERBOSITY_NORMAL;
			} else if( "-debug".equals(arg) ) {
				verbosity = VERBOSITY_MUCH;
			} else if( arg.startsWith("-") ) {
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
		
		if( threadCount == -1 ) {
			threadCount = Runtime.getRuntime().availableProcessors();
		}
		InetRenderWorker worker = new InetRenderWorker(serverName, serverPort);
		worker.verbosity = verbosity;
		worker.run(threadCount);
	}
}
