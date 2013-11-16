package togos.solidtree.trace.job;

public interface PixelRayIterator
{
	public int getImageDataSize();
	public boolean next( PixelRayBuffer b );
}
