package togos.hdrutil;



public class ExposureScaler
{
	protected static final int clamp( int min, int v, int max ) {
		return v < min ? min : v > max ? max : v;
	}
	
	static final float s = 0.5f / 4; // 1x1 away
	static final float m = 0.2f / 4; // 1x3 away
	static final float l = 0.1f / 4; // 3x3 away
	
	static final float[][] weights = new float[][] {
		new float[] { l, m, m, s },
		new float[] { m, l, s, m },
		new float[] { m, s, l, m },
		new float[] { s, m, m, l }
	};
	
	public static HDRExposure scaleUp( HDRExposure exp ) {
		final int[] idx = new int[4];
		
		final HDRExposure big = new HDRExposure(exp.width*2, exp.height*2);
		
		final float[] er = exp.r;
		final float[] eg = exp.g;
		final float[] eb = exp.b;
		final float[] ee = exp.e;
		final float[] br = big.r;
		final float[] bg = big.g;
		final float[] bb = big.b;
		final float[] be = big.e;
		
		for( int i=big.height*big.width-1, y=big.height-1; y>=0; --y ) for( int x=big.width-1; x>=0; --x, --i ) {
			int x0 = clamp(0, (x-1)/2, exp.width -1 );
			int x1 = clamp(0, (x+1)/2, exp.width -1 );
			int y0 = clamp(0, (y-1)/2, exp.height-1 );
			int y1 = clamp(0, (y+1)/2, exp.height-1 );
			idx[0] = y0*exp.width + x0;
			idx[1] = y0*exp.width + x1;
			idx[2] = y1*exp.width + x0;
			idx[3] = y1*exp.width + x1;
			
			float[] w = weights[((y&1)<<1) + (x&1)]; 
			
			for( int j=0; j<4; ++j ) {
				br[i] += er[idx[j]]*w[j];
				bg[i] += eg[idx[j]]*w[j];
				bb[i] += eb[idx[j]]*w[j];
				be[i] += ee[idx[j]]*w[j];
			}
		}
		
		return big;
	}
	
	public static HDRExposure scaleDown( HDRExposure exp, int ratio ) {
		final HDRExposure ltl = new HDRExposure(exp.width/ratio, exp.height/ratio);
		
		final float[] er = exp.r;
		final float[] eg = exp.g;
		final float[] eb = exp.b;
		final float[] ee = exp.e;
		final float[] lr = ltl.r;
		final float[] lg = ltl.g;
		final float[] lb = ltl.b;
		final float[] le = ltl.e;
		
		for( int i=ltl.height*ltl.width-1, y=ltl.height-1; y>=0; --y ) for( int x=ltl.width-1; x>=0; --x, --i ) {
			for( int jy=0; jy<ratio; ++jy ) for( int jx=0; jx<ratio; ++jx ) {
				int eIdx = (x*ratio+jx) + (y*ratio+jy)*exp.width;
				lr[i] += er[eIdx];
				lg[i] += eg[eIdx];
				lb[i] += eb[eIdx];
				le[i] += ee[eIdx];
			}
		}
		
		return ltl;
	}
	
	public static boolean aspectRatioPreserved( int w1, int h1, int w2, int h2 ) {
		if( h1 > w1 ) return aspectRatioPreserved( h1, w1, h2, w2 );
		
		return (double)w1 / h1 == (double)w2 / h2;
	}
	
	public static HDRExposure scaleTo( HDRExposure exp, int width, int height ) {
		if( !aspectRatioPreserved(exp.width, exp.height, width, height) ) {
			throw new UnsupportedOperationException("Cannot change aspect ratio");
		}
		
		double scale = width / exp.width;
		if( scale == 0 ) {
			return new HDRExposure(0, 0);
		} else if( scale < 1 ) {
			double ratio = exp.width / width;
			if( (int)ratio != ratio ) {
				throw new UnsupportedOperationException("Cannot scale down by non-integer ratio "+scale);
			}
			return scaleDown( exp, (int)ratio );
		} else {
			int iScale = (int)scale;
			if( iScale != scale ) {
				throw new UnsupportedOperationException("Cannot scale by non-integer "+scale);
			}
			while( iScale > 0 && (iScale & 1) == 0) {
				exp = scaleUp(exp);
				iScale >>= 1;
			}
			if( iScale != 1 ) {
				throw new UnsupportedOperationException("Cannot scale by non-power-of-2 "+scale);
			}
			return exp;
		}
	}
}
