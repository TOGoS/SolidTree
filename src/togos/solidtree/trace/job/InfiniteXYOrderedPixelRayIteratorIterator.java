package togos.solidtree.trace.job;

import java.io.Serializable;

import togos.solidtree.matrix.Matrix;
import togos.solidtree.trace.Projection;

public class InfiniteXYOrderedPixelRayIteratorIterator extends InfiniteIterator<XYOrderedPixelRayIterator>
	implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	protected final int imageWidth;
	protected final int imageHeight;
	protected final Projection projection;
	protected final Matrix transform;
	protected final int innerIterations;
	
	transient int currentInnerIterations;
	
	public InfiniteXYOrderedPixelRayIteratorIterator(int imageWidth, int imageHeight, Projection projection, Matrix transform, int innerIterations) {
		this.imageWidth = imageWidth;
		this.imageHeight = imageHeight;
		this.projection = projection;
		this.transform = transform;
		this.innerIterations = innerIterations;
	}
	
	@Override public XYOrderedPixelRayIterator next() {
		if( currentInnerIterations <= 0 ) currentInnerIterations = 1;
		if( currentInnerIterations > innerIterations ) currentInnerIterations = innerIterations;
		
		assert currentInnerIterations > 0;
		
		XYOrderedPixelRayIterator it = new XYOrderedPixelRayIterator(imageWidth, imageHeight, projection, transform, currentInnerIterations);
		
		currentInnerIterations *= 2;
		if( currentInnerIterations > innerIterations ) currentInnerIterations = innerIterations;
		
		return it;
	}
}
