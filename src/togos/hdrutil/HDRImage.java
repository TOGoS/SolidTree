// Auto-generated; see Makefile.
package togos.hdrutil;

public class HDRImage
{
	final int width, height;
	final HDRChannel r, g, b;
	final HDRChannel[] colorChannels;
	
	public HDRImage( int width, int height ) {
		this.r = new HDRChannel(width*height);
		this.g = new HDRChannel(width*height);
		this.b = new HDRChannel(width*height);
		this.colorChannels = new HDRChannel[]{ r, g, b };
		this.width = width;
		this.height = height;
	}
	
	public void load( HDRExposure e ) {
		assert width == e.width;
		assert height == e.height;
		
		for( int i=width*height-1; i>=0; --i ) {
			r.data[i] = e.r.data[i] / e.e.data[i];
			g.data[i] = e.g.data[i] / e.e.data[i];
			b.data[i] = e.b.data[i] / e.e.data[i];
		}
	}
	
	
	public void multiply( float m ) {
		for( int c=0; c<colorChannels.length; ++c ) colorChannels[c].multiply(m);
	}
	
	public void exponentiate( float p ) {
		for( int c=0; c<colorChannels.length; ++c ) colorChannels[c].exponentiate(p);
	}

	public float max() {
		float max = Float.NEGATIVE_INFINITY;
		for( int c=0; c<colorChannels.length; ++c ) {
			for( int i=width*height-1; i>=0; --i ) {
				max = Math.max( max, colorChannels[c].data[i] );
			}
		}
		return max;
	}

	protected static int clampByte( int i ) {
		return i < 0 ? 0 : i > 255 ? 255 : i;
	}
	
	public void toArgb( int[] dest, boolean dither ) {
		for( int i=width*height-1; i>=0; --i ) {
			// Ideal values
			float iR = 255 * r.data[i];
			float iG = 255 * g.data[i];
			float iB = 255 * b.data[i];
			// Quantized values
			// TODO: Use a better dithering algorithm
			int qR, qG, qB;
			if( dither ) {
				qR = (int)Math.round(iR + (Math.random() - 0.5));
				qG = (int)Math.round(iG + (Math.random() - 0.5));
				qB = (int)Math.round(iB + (Math.random() - 0.5));
			} else {
				qR = (int)Math.round(iR);
				qG = (int)Math.round(iG);
				qB = (int)Math.round(iB);
			}
			
			dest[i] =
				0xFF000000 |
				(clampByte(qR) << 16) |
				(clampByte(qG) <<  8) | 
				(clampByte(qB) <<  0);
		}
	}

	public int getWidth() { return width; }
	public int getHeight() { return height; }
}
