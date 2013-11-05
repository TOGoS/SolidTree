package togos.solidtree.matrix;

import junit.framework.TestCase;

public class MatrixMathTest extends TestCase
{
	public void testMultiplyMatrixVector() {
		Matrix m = new Matrix(4,4);
		Matrix v = new Matrix(4,1);
		Matrix d = new Matrix(4,1);
		
		m.put(0,0,3);
		m.put(1,0,2);
		m.put(2,0,1);
		m.put(3,3,1);
		
		v.put(0,0,1);
		v.put(1,0,0);
		v.put(2,0,0);
		v.put(3,0,1);
		
		MatrixMath.multiply(m, v, d);
		
		assertEquals( 3d, d.get(0,0) );
		assertEquals( 2d, d.get(1,0) );
		assertEquals( 1d, d.get(2,0) );
		assertEquals( 1d, d.get(3,0) );
	}
}
