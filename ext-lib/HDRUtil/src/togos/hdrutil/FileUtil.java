package togos.hdrutil;

import java.io.File;

public class FileUtil
{
	public static void mkParentDirs( File f ) {
		File parent = f.getParentFile();
		if( parent != null && !parent.exists() ) {
			parent.mkdirs();
		}
	}
}
