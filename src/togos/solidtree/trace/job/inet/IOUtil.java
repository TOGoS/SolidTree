package togos.solidtree.trace.job.inet;

import java.io.IOException;
import java.net.Socket;

public final class IOUtil
{
	private IOUtil() { }
	
	/**
	 * For compatibility with Java < 1.7
	 * where Socket doesn't implement Closeable. 
	 */
	public static void forceClose( Socket c ) {
		if( c == null ) return;
		try {
			c.close();
		} catch( IOException e ) {
		}
	}
}
