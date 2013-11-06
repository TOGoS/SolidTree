package togos.hdrutil;

public class HDRExposure
{
	public final int width, height;
	/** Sum of red, green, and blue over entire exposure for each pixel */ 
	public final float[] r, g, b;
	/** Virtual exposure time for each pixel */
	public final float[] e;
	
	public HDRExposure( int width, int height ) {
		assert Util.dimensionsSane(width, height);
		
		this.e = new float[width*height];
		this.r = new float[width*height];
		this.g = new float[width*height];
		this.b = new float[width*height];
		this.width = width;
		this.height = height;
	}
	
	public int getWidth() { return width; }
	public int getHeight() { return height; }
	
	public boolean isCompatible( HDRExposure other ) {
		return other.width == this.width && other.height == this.height;
	}
	
	public void add( HDRExposure other ) throws IncompatibleImageException {
		if( !isCompatible(other) ) throw new IncompatibleImageException("Cannot add(); images are incompatible sizes");
		
		BatchMath.add(e, other.e, e);
		BatchMath.add(r, other.r, r);
		BatchMath.add(g, other.g, g);
		BatchMath.add(b, other.b, b);
	}
	
	public HDRImage getImage() {
		HDRImage img = new HDRImage( width, height );
		img.load(this);
		return img;
	}
	
	public void clear() {
		for( int i=width*height-1; i>=0; --i ) {
			e[i] = r[i] = g[i] = b[i] = 0;
		}
	}
	
	public double getAverageExposure() {
		double sum = 0;
		for( int i=width*height-1; i>=0; --i ) {
			sum += e[i];
		}
		return sum / (width*height);
	}
}
