package togos.hdrutil;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

public class ChunkyDump
{
	public static HDRExposure loadExposure( DataInputStream in ) throws IOException {
		int width  = in.readInt();
		int height = in.readInt();
		int dumpSpp = in.readInt();
		in.readLong(); // 'dump time', which we don't care about.
		HDRExposure img = new HDRExposure(width, height);
		img.e.set(dumpSpp);
		for( int x=0; x<width; ++x ) for( int y=0; y<height; ++y ) {
			int destIdx = y*width+x; // Chunky stores samples column-first
			img.r.data[destIdx] = (float)in.readDouble()*dumpSpp;
			img.g.data[destIdx] = (float)in.readDouble()*dumpSpp;
			img.b.data[destIdx] = (float)in.readDouble()*dumpSpp;
		}
		return img;
	}
	
	public static HDRExposure loadExposure( File dumpFile ) throws IOException {
		DataInputStream in = new DataInputStream(new GZIPInputStream(new FileInputStream(dumpFile)));
		try {
			return loadExposure(in);
		} finally {
			in.close();
		}
	}
}
