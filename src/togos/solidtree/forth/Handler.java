package togos.solidtree.forth;

public interface Handler<T,E extends Throwable>
{
	public void handle(T v) throws E;
}
