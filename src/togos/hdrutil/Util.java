package togos.hdrutil;

public class Util
{
	public static boolean dimensionsSane( int w, int h ) {
		if( w < 0 || h < 0 ) return false;
		long size = w*h;
		return size <= Integer.MAX_VALUE;
	}
}
