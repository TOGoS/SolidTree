package togos.hdrutil;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

public class ChunkyDump
{
	public static HDRExposure loadChunkyDump( DataInputStream in ) throws IOException {
		int width  = in.readInt();
		int height = in.readInt();
		int dumpSpp = in.readInt();
		in.readLong(); // 'dump time', which we don't care about.
		HDRExposure img = new HDRExposure(4, width, height);
		img.e.set(dumpSpp);
		System.err.println("Width: "+width+", height:"+height);
		// Chunky stores columns, but we store rows
		for( int x=0; x<width; ++x ) for( int y=0; y<height; ++y ) {
			int destIdx = y*width+x;
			img.r.data[destIdx] = in.readDouble()*dumpSpp;
			img.g.data[destIdx] = in.readDouble()*dumpSpp;
			img.b.data[destIdx] = in.readDouble()*dumpSpp;
		}
		return img;
	}
	
	public static HDRExposure loadChunkyDump( File dumpFile ) throws IOException {
		DataInputStream in = new DataInputStream(new GZIPInputStream(new FileInputStream(dumpFile)));
		try {
			return loadChunkyDump(in);
		} finally {
			in.close();
		}
	}
}
