package togos.solidtree.func;

import java.util.Random;

import togos.solidtree.DensityFunction;

public abstract class BaseDensityFunction implements DensityFunction
{
	public static double estimateMaxGradient(DensityFunction df) {
		double maxGrad = 0;
		Random r = new Random(133780085);
		for( int i=0; i<1000; ++i ) {
			double x = r.nextGaussian()*1024;
			double y = r.nextGaussian()*1024;
			double z = r.nextGaussian()*1024;
			
			double va = df.apply(x, y, z);
			
			for( int j=0; j<10; ++j ) {
				double f = Math.pow(2, 16-r.nextInt(32));
				double dx = f*r.nextGaussian();
				double dy = f*r.nextGaussian();
				double dz = f*r.nextGaussian();
				
				double vb = df.apply(x+dx, y+dy, z+dz);
				double dist = Math.sqrt(dx*dx+dy*dy+dz*dz);
				if( dist == 0 ) continue;
				
				maxGrad = Math.max(maxGrad, Math.abs(va-vb)/dist);
			}
		}
		return maxGrad;
	}
	
	protected final double maxGradient;
	
	public BaseDensityFunction() {
		this.maxGradient = estimateMaxGradient(this);
	}
	
	@Override public double getMaxGradient() { return maxGradient; }
	
	public abstract double apply(double x, double y, double z);
}
