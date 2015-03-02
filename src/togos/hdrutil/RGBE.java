package togos.hdrutil;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * Documents on the Radiance RGBE format:
 * 
 * https://www.andrew.cmu.edu/user/yihuang/radiance_pic/rgbe.java
 * http://www.graphics.cornell.edu/~bjw/rgbe.html
 * http://www.graphics.cornell.edu/~bjw/rgbe/rgbe.c
 * 
 * exponent = exponent byte - 128
 * component value  = component byte * (2 ** exponent) / 256
 * 
 * i.e. component bytes represent 256ths of 2**exponent
 * 
 * Though the exponent is irrelevant when all significands are zero,
 * this case is conventionally represented as 0x00000000.
 * I suppose this might aid general purpose compression of
 * image files containing lots of black.
 */
public class RGBE
{
	// IEEE float32s can't actually represent numbers this big,
	// so this is probably redundant.
	static final int MIN_NONZERO_EXPONENT = -127;
	static final int MIN_EXPONENT = -128;
	static final int MAX_EXPONENT = +127;
	
	static final float MIN_NONZERO_VALUE = (float)(0.5/256 * Math.pow(2, MIN_NONZERO_EXPONENT));
	
	protected static final int toInt32( int a, int b, int c, int d ) {
		return
			((a << 24) & 0xFF000000) |
			((b << 16) & 0x00FF0000) |
			((c <<  8) & 0x0000FF00) |
			((d <<  0) & 0x000000FF);
	}
	
	protected static final int component( int composite, int shift ) {
		return (composite >> shift) & 0xFF;
	}
	
	protected static int clampUByte( float v ) {
		return (int)(v < 0 ? 0 : v > 255 ? 255 : v);
	}
	protected static int clampExponent( double v ) {
		return (int)(v < -128 ? -128 : v > 127 ? 127 : v);
	}
	
	public static int encode( float r, float g, float b ) {
		assert r >= 0 && g >= 0 && b >= 0;
		
		float max = r > g ? r : g;
		max = max > b ? max : b;
		if( max < MIN_NONZERO_VALUE ) return 0;
		
		int exponent = clampExponent(Math.ceil(Math.log(max)/Math.log(2)));
		float exponentFactor = (float)Math.pow(2, exponent);
		if( max / exponentFactor >= 255.5/256 && exponent < 127 ) {
			// Need a larger exponent to represent this!
			// Translate (256/256) * 2**e to (128/256) * 2**(e+1)
			++exponent;
			exponentFactor *= 2;
		}
		assert exponent >= MIN_EXPONENT;
		assert exponent <= MAX_EXPONENT;
		
		return toInt32(
			clampUByte(256 * r / exponentFactor + 0.5f),
			clampUByte(256 * g / exponentFactor + 0.5f),
			clampUByte(256 * b / exponentFactor + 0.5f),
			exponent + 128
		);
	}
	
	public static String inspect( int rgbe ) {
		int
			rb = component( rgbe, 24 ),
			gb = component( rgbe, 16 ),
			bb = component( rgbe,  8 ),
			eb = component( rgbe,  0 );
		int exponent = eb - 128;
		float factor = eb == 0 ? 0 : (float)Math.pow(2, exponent - 8);
		return "RGBE "+rb+" "+gb+" "+bb+" "+eb+" ("+exponent+") = "+(rb*factor)+" "+(gb*factor)+" "+(bb*factor);
	}
	
	public static void encode( float[] r, float[] g, float[] b, int[] rgbe, int count ) {
		assert r.length >= count;
		assert g.length >= count;
		assert b.length >= count;
		assert rgbe.length >= count;
		for( int i=count-1; i>=0; --i ) rgbe[i] = encode(r[i], g[i], b[i]);
	}
	
	public static void decode( int[] rgbe, float[] r, float[] g, float[] b, int count ) {
		assert r.length >= count;
		assert g.length >= count;
		assert b.length >= count;
		assert rgbe.length >= count;
		for( int i=count-1; i>=0; --i ) {
			int
				rb = component( rgbe[i], 24 ),
				gb = component( rgbe[i], 16 ),
				bb = component( rgbe[i],  8 ),
				eb = component( rgbe[i],  0 );
			float factor = eb == 0 ? 0 : (float)Math.pow(2, eb - 128 - 8);
			r[i] = rb*factor;
			g[i] = gb*factor;
			b[i] = bb*factor;
		}
	}
	
	//// Read!
	
	static class ImageMetadata {
		public final int width;
		public final int height;
		public final int spp;
		public ImageMetadata( int w, int h, int spp ) {
			this.width = w;
			this.height = h;
			this.spp = spp;
		}
	}
	
	static final Pattern DIMS_EXTENSION_PATTERN = Pattern.compile("\\.(\\d+)x(\\d+)\\.");
	static final Pattern SPP_EXTENSION_PATTERN = Pattern.compile("\\.(\\d+)spp\\.");
	
	static ImageMetadata guessMetadata( String filename ) {
		int w = -1, h = -1, spp = -1;
		Matcher m;
		
		m = DIMS_EXTENSION_PATTERN.matcher(filename);
		// 'while' instead of 'if' to always find the last occurrence,
		while( m.find() ) {
			w = Integer.parseInt(m.group(1));
			h = Integer.parseInt(m.group(2));
		}
		
		m = SPP_EXTENSION_PATTERN.matcher(filename);
		while( m.find() ) {
			spp = Integer.parseInt(m.group(1));
		}
		
		return new ImageMetadata( w, h, spp );
	}
	
	public static void readData( DataInputStream dis, int w, int h, float[] r, float[] g, float[] b ) throws IOException {
		assert Util.dimensionsSane(w, h);
		int size = w*h;
		
		int[] buf = new int[size];
		for( int i=0; i<size; ++i ) buf[i] = dis.readInt();
		
		decode( buf, r, g, b, size );
	}
	
	public static HDRExposure loadExposureFromRawRgbe( File f ) throws IOException {
		ImageMetadata m = guessMetadata(f.getName());
		if( m.width == -1 || m.height == -1 ) {
			throw new IOException("Dimensions not found in filename: "+f.getName());
		}
		if( m.spp == -1 ) {
			throw new IOException("SPP not found in filename: "+f.getName());
		}
		
		HDRExposure exp = new HDRExposure(m.width, m.height);
		
		DataInputStream dis = new DataInputStream(new FileInputStream(f));
		try {
			readData( dis, m.width, m.height, exp.r, exp.g, exp.b );
			BatchMath.multiply(exp.r, m.spp, exp.r);
			BatchMath.multiply(exp.g, m.spp, exp.g);
			BatchMath.multiply(exp.b, m.spp, exp.b);
			Util.fill(exp.e, m.spp);
			return exp;
		} finally {
			dis.close();
		}
	}
	
	//// Write!
	
	public static String rawRgbeExtension( int width, int height, int spp ) {
		return width+"x"+height+(spp != -1 ? "."+spp+"spp" : "")+".rgbe";
	}
	
	public static void write( float[] r, float[] g, float[] b, float[] exp, DataOutputStream dos, int size ) throws IOException {
		for( int i=0; i<size; ++i ) {
			dos.writeInt(encode(r[i]/exp[i], g[i]/exp[i], b[i]/exp[i]));
		}
	}
	
	public static void write( HDRExposure exp, File f ) throws IOException {
		DataOutputStream dos = new DataOutputStream(new FileOutputStream(f));
		try {
			write( exp.r, exp.g, exp.b, exp.e, dos, exp.width*exp.height );
		} finally {
			dos.close();
		}
	}
	
	public File writeAutoExtension( HDRExposure exp, String basename ) throws IOException {
		File f = new File(basename + "." + rawRgbeExtension(exp.width, exp.height, (int)exp.getAverageExposure()));
		write( exp, f );
		return f;
	}
	
	////
	
	public static void main( String[] args ) {
		System.err.println(inspect(encode(0.001f,0,0)));
		System.err.println(inspect(encode(1,16,255)));
		System.err.println(inspect(encode(1,16,255.4f)));
		System.err.println(inspect(encode(1,16,255.6f)));
		System.err.println(inspect(encode(0.25f,0.5f,1.0f)));
		System.err.println(inspect(encode(0.25f,16,32)));
		System.err.println(inspect(encode(0.25f,16,64)));
		System.err.println(inspect(encode(0,0,0)));
	}
}
