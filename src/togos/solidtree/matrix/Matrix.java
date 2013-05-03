package togos.solidtree.matrix;

public class Matrix
{
	final int columns, rows;
	
	/** [row][column] */
	final double[][] values;
	
	public Matrix( int rows, int columns ) {
		this.columns = columns;
		this.rows    = rows;
		this.values = new double[rows][columns];
	}
	
	public double get( int row, int col ) {
		return values[row][col];
	}
	
	public void set( int row, int col, double v ) {
		values[row][col] = v;
	}
}
