package togos.solidtree.trace.job.inet;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;

import togos.solidtree.trace.job.DistributingRenderServer;
import togos.solidtree.trace.job.RenderResult;

public class InetRenderServer extends Thread
{
	public static final int DEFAULT_PORT = 31148;
	
	public final DistributingRenderServer wrappedServer;
	public final int port;
	private ServerSocket ss;
	
	public InetRenderServer( DistributingRenderServer wrappedServer, int port ) {
		this.wrappedServer = wrappedServer;
		this.port = port;
	}
	
	protected void debug( String text ) {
		// no-op
	}
	
	class ConnectionHandler extends Thread {
		private final Socket sock;
		private final ObjectOutputStream oos;
		private final ObjectInputStream ois;
		private boolean closed = false;
		
		public ConnectionHandler( Socket sock ) throws IOException {
			this.sock = sock;
			this.oos = new ObjectOutputStream(sock.getOutputStream());
			this.ois = new ObjectInputStream(sock.getInputStream());
		}
		
		protected synchronized void send( Object msg ) {
			if( closed ) return;
			try {
				debug("Sending "+msg.getClass()+" to client");
				oos.writeObject(msg);
				oos.flush();
			} catch( IOException e ) {
				closed = true;
			}
		}
		
		@Override public void run() {
			Object msg;
			try {
				while( !closed && (msg = ois.readObject()) != null ) {
					debug("Received "+msg.getClass()+" from client");
					
					if( msg instanceof StartTask ) {
						final StartTask startTask = (StartTask)msg;
						wrappedServer.start(startTask.task, new DistributingRenderServer.RenderResultListener() {
							@Override public void result(RenderResult res) throws IOException, InterruptedException {
								oos.writeObject(new TaskResult(startTask.task.taskId, res));
							}
							
							@Override public void end() {
								try {
									oos.writeObject(new TaskResult(startTask.task.taskId, null));
								} catch( IOException e ) {
									debug("Couldn't write task result stream end to socket");
								}
							}
						});
					} else if( msg instanceof StopTask ) {
						// TODO
						throw new IOException("Stop task not implemented!");
					} else if( msg instanceof TaskRequest ) {
						send( wrappedServer.takeTask() );
					} else if( msg instanceof TaskResult ) {
						TaskResult taskResult = (TaskResult)msg;
						wrappedServer.putTaskResult(taskResult.taskId, taskResult.result);
					} else {
						debug("Don't understand message "+msg.getClass());
					}
				}
			} catch( InterruptedException e ) {
				Thread.currentThread().interrupt();
			} catch( IOException e ) {
				closed = true;
				e.printStackTrace();
			} catch( ClassNotFoundException e ) {
				closed = true;
				e.printStackTrace();
			} finally {
				removeConnectionHandler(this);
			}
		}
		
		public void close() throws IOException {
			closed = true;
			sock.close();
			this.interrupt();
		}
	}
	
	private HashSet<ConnectionHandler> connectionHandlers = new HashSet<ConnectionHandler>();
	
	protected void addConnectionHandler( ConnectionHandler h ) {
		synchronized(connectionHandlers) { connectionHandlers.add(h); }
	}
	protected void removeConnectionHandler( ConnectionHandler h ) {
		synchronized(connectionHandlers) { connectionHandlers.add(h); }
	}
	
	@Override public void run() {
		Socket sock;
		try {
			ss = new ServerSocket(port);
			debug("Render server listening on port "+port);
			
			while( (sock = ss.accept()) != null ) {
				debug("Got connection from "+sock.getRemoteSocketAddress());
				try {
					ConnectionHandler ch = new ConnectionHandler(sock);
					addConnectionHandler(ch);
					ch.start();
				} catch (IOException e) {
					debug("Error creating connection handler");
					e.printStackTrace();
				}
			}
		} catch (IOException e) {
			debug("Error listening");
		}
	}
	
	public void close() throws IOException {
		ss.close();
		synchronized( connectionHandlers ) {
			for( ConnectionHandler ch : connectionHandlers ) {
				try {
					ch.close();
				} catch( IOException e ) {
					debug("Error while closing connection handler during server shutdown");
					e.printStackTrace();
				}
			}
		}
	}
}
