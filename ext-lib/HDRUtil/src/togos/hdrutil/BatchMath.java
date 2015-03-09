package togos.hdrutil;

public class BatchMath
{
	public static void add( float[] a, float[] b, float[] dest ) {
		assert a.length == b.length;
		assert a.length == dest.length;
		for( int i=dest.length-1; i>=0; --i ) dest[i] = a[i] + b[i];
	}

	public static void exponentiate( float[] a, float p, float[] dest ) {
		assert a.length == dest.length;
		for( int i=dest.length-1; i>=0; --i ) dest[i] = (float)Math.pow(a[i], p);
	}

	public static void multiply( float[] a, float m, float[] dest ) {
		assert a.length == dest.length;
		for( int i=dest.length-1; i>=0; --i ) dest[i] = a[i] * m;
	}
}
