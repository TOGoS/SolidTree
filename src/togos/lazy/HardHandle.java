package togos.lazy;

public class HardHandle<T> implements Ref<T>
{
	protected final T v;
	public HardHandle(T v) { this.v = v; }
	public T get() { return v; }
}
