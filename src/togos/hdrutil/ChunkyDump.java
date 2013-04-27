package togos.hdrutil;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

public class ChunkyDump
{
	public static HDRImage loadChunkyDump( DataInputStream in ) throws IOException {
		int width = in.readInt();
		int height= in.readInt();
		int dumpSpp = in.readInt();
		in.readLong(); // 'dump time', which we don't care about.
		HDRImage img = new HDRImage(4, width, height);
		img.channels[0].set(dumpSpp);
		final int size = width*height;
		for( int i=0; i<size; ++i ) {
			img.channels[1].data[i] = in.readDouble();
			img.channels[2].data[i] = in.readDouble();
			img.channels[3].data[i] = in.readDouble();
		}
		return img;
	}
	
	public static HDRImage loadChunkyDump( File dumpFile ) throws IOException {
		DataInputStream in = new DataInputStream(new GZIPInputStream(new FileInputStream(dumpFile)));
		try {
			return loadChunkyDump(in);
		} finally {
			in.close();
		}
	}
}
