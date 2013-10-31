package togos.hdrutil;

public class HDRExposure
{
	public final int width, height;
	/** Virtual exposure time for each pixel */
	public final HDRChannel e;
	/** Sum of red, green, and blue over entire exposure for each pixel */ 
	public final HDRChannel r, g, b;
	
	public HDRExposure( int width, int height ) {
		assert Util.dimensionsSane(width, height);
		
		this.e = new HDRChannel(width*height);
		this.r = new HDRChannel(width*height);
		this.g = new HDRChannel(width*height);
		this.b = new HDRChannel(width*height);
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
		
		e.add(other.e);
		r.add(other.r);
		g.add(other.g);
		b.add(other.b);
	}
	
	public HDRImage getImage() {
		HDRImage img = new HDRImage( width, height );
		img.load(this);
		return img;
	}
	
	public void clear() {
		for( int i=width*height-1; i>=0; --i ) {
			e.data[i] = r.data[i] = g.data[i] = b.data[i] = 0;
		}
	}
	
	public double getAverageExposure() {
		double sum = 0;
		for( int i=width*height-1; i>=0; --i ) {
			sum += e.data[i];
		}
		return sum / (width*height);
	}
}
