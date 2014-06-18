package togos.komput;

public interface Komputr<I, O>
{
	public O apply( I input, int iteration );
}
