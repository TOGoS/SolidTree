package togos.solidtree.trace;

final class Vector3D {
	double x, y, z;
	
	public void set( double x, double y, double z ) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public void set( Vector3D other ) {
		set( other.x, other.y, other.z );
	}
	
	public void scale( double d ) {
		x *= d;
		y *= d;
		z *= d;
	}
	
	public double magnitude() {
		return Math.sqrt( x*x + y*y + z*z );
	}
	
	public void normalize( double scale ) {
		double m = magnitude();
		if( m != 0 ) {
			x *= scale / m;
			y *= scale / m;
			z *= scale / m;
		}
	}
	
	public boolean isZero() {
		return x == 0 && y == 0 && z == 0;
	}
}