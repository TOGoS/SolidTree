package togos.hdrutil;

public class HDRExposure
{
	public final int width, height;
	public final Channel e, r, g, b;
	
	public HDRExposure( int width, int height ) {
		this.e = new Channel(width*height);
		this.r = new Channel(width*height);
		this.g = new Channel(width*height);
		this.b = new Channel(width*height);
		this.width = width;
		this.height = height;
	}
	
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
}
