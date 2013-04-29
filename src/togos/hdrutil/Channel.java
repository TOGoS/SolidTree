package togos.hdrutil;

public class Channel {
	public final double[] data;
	
	public Channel( int size ) {
		this.data = new double[size];
	}
	
	public void set( double value ) {
		for( int i=data.length-1; i>=0; --i ) data[i] = value;
	}
	
	public boolean isConstant( double value ) {
		for( int i=data.length-1; i>=0; --i ) if(data[i] != value) return false;
		return true;
	}
	
	public void multiply( double v ) {
		for( int i=data.length-1; i>=0; --i ) data[i] *= v;
	}
	
	public void exponentiate( double p ) {
		for( int i=data.length-1; i>=0; --i ) data[i] = Math.pow(data[i], p);
	}
	
	////
	
	protected void add( Channel other ) {
		for( int i=data.length-1; i>=0; --i ) data[i] += other.data[i];
	}
	
	protected void multiply( Channel other ) {
		for( int i=data.length-1; i>=0; --i ) data[i] *= other.data[i];
	}
	
	protected void divide( Channel other ) {
		for( int i=data.length-1; i>=0; --i ) data[i] /= other.data[i];
	}
}
