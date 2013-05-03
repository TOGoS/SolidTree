package togos.solidtree.matrix;

public class MatrixMath
{
	public static void multiply( Matrix a, Matrix b, Matrix dest ) {
		if( dest.columns != b.columns ) {
			throw new RuntimeException("Destination matrix width is wrong");
		}
		if( dest.rows != b.rows ) {
			throw new RuntimeException("Destination matrix height is wrong");
		}
		if( a.columns != b.rows ) {
			throw new RuntimeException("a.width != b.height");
		}
		for( int y=dest.rows-1; y>=0; --y ) {
			for( int x=dest.columns-1; x>=0; --x ) {
				double v = 0;
				for( int i=a.columns-1; i>=0; --i ) {
					v += a.values[y][i] * b.values[i][x];
				}
				dest.values[y][x] = v;
			}
		}
	}
}
