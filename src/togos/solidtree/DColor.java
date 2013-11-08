package togos.solidtree;

import java.io.Serializable;
import java.util.Random;

public class DColor implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	public static final DColor BLACK = new DColor(0,0,0);
	public static final DColor WHITE = new DColor(1,1,1);
	
	public double r, g, b;
	
	public DColor( double r, double g, double b ) {
		set(r,g,b);
	}
	
	public final void set( double r, double g, double b ) {
		this.r = r;
		this.g = g;
		this.b = b;
	}
	
	protected static int clampByte( int i ) {
		return i < 0 ? 0 : i > 255 ? 255 : i;
	}
	
	public static int toArgb( double r, double g, double b, Random dither ) {
		double iR = 255 * r;
		double iG = 255 * g;
		double iB = 255 * b;
		
		int qR = (int)Math.round(iR + (dither.nextDouble() - 0.5));
		int qG = (int)Math.round(iG + (dither.nextDouble() - 0.5));
		int qB = (int)Math.round(iB + (dither.nextDouble() - 0.5));
		
		return
			0xFF000000 |
			(clampByte(qR) << 16) |
			(clampByte(qG) <<  8) | 
			(clampByte(qB) <<  0);
	}
}
