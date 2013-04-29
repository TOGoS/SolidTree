package togos.solidtree.trace;

public class Math3D
{
	private Math3D() { }
	
	public static double dotProduct( Vector3D a, Vector3D b ) {
		return a.x * b.x + a.y * b.y + a.z * b.z;
	}
	
	public static void add( Vector3D a, Vector3D b, Vector3D dest ) {
		dest.x = a.x + b.x;
		dest.y = a.y + b.y;
		dest.z = a.z + b.z;
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
		return Math.sqrt( dx*dx + dy*dy + dz*dz );
	}
}
