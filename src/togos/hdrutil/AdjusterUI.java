package togos.hdrutil;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;

public class AdjusterUI extends Canvas
{
	HDRImage hdrImage;
	int[] argbBuf;
	BufferedImage bImg;
	
	public synchronized void setImage( HDRImage img ) {
		bImg = new BufferedImage( img.width, img.height, BufferedImage.TYPE_INT_ARGB );
		argbBuf = new int[img.width*img.height];
		this.hdrImage = img;
		repaint();
	}
	
	@Override public void paint( Graphics g ) {
		if( hdrImage == null ) {
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, getWidth(), getHeight());
		} else {
			hdrImage.toArgb(argbBuf);
			bImg.setRGB(0, 0, hdrImage.width, hdrImage.height, argbBuf, 0, hdrImage.width);
			g.drawImage(bImg, 0, 0, null);
		}
	}
	
	public static void main( String[] args ) throws Exception {
		String dumpFilename = args[0];
		File dumpFile = new File(dumpFilename);
		HDRImage img = ChunkyDump.loadChunkyDump(dumpFile);
		
		final Frame f = new Frame("Image adjuster");
		AdjusterUI adj = new AdjusterUI();
		adj.setImage(img);
		adj.setPreferredSize( new Dimension(img.width, img.height));
		f.add(adj);
		f.pack();
		
		f.setVisible(true);
		f.addWindowListener( new WindowAdapter() {
			@Override public void windowClosing( WindowEvent evt ) {
				f.dispose();
			}
		});
	}
}
