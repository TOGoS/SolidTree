package togos.solidtree.trace.job;

import java.util.Iterator;

public abstract class InfiniteIterator<T> implements Iterator<T>
{
	@Override public boolean hasNext() { return true; }
	@Override public void remove() { throw new UnsupportedOperationException(); }
}
