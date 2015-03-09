package togos.solidtree;

public interface DensityFunction
{
	public double getMaxGradient();
	public double apply(double x, double y, double z);
}
