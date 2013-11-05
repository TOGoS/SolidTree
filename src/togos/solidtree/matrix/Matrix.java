package togos.solidtree.matrix;

public class Matrix
{
	public final int width, height;
	/** [row*width+column] */
	private final double[] values;
	
	public Matrix( int height, int width ) {
		this.width  = width;
		this.height = height;
		this.values = new double[height*width];
	}
	
	public final double get( int row, int col ) {
		return values[row*height+col];
	}
	
	public final void put( int row, int col, double v ) {
		values[row*height+col] = v;
	}
	
	public void clear() {
		for( int r=0; r<height; ++r ) for( int c=0; c<width; ++c ) {
			put(r, c, 0);
		}
	}
	
	////
	
	public Matrix multiply( Matrix other ) {
		Matrix dest = new Matrix(this.height, other.width);
		MatrixMath.multiply(this, other, dest);
		return dest;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for( int r=0; r<height; ++r ) {
			if( r != 0 ) sb.append("\n");
			sb.append( r == 0 ? "/" : r == height-1 ? "\\" : "|");
			for( int c=0; c<width; ++c ) {
				sb.append( String.format(" % 5.2f",get(r,c)) );
			}
			sb.append(" ");
			sb.append( r == 0 ? "\\" : r == height-1 ? "/" : "|");
		}
		return sb.toString();
	}
}
