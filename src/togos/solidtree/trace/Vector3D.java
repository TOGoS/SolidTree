package togos.solidtree.trace;

final class Vector3D {
	double x, y, z;
	
	public Vector3D() { }
	
	public Vector3D( Vector3D copyOf ) {
		set(copyOf);
	}
	
	public Vector3D( double x, double y, double z ) {
		set(x,y,z);
	}
	
	public void set( double x, double y, double z ) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public void set( Vector3D other ) {
		set( other.x, other.y, other.z );
	}
	
	public void scaleInPlace( double d ) {
		x *= d;
		y *= d;
		z *= d;
	}
	
	public double magnitude() {
		return (double)Math.sqrt( x*x + y*y + z*z );
	}
	
	public void normalizeInPlace( double scale ) {
		double m = magnitude();
		
		assert m != 0;
		
		x *= scale / m;
		y *= scale / m;
		z *= scale / m;
	}
	
	////
	
	public boolean isZero() {
		return x == 0 && y == 0 && z == 0;
	}

	public boolean isDefined() {
		return !Double.isNaN(x) && !Double.isNaN(y) && !Double.isNaN(z);
    }
	
	public boolean isInfinite() {
		return Double.isInfinite(x) && Double.isInfinite(y) && Double.isInfinite(z);
    }

	public boolean isFinite() {
		return isDefined() && !isInfinite(); 
    }
	
	public boolean isRegular() {
		return isFinite() && !isZero(); 
    }
	
	public String toString() {
		return "<"+x+", "+y+", "+z+">";
	}
	
	////
	
	public Vector3D normalize( double magnitude ) {
		Vector3D res = new Vector3D(this);
		res.normalizeInPlace(magnitude);
		return res;
	}
	
	public Vector3D normalize() {
		return new Vector3D(this).normalize(1);
	}
	
	public Vector3D add( Vector3D other ) {
		Vector3D res = new Vector3D();
		VectorMath.add(this,other,res);
		return res;
	}
	
	public Vector3D subtract( Vector3D other ) {
		Vector3D res = new Vector3D();
		VectorMath.subtract(this,other,res);
		return res;
	}
	
	public Vector3D scale( double s ) {
		Vector3D res = new Vector3D(this);
		res.scaleInPlace(s);
		return res;
	}

	public double dot( Vector3D other ) {
		return VectorMath.dotProduct(this, other);
    }
}
