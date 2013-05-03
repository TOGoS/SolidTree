package togos.solidtree.matrix;

import junit.framework.TestCase;

public class MatrixMathTest extends TestCase
{
	public void testMultiplyMatrixVector() {
		Matrix m = new Matrix(4,4);
		Matrix v = new Matrix(4,1);
		Matrix d = new Matrix(4,1);
		
		m.set(0,0,3);
		m.set(1,0,2);
		m.set(2,0,1);
		m.set(3,3,1);
		
		v.set(0,0,1);
		v.set(1,0,0);
		v.set(2,0,0);
		v.set(3,0,1);
		
		MatrixMath.multiply(m, v, d);
		
		assertEquals( 3d, d.get(0,0) );
		assertEquals( 2d, d.get(1,0) );
		assertEquals( 1d, d.get(2,0) );
		assertEquals( 1d, d.get(3,0) );
	}
}
