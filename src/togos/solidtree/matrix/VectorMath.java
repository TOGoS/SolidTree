package togos.solidtree.matrix;


public class VectorMath
{
	private VectorMath() { }
	
	public static double dotProduct( Vector3D a, Vector3D b ) {
		return a.x * b.x + a.y * b.y + a.z * b.z;
	}
	
	public static void scale( Vector3D a, double s, Vector3D dest ) {
		dest.x = a.x * s;
		dest.y = a.y * s;
		dest.z = a.z * s;
	}
	
	public static void add( Vector3D a, Vector3D b, Vector3D dest ) {
		dest.x = a.x + b.x;
		dest.y = a.y + b.y;
		dest.z = a.z + b.z;
	}
	
	public static void subtract( Vector3D a, Vector3D b, Vector3D dest ) {
		dest.x = a.x - b.x;
		dest.y = a.y - b.y;
		dest.z = a.z - b.z;
	}
	
	public static void reflect( Vector3D v, Vector3D across, Vector3D dest ) {
		double dot = dotProduct( v, across );
		dest.x = -2 * dot * across.x + v.x;
		dest.y = -2 * dot * across.y + v.y;
		dest.z = -2 * dot * across.z + v.z;
	}
	
	public static double dist( Vector3D a, Vector3D b ) {
		double dx = a.x - b.x;
		double dy = a.y - b.y;
		double dz = a.z - b.z;
		return (double)Math.sqrt( dx*dx + dy*dy + dz*dz );
	}
	
	public static boolean isNormalized( double x, double y, double z ) {
		double len = x*x + y*y + z*z;
		// No need to sqrt since we only care if it's 1 or not!
		return len > 0.99999 && len < 1.00001; 
	}
	
	public static void normalize( Vector3D normal, double scale, Vector3D dest ) {
		dest.set(normal);
		dest.normalizeInPlace(scale);
    }
}
