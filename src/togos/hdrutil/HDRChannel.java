// Auto-generated; see Makefile.
package togos.hdrutil;

public class HDRChannel {
	public final float[] data;
	
	public HDRChannel( int size ) {
		this.data = new float[size];
	}
	
	public void set( float value ) {
		for( int i=data.length-1; i>=0; --i ) data[i] = value;
	}
	
	public boolean isConstant( float value ) {
		for( int i=data.length-1; i>=0; --i ) if(data[i] != value) return false;
		return true;
	}
	
	public void multiply( float v ) {
		for( int i=data.length-1; i>=0; --i ) data[i] *= v;
	}
	
	public void exponentiate( float p ) {
		for( int i=data.length-1; i>=0; --i ) data[i] = (float)Math.pow(data[i], p);
	}
	
	////
	
	protected void add( HDRChannel other ) {
		for( int i=data.length-1; i>=0; --i ) data[i] += other.data[i];
	}
	
	protected void multiply( HDRChannel other ) {
		for( int i=data.length-1; i>=0; --i ) data[i] *= other.data[i];
	}
	
	protected void divide( HDRChannel other ) {
		for( int i=data.length-1; i>=0; --i ) data[i] /= other.data[i];
	}
}
