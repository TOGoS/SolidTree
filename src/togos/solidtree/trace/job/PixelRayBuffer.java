package togos.solidtree.trace.job;

public class PixelRayBuffer
{
	public final int maxVectorSize;
	public int vectorSize;
	public final int[] index;
	public final double[] ox, oy, oz;
	public final double[] dx, dy, dz;
	public final double[] time;
	
	public PixelRayBuffer( final int size ) {
		this.maxVectorSize = size;
		index = new int[size];
		ox = new double[size]; oy = new double[size]; oz = new double[size];
		dx = new double[size]; dy = new double[size]; dz = new double[size];
		time = new double[size];
	}
}
