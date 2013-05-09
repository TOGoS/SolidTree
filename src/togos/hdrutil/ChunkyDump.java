package togos.hdrutil;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class ChunkyDump
{
	public static HDRExposure loadExposure( DataInputStream in ) throws IOException {
		int width  = in.readInt();
		int height = in.readInt();
		int dumpSpp = in.readInt();
		in.readLong(); // 'dump time', which we don't care about.
		HDRExposure exp = new HDRExposure(width, height);
		exp.e.set(dumpSpp);
		for( int x=0; x<width; ++x ) for( int y=0; y<height; ++y ) {
			int destIdx = y*width+x; // Chunky stores samples column-first
			// We store sum of exposures for each component but Chunky stores the average,
			// so convert average->sum by multiplying by exposure. 
			exp.r.data[destIdx] = (float)in.readDouble()*dumpSpp;
			exp.g.data[destIdx] = (float)in.readDouble()*dumpSpp;
			exp.b.data[destIdx] = (float)in.readDouble()*dumpSpp;
		}
		return exp;
	}
	
	public static HDRExposure loadExposure( File dumpFile ) throws IOException {
		DataInputStream in = new DataInputStream(new GZIPInputStream(new FileInputStream(dumpFile)));
		try {
			return loadExposure(in);
		} finally {
			in.close();
		}
	}
	
	public static void saveExposure( HDRExposure exp, DataOutputStream os ) throws IOException {
		final int width = exp.width, height = exp.height;
		os.writeInt(width);
		os.writeInt(height);
		os.writeInt((int)exp.getAverageExposure());
		os.writeLong(0); // 'dump time', which we don't care about.
		for( int x=0; x<width; ++x ) for( int y=0; y<height; ++y ) {
			int srcIdx = y*width+x; // Chunky stores samples column-first
			float e = exp.e.data[srcIdx];
			os.writeDouble( exp.r.data[srcIdx]/e );
			os.writeDouble( exp.g.data[srcIdx]/e );
			os.writeDouble( exp.b.data[srcIdx]/e );
		}
	}
	
	public static void saveExposure( HDRExposure exp, File dumpFile ) throws IOException {
		DataOutputStream out = new DataOutputStream(new GZIPOutputStream(new FileOutputStream(dumpFile)));
		try {
			saveExposure(exp, out);
		} finally {
			out.close();
		}
	}
}
