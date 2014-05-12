package togos.hdrutil.effects;

import togos.hdrutil.HDRImage;

public class Bleed
{
	private Bleed() { }
	
	public static void bleed( float[] in, float dry, float falloff, float[] out, int offset, int stride, int count ) {
		final float wet = 1-dry;
		float acc = 0;
		for( int i=count-1; i>=0; --i ) {
			out[offset+i*stride] = in[offset+i*stride] * dry;
		}
		for( int i=0; i<count; ++i ) {
			float v = in[offset+i*stride]/2;
			acc += v*wet;
			float take = acc*falloff;
			acc -= take;
			out[offset+i*stride] += take;
		}
		acc = 0;
		for( int i=count-1; i>=0; --i ) {
			float v = in[offset+i*stride]/2;
			acc += v*wet;
			float take = acc*falloff;
			acc -= take;
			out[offset+i*stride] += take;
		}
	}
	
	public static HDRImage bleedXY( HDRImage in, float hdry, float hfalloff, float vdry, float vfalloff, HDRImage out ) {
		float[] scratch = new float[in.width*in.height];
		for( int ci=0; ci<3; ++ci ) {
			for( int y=0; y<in.height; ++y ) {
				bleed(in.colorChannels[ci], hdry, hfalloff, scratch, y*in.width, 1, in.width);
			}
			for( int x=0; x<in.width; ++x ) {
				bleed(scratch, vdry, vfalloff, out.colorChannels[ci], x*1, in.width, in.height);
			}
		}
		return out;
	}
}
