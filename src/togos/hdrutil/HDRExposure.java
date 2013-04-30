// Auto-generated; see Makefile.
package togos.hdrutil;

public class HDRExposure
{
	public final int width, height;
	public final HDRChannel e, r, g, b;
	
	public HDRExposure( int width, int height ) {
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
	
}
