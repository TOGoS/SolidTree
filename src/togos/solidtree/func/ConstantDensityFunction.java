package togos.solidtree.func;

import togos.solidtree.DensityFunction;

public class ConstantDensityFunction implements DensityFunction
{
	public final double value;
	
	public ConstantDensityFunction( double value ) {
		this.value = value;
	}
	
	@Override public double getMaxGradient() { return 0; }
	@Override public double apply(double x, double y, double z) { return value; }
}
