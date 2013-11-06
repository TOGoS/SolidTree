package togos.solidtree.trace.job;

import java.util.Random;

import togos.solidtree.trace.Projection;

public class XYOrderedPixelRayIterator implements PixelRayIterator
{
	final int imageWidth, imageHeight;
	final double viewX0, viewX1, viewY0, viewY1;
	final Random rand = new Random();
	final long targetSamples;
	final Projection projection;
	int x, y;
	long totalSamples;
	
	double[] screenX, screenY;
	
	public XYOrderedPixelRayIterator( int imageWidth, int imageHeight, Projection projection, int targetSamplesPerPixel ) {
		assert imageWidth >= 0;
		assert imageHeight >= 0;
		assert targetSamplesPerPixel >= 0;
		this.imageWidth = imageWidth;
		this.imageHeight = imageHeight;
		this.projection = projection;
		this.targetSamples = imageWidth*imageHeight*targetSamplesPerPixel;
		this.viewX0 = -0.5;
		this.viewX1 = +0.5;
		this.viewY0 = -0.5;
		this.viewY1 = +0.5;
	}
	
	@Override public boolean next(PixelRayBuffer b) {
		if( totalSamples == targetSamples ) return false;
		
		b.vectorSize = (int)Math.min(targetSamples - totalSamples, b.maxVectorSize);
		if( screenX == null || screenX.length < b.vectorSize ) {
			screenX = new double[b.vectorSize];
			screenY = new double[b.vectorSize];
		}
		
		int i;
		for( i=0; i<b.vectorSize; ++i, ++x ) {
			if( x >= imageWidth ) {
				x = 0; ++y;
			}
			if( y >= imageHeight ) {
				y = 0;
			}
			screenX[i] = (x + rand.nextDouble()) * (viewX1 - viewX0) + viewX0;
			screenY[i] = (y + rand.nextDouble()) * (viewY1 - viewY0) + viewY0;
		}
		
		projection.project(b.vectorSize, screenX, screenY, b.ox, b.oy, b.oz, b.dx, b.dy, b.dz);
		
		totalSamples += b.vectorSize; 
		return true;
	}
}
