package togos.solidtree.trace.job;

import java.util.Random;

import togos.solidtree.matrix.Matrix;
import togos.solidtree.matrix.MatrixMath;
import togos.solidtree.matrix.Vector3D;
import togos.solidtree.trace.Projection;

public class XYOrderedPixelRayIterator implements PixelRayIterator
{
	// Offset/direction from projection, before transforming
	final Vector3D pixelOffset = new Vector3D();
	final Vector3D pixelDirection = new Vector3D();
	// Offset/direction after transforming
	final Vector3D rayOffset = new Vector3D();
	final Vector3D rayDirection = new Vector3D();
	
	final int imageWidth, imageHeight;
	final double viewX0, viewX1, viewY0, viewY1;
	final Random rand = new Random();
	final long targetSamples;
	final Matrix cameraTransform;
	final Projection projection;
	
	int x, y;
	long totalSamples;
	
	double[] screenX, screenY;
	
	public XYOrderedPixelRayIterator( int imageWidth, int imageHeight, Projection projection, Matrix cameraTransform, int targetSamplesPerPixel ) {
		assert imageWidth >= 0;
		assert imageHeight >= 0;
		assert targetSamplesPerPixel >= 0;
		this.imageWidth = imageWidth;
		this.imageHeight = imageHeight;
		this.cameraTransform = cameraTransform;
		this.projection = projection;
		this.targetSamples = (long)imageWidth*imageHeight*targetSamplesPerPixel;
		this.viewX0 = -0.5;
		this.viewX1 = +0.5;
		this.viewY0 = +0.5;
		this.viewY1 = -0.5;
	}
	
	@Override public int getImageDataSize() {
		return imageWidth*imageHeight;
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
			
			b.index[i] = x + y*imageWidth;
			screenX[i] = (x + rand.nextDouble()) * (viewX1 - viewX0) / imageWidth  + viewX0;
			screenY[i] = (y + rand.nextDouble()) * (viewY1 - viewY0) / imageHeight + viewY0;
		}
		
		projection.project(b.vectorSize, screenX, screenY, b.ox, b.oy, b.oz, b.dx, b.dy, b.dz);
		
		for( i=0; i<b.vectorSize; ++i ) {
			// Apply transformation!!
			   pixelOffset.set(b.ox[i], b.oy[i], b.oz[i]);
			pixelDirection.set(b.dx[i], b.dy[i], b.dz[i]);
			MatrixMath.multiply( cameraTransform, pixelOffset, rayOffset );
			MatrixMath.multiplyRotationOnly( cameraTransform, pixelDirection, rayDirection );
			b.ox[i] =    rayOffset.x; b.oy[i] =    rayOffset.y; b.oz[i] =    rayOffset.z;
			b.dx[i] = rayDirection.x; b.dy[i] = rayDirection.y; b.dz[i] = rayDirection.z;
		}
		
		totalSamples += b.vectorSize; 
		return true;
	}
}
