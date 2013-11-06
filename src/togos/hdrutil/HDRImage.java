package togos.hdrutil;

public class HDRImage
{
	final int width, height;
	final float[] r, g, b;
	final float[][] colorChannels;
	
	public HDRImage( int width, int height ) {
		this.r = new float[width*height];
		this.g = new float[width*height];
		this.b = new float[width*height];
		this.colorChannels = new float[][]{ r, g, b };
		this.width = width;
		this.height = height;
	}
	
	public void load( HDRExposure e ) {
		assert width == e.width;
		assert height == e.height;
		
		for( int i=width*height-1; i>=0; --i ) {
			r[i] = e.r[i] / e.e[i];
			g[i] = e.g[i] / e.e[i];
			b[i] = e.b[i] / e.e[i];
		}
	}
	
	public void multiply( float m ) {
		for( int c=0; c<colorChannels.length; ++c ) BatchMath.multiply( colorChannels[c], m, colorChannels[c] );
	}
	
	public void exponentiate( float p ) {
		for( int c=0; c<colorChannels.length; ++c ) BatchMath.exponentiate( colorChannels[c], p, colorChannels[c] );
	}

	public float max() {
		float max = Float.NEGATIVE_INFINITY;
		for( int c=0; c<colorChannels.length; ++c ) {
			for( int i=width*height-1; i>=0; --i ) {
				max = Math.max( max, colorChannels[c][i] );
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
			float iR = 255 * r[i];
			float iG = 255 * g[i];
			float iB = 255 * b[i];
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
