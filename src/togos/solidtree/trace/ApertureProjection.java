package togos.solidtree.trace;

import java.util.Random;

public class ApertureProjection implements Projection {
	final Projection wrapped;
	final double aperture;
	final double subjectDistance;
	
	public ApertureProjection( Projection wrapped, double apertureSize, double subjectDistance ) {
		this.wrapped = wrapped;
		this.aperture = apertureSize;
		this.subjectDistance = subjectDistance;
	}
	
	Random rand = new Random();
	
	@Override
	public void project(
		int vectorSize,
		double[] screenX, double[] screenY,
		double[] posX, double[] posY, double[] posZ,
		double[] dirX, double[] dirY, double[] dirZ
	) {
		wrapped.project( vectorSize, screenX, screenY, posX, posY, posZ, dirX, dirY, dirZ );
		for( int i=vectorSize-1; i>=0; --i ) {
			dirX[i] *= subjectDistance / dirZ[i];
			dirY[i] *= subjectDistance / dirZ[i];
			
			// find random point in aperture
			double rx, ry;
			while (true) {
				rx = 2 * rand.nextDouble() - 1;
				ry = 2 * rand.nextDouble() - 1;
				double s = rx * rx + ry * ry;
				if( s > 0.0001 && s <= 1 ) {
					rx *= aperture;
					ry *= aperture;
					break;
				}
			}
			
			dirX[i] -= rx;
			dirY[i] -= ry;
			
			posX[i] += rx;
			posY[i] += ry;
		}
	}
}
