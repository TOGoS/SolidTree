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
	private static final long serialVersionUID = 1L;
	
	HDRImage hdrImage;
	int[] argbBuf;
	BufferedImage bImg;
	
	public AdjusterUI() {
		super();
		setBackground(Color.BLACK);
	}
	
	public synchronized void setImage( HDRImage img ) {
		setPreferredSize( new Dimension(img.width, img.height));
		this.bImg = new BufferedImage( img.width, img.height, BufferedImage.TYPE_INT_ARGB );
		this.argbBuf = new int[img.width*img.height];
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
			
			int scale = 2;
			while( hdrImage.width*scale <= getWidth() && hdrImage.height*scale <= getHeight() ) {
				++scale;
			}
			--scale;
			
			int x = (getWidth() - hdrImage.width*scale) / 2;
			int y = (getHeight() - hdrImage.height*scale) / 2;
			
			g.drawImage(bImg, x, y, x+hdrImage.width*scale, y+hdrImage.height*scale, 0, 0, hdrImage.width, hdrImage.height, null);
		}
	}
	
	public static void main( String[] args ) throws Exception {
		String dumpFilename = args[0];
		File dumpFile = new File(dumpFilename);
		System.err.println("Loading dump...");
		HDRExposure exp = ChunkyDump.loadChunkyDump(dumpFile);
		System.err.println("Converting to image...");
		HDRImage img = new HDRImage( exp.width, exp.height );
		img.load(exp);
		System.err.println("Adjusting image...");
		img.multiply(100.0/img.maxRgb());
		img.exponentiate( 1/3.0 );
		
		final Frame f = new Frame("Image adjuster");
		AdjusterUI adj = new AdjusterUI();
		adj.setImage(img);
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
