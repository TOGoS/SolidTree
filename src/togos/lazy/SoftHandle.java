package togos.lazy;

import java.lang.ref.SoftReference;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SoftHandle<T> implements Ref<T>, Future<T> {
	protected final Callable<T> generator;
	protected SoftReference<T> softReference;
	
	public SoftHandle( Callable<T> generator ) {
		this.generator = generator;
	}
	
	@Override public T get() throws ExecutionException {
		SoftReference<T> sr = softReference;
		T v;
		if( sr != null && (v = sr.get()) != null ) {
			return v;
		}
		// This ain't necessarily an efficient use of threads
		// if multiple threads try to get() at once!
		try {
			v = generator.call();
		} catch( Exception e ) {
			throw new ExecutionException(e);
		}
		softReference = new SoftReference<T>(v);
		return v;
	}
	
	@Override public boolean isDone() {
		SoftReference<T> sr = softReference;
		return sr != null && (sr.get() != null);
	}
	
	@Override public boolean cancel(boolean mayInterruptIfRunning) { return false; }
	@Override public boolean isCancelled() { return false; }
	@Override public T get(long timeout, TimeUnit unit)
		throws InterruptedException,
		ExecutionException, TimeoutException
	{
		SoftReference<T> sr = softReference;
		T v;
		if( sr != null && (v = sr.get()) != null ) return v;
		throw new TimeoutException("get w/ timeout not yet implemented");
	}
}
