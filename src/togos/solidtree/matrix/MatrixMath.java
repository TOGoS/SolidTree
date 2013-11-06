package togos.solidtree.matrix;

public class MatrixMath
{
	public static void identity( Matrix m ) {
		assert m.width == m.height;
		m.clear();
		for( int i=0; i<m.width; ++i ) m.put(i, i, 1);
	}
	
	public static void multiply( Matrix a, Matrix b, Matrix dest ) {
		if( dest.width != b.width ) {
			throw new RuntimeException("Destination matrix width is wrong");
		}
		if( dest.height != b.height ) {
			throw new RuntimeException("Destination matrix height is wrong");
		}
		if( a.width != b.height ) {
			throw new RuntimeException("a.width != b.height");
		}
		for( int y=dest.height-1; y>=0; --y ) {
			for( int x=dest.width-1; x>=0; --x ) {
				double v = 0;
				for( int i=a.width-1; i>=0; --i ) {
					v += a.get(y,i) * b.get(i,x);
				}
				dest.put(y, x, v);
			}
		}
	}
	
	public static void multiply( Matrix a, Vector3D b, Vector3D dest ) {
		assert a.width  >= 4;
		assert a.height >= 3;
		
		dest.x = a.get(0,0) * b.x + a.get(0,1) * b.y + a.get(0,2) * b.z + a.get(0,3);
		dest.y = a.get(1,0) * b.x + a.get(1,1) * b.y + a.get(1,2) * b.z + a.get(1,3);
		dest.z = a.get(2,0) * b.x + a.get(2,1) * b.y + a.get(2,2) * b.z + a.get(2,3);
	}
	
	public static void axisAngleToRotationMatrix( double ax, double ay, double az, double angle, Matrix dest ) {
		assert VectorMath.isNormalized(ax, ay, az);
		assert dest.width >= 3 && dest.height >= 3;
		double sin = (double)Math.sin(angle);
		double cos = (double)Math.cos(angle);
		double xx = ax*ax;
		double yy = ay*ay;
		double zz = az*az;
		double icos = 1-cos;
		
		dest.clear();
		dest.put(3, 3, 1);
		
		// https://en.wikipedia.org/wiki/Rotation_matrix#Rotation_matrix_from_axis_and_angle
		dest.put(0, 0,    cos + xx*icos);
		dest.put(1, 0, ay*ax*icos + az*sin);
		dest.put(2, 0, az*ax*icos - ay*sin);
		
		dest.put(0, 1, ax*ay*icos - az*sin);
		dest.put(1, 1,    cos + yy*icos);
		dest.put(2, 1, az*ay*icos + ax*sin);
		
		dest.put(0, 2, ax*az*icos + ay*sin);
		dest.put(1, 2, ay*az*icos - ax*sin);
		dest.put(2, 2,    cos + zz*icos);
	}
	
	public static void translation(double x, double y, double z, Matrix dest) {
		identity(dest);
		dest.put(0, 3, x);
		dest.put(1, 3, y);
		dest.put(2, 3, z);
	}
	
	public static void yawPitchRoll(double yaw, double pitch, double roll, Matrix scratchA, Matrix scratchB, Matrix dest) {
		identity(scratchA);
		axisAngleToRotationMatrix( 0, -1, 0, yaw  , scratchB );
		multiply(scratchA, scratchB, dest);
		axisAngleToRotationMatrix( 1, 0, 0, pitch, scratchB );
		multiply(dest, scratchB, scratchA);
		axisAngleToRotationMatrix( 0, 0, 1, roll , scratchB );
		multiply(scratchA, scratchB, dest);
	}
}
