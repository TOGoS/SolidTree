package togos.hdrutil;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;

public class AdjusterUI extends Canvas
{
	private static final long serialVersionUID = 1L;
	
	HDRExposure hdrExposure;
	HDRImage hdrImage;
	int[] argbBuf;
	BufferedImage bImg;
	double exposure = 1;
	double gamma = 2.2;
	boolean dither = true;
	
	public AdjusterUI() {
		super();
		setBackground(Color.BLACK);
		addKeyListener(new KeyAdapter() {
			protected double neg( double v, boolean negate ) {
				return negate ? 1.0/v : v;
			}
			
			@Override public void keyPressed(KeyEvent evt) {
				boolean negate = evt.isShiftDown();
				boolean controlled = evt.isControlDown();
				switch( evt.getKeyCode() ) {
				case KeyEvent.VK_G:
					gamma *= neg(controlled ? 1.125 : 1.5, negate);
					recalculate();
					break;
				case KeyEvent.VK_E:
					exposure *= neg(controlled ? 1.125 : 1.5, negate);
					recalculate();
					break;
				case KeyEvent.VK_D:
					dither ^= true;
					recalculate();
					break;
				}
			}
		});
	}
	
	protected synchronized void recalculate() {
		System.err.println("[Re]oading exposure");
		hdrImage.load(hdrExposure);
		System.err.println("Multiplying");
		hdrImage.multiply(exposure);
		System.err.println("Exponentiating");
		hdrImage.exponentiate( 1/gamma );
		
		System.err.println("Calculating output image");
		hdrImage.toArgb(argbBuf, dither);
		bImg.setRGB(0, 0, hdrImage.width, hdrImage.height, argbBuf, 0, hdrImage.width);
		
		repaint();
	}
	
	public synchronized void setExposure( HDRExposure exp ) {
		setPreferredSize( new Dimension(exp.width, exp.height));
		this.bImg = new BufferedImage( exp.width, exp.height, BufferedImage.TYPE_INT_ARGB );
		this.argbBuf = new int[exp.width*exp.height];
		this.hdrImage = new HDRImage( exp.width, exp.height );
		this.hdrExposure = exp;
		
		System.err.println("Calculating default exposure...");
		hdrImage.load(exp);
		if( hdrImage.max() > 0 ) {
			this.exposure = 100 / hdrImage.max();
		}
		
		recalculate();
	}
	
	public void exposureUpdated() {
		recalculate();
	}
	
	@Override public void paint( Graphics g ) {
		if( hdrImage == null ) {
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, getWidth(), getHeight());
		} else {
			int scale = 2;
			while( hdrImage.width*scale <= getWidth() && hdrImage.height*scale <= getHeight() ) {
				++scale;
			}
			--scale;
			
			int right = (getWidth()  - hdrImage.width *scale) / 2;
			int top   = (getHeight() - hdrImage.height*scale) / 2;
			int left  = hdrImage.width *scale + right;
			int bottom= hdrImage.height*scale - top  ;
			
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, right, getHeight());
			g.fillRect(left, 0, getWidth()-left, getHeight());
			g.fillRect(left, 0, right-left, top);
			g.fillRect(left, bottom, right-left, getHeight()-bottom);
			
			g.drawImage(bImg, right, top, right+hdrImage.width*scale, top+hdrImage.height*scale, 0, 0, hdrImage.width, hdrImage.height, null);
			
			g.setColor(Color.WHITE);
			g.drawString(String.format("Exposure: %12.4f", exposure), 4, 16 );
			g.drawString(String.format("Gamma:    %12.4f", gamma   ), 4, 32 );
			g.drawString("Dithering: " +(dither ? "enabled" : "disabled"), 4, 48);
		}
	}
	
	@Override public void update( Graphics g ) {
		paint(g);
	}
	
	public static void main( String[] args ) throws Exception {
		HDRExposure sum = null;
		for( String arg : args ) {
			String dumpFilename = arg;
			File dumpFile = new File(dumpFilename);
			System.err.println("Loading dump...");
			HDRExposure exp = ChunkyDump.loadChunkyDump(dumpFile);
			if( sum == null ) {
				sum = exp;
			} else {
				sum.add(exp);
			}
		}
		
		if( sum == null ) {
			System.err.println("No dumps specified!");
			System.exit(1);
		}
		
		final Frame f = new Frame("Image adjuster");
		AdjusterUI adj = new AdjusterUI();
		adj.setExposure(sum);
		f.add(adj);
		f.pack();
		
		f.setVisible(true);
		adj.requestFocus();
		f.addWindowListener( new WindowAdapter() {
			@Override public void windowClosing( WindowEvent evt ) {
				f.dispose();
			}
		});
	}
}
