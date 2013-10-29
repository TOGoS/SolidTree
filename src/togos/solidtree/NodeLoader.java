package togos.solidtree;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;

public class NodeLoader
{
	interface LoadContext<T>
	{
		public T get(String name);
	}
	
	static class NullLoadContext<T> implements LoadContext<T>
	{
		@Override public T get(String name) { return null; }
	}
	
	static class HashMapLoadContext<T> implements LoadContext<T>
	{
		protected final LoadContext<T> parent;
		protected final HashMap<String,T> values = new HashMap<String, T>();
		
		public HashMapLoadContext(LoadContext<T> parent) {
			this.parent = parent;
		}
		
		@Override public T get(String name) {
			if( values.containsKey(name) ) {
				return values.get(name);
			}
			T v = parent.get(name);
			values.put(name, v);
			return v;
		}
		
		protected void set(String name, T value) {
			values.put(name, value);
		}
	}
	
	public SolidNode readTextNode(BufferedReader r, LoadContext<SolidNode> ctx) throws IOException {
		String l;
		
		while( (l = r.readLine()) != null ) {
			l = l.trim();
			// TODO: deal with =defs and everything.
		}
		throw new UnsupportedOperationException();
	}
}
