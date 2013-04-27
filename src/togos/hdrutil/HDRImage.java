package togos.hdrutil;

public class HDRImage
{
	static class Channel {
		final double[] data;
		
		public Channel( int size ) {
			this.data = new double[size];
		}
		
		public void set( double value ) {
			for( int i=data.length-1; i>=0; --i ) data[i] = value;
		}
		
		public void multiply( double v ) {
			for( int i=data.length-1; i>=0; --i ) data[i] *= v;
		}
		
		public void exponentiate( double p ) {
			for( int i=data.length-1; i>=0; --i ) data[i] = Math.pow(data[i], p);
		}
		
		////
		
		protected void add( Channel other ) {
			for( int i=data.length-1; i>=0; --i ) data[i] += other.data[i];
		}
		
		protected void multiply( Channel other ) {
			for( int i=data.length-1; i>=0; --i ) data[i] *= other.data[i];
		}
		
		protected void divide( Channel other ) {
			for( int i=data.length-1; i>=0; --i ) data[i] /= other.data[i];
		}
	}
	
	// Standard channels:
	// 0 = path trace iterations
	// 1 = red
	// 2 = green
	// 3 = blue
	
	final int width, height;
	final Channel[] channels;
	
	public HDRImage( int nChannels, int width, int height ) {
		this.channels = new Channel[nChannels];
		this.width = width;
		this.height = height;
		for( int i=0; i<nChannels; ++i ) {
			channels[i] = new Channel(width*height);
		}
	}
	
	public boolean isCompatible( HDRImage other ) {
		return other.width == this.width && other.height == this.height && other.channels.length == this.channels.length;
	}
	
	public void add( HDRImage other ) throws IncompatibleImageException {
		if( !isCompatible(other) ) throw new IncompatibleImageException("Cannot add(); images are incompatible");
		
		for( int i=0; i<channels.length; ++i ) {
			channels[i].add( other.channels[i] );
		}
	}
	
	/**
	 * Divides every channel by the values in channel i.
	 */
	public void normalize( int byChannelId ) {
		assert byChannelId >= 0;
		assert byChannelId < channels.length;
		
		for( int i=0; i<channels.length; ++i ) {
			if( i != byChannelId ) {
				channels[i].divide(channels[byChannelId]);
			}
		}
		channels[byChannelId].set(1);
	}
	
	public void multiply( double m ) {
		for( int i=0; i<channels.length; ++i ) channels[i].multiply(m);
	}
	
	public void exponentiate( double p ) {
		for( int i=0; i<channels.length; ++i ) channels[i].exponentiate(p);
	}
	
	protected static int clampByte( int i ) {
		return i < 0 ? 0 : i > 255 ? 255 : i;
	}
	
	public void toArgb( int[] dest ) {
		assert channels.length == 4;
		assert dest.length >= width*height;
		
		for( int i=width*height-1; i>=0; --i ) {
			// Ideal values
			double iR = 255 * channels[1].data[i] / channels[0].data[i];
			double iG = 255 * channels[2].data[i] / channels[0].data[i];
			double iB = 255 * channels[3].data[i] / channels[0].data[i];
			// Quantized values
			// TODO: Use a better dithering algorithm
			int qR = (int)Math.round(iR + (Math.random() - 0.5));
			int qG = (int)Math.round(iG + (Math.random() - 0.5));
			int qB = (int)Math.round(iB + (Math.random() - 0.5));
			
			dest[i] =
				0xFF000000 |
				(clampByte(qR) << 24) |
				(clampByte(qG) << 24) | 
				(clampByte(qB) << 24);
		}
	}
}
