package togos.solidtree.trace;

import java.io.Serializable;
import java.util.Random;

import togos.solidtree.matrix.VectorMath;

public class ApertureProjection implements Projection, Serializable
{
	private static final long serialVersionUID = 1L;
	
	final Projection wrapped;
	final double apertureSize;
	final double focalDistance;
	
	public ApertureProjection( Projection wrapped, double apertureSize, double subjectDistance ) {
		this.wrapped = wrapped;
		this.apertureSize = apertureSize;
		this.focalDistance = subjectDistance;
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
			assert VectorMath.isNormalized(dirX[i], dirY[i], dirZ[i]);
			// Scale so that the tips of the direction vectors are at the focus
			dirX[i] *= focalDistance / dirZ[i];
			dirY[i] *= focalDistance / dirZ[i];
			dirZ[i] *= focalDistance;
			
			// find random point in aperture
			double rx, ry;
			while (true) {
				rx = 2 * rand.nextDouble() - 1;
				ry = 2 * rand.nextDouble() - 1;
				double s = rx * rx + ry * ry;
				if( s > 0.0001 && s <= 1 ) {
					rx *= apertureSize;
					ry *= apertureSize;
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
