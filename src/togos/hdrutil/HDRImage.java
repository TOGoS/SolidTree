package togos.hdrutil;

public class HDRImage
{
	final int width, height;
	final Channel r, g, b;
	final Channel[] colorChannels;
	
	public HDRImage( int width, int height ) {
		this.r = new Channel(width*height);
		this.g = new Channel(width*height);
		this.b = new Channel(width*height);
		this.colorChannels = new Channel[]{ r, g, b };
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
	
	
	public void multiply( double m ) {
		for( int c=0; c<colorChannels.length; ++c ) colorChannels[c].multiply(m);
	}
	
	public void exponentiate( double p ) {
		for( int c=0; c<colorChannels.length; ++c ) colorChannels[c].exponentiate(p);
	}

	public double max() {
		double max = Double.NEGATIVE_INFINITY;
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
			double iR = 255 * r.data[i];
			double iG = 255 * g.data[i];
			double iB = 255 * b.data[i];
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
}
