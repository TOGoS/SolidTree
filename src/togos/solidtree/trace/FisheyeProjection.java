package togos.solidtree.trace;

public class FisheyeProjection implements Projection
{
	/** Horizontal and vertical fields of view, in radians */
	final double fovX, fovY;
	
	public FisheyeProjection( double fovX, double fovY ) {
		this.fovX = fovX;
		this.fovY = fovY;
	}
	
	@Override
	public void project( int vectorSize,
		double[] screenX, double[] screenY,
		double[] posX, double[] posY, double[] posZ,
		double[] dirX, double[] dirY, double[] dirZ
	) {
		for( int i=vectorSize-1; i>=0; --i ) {
			double ay = screenY[i] * fovY;
			double ax = screenX[i] * fovX;
			double avSquared = ay * ay + ax * ax;
			double angleFromCenter = Math.sqrt(avSquared);
			double dz = Math.cos(angleFromCenter);
			double dv = Math.sin(angleFromCenter);
			double dy, dx;
			if (angleFromCenter == 0) {
				dx = dy = 0;
			} else {
				dx = dv * (ax / angleFromCenter);
				dy = dv * (ay / angleFromCenter);
			}
			posX[i] = posY[i] = posZ[i] = 0;
			assert dx != 0 || dy != 0 || dz != 0;
			dirX[i] = dx;
			dirY[i] = dy;
			dirZ[i] = dz;
		}
	}
}
