package togos.solidtree.trace;

public interface Projection
{
	public void project(
		int vectorSize,
		double[] screenX, double[] screenY,
		double[] posX, double[] posY, double[] posZ,
		double[] dirX, double[] dirY, double[] dirZ
	);
}
