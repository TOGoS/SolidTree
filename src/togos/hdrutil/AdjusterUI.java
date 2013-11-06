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
import java.io.IOException;

import javax.imageio.ImageIO;

/*
 * TODO: Resettable background thread for recalculating image
 * TODO: Save, eXport, Merge from the UI
 */
public class AdjusterUI extends Canvas
{
	private static final long serialVersionUID = 1L;
	public static final String[] EMPTY_STRING_ARRAY = new String[0];
	
	protected int overlayTextMode = 1; 
	public String exportFilenamePrefix;
	public String[] extraStatusLines = EMPTY_STRING_ARRAY;
	HDRExposure hdrExposure;
	HDRImage hdrImage;
	int[] argbBuf;
	BufferedImage bImg;
	float exposure = 1;
	float gamma = 2.2f;
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
				case KeyEvent.VK_X:
					// Export
					try {
						exportImage();
					} catch( Exception e ) {
						System.err.println("Failed to export");
						e.printStackTrace(System.err);
					}
					break;
				case KeyEvent.VK_F1:
					++overlayTextMode;
					if( overlayTextMode > 2 ) overlayTextMode = 0;
					repaint();
					break;
				}
			}
		});
	}
	
	protected synchronized void recalculate() {
		hdrImage.load(hdrExposure);
		hdrImage.multiply(exposure);
		hdrImage.exponentiate( 1/gamma );
		
		hdrImage.toArgb(argbBuf, dither);
		bImg.setRGB(0, 0, hdrImage.getWidth(), hdrImage.getHeight(), argbBuf, 0, hdrImage.getWidth());
		
		repaint();
	}
	
	protected File getNewOutputFile( String prefix, String suffix ) {
		File f;
		for( int i=0; (f = new File(prefix+i+suffix)).exists(); ++i );
		return f;
	}
	
	protected File getNewImageExportFile() {
		return getNewOutputFile(exportFilenamePrefix+"-E"+exposure+"-G"+gamma+"-", ".png");
	}
	
	protected void exportImage() throws IOException {
		BufferedImage img = bImg;
		if( img == null ) {
			throw new RuntimeException("No current image");
		}
		File out = getNewImageExportFile();
		System.err.println("Exporting to "+out.getPath()+"...");
		FileUtil.mkParentDirs(out);
		ImageIO.write(img, "png", out);
		System.err.println("Wrote "+out.getPath());
	}
	
	public synchronized void setExposure( HDRExposure exp, boolean recalculate ) {
		setPreferredSize( new Dimension(exp.getWidth(), exp.getHeight()));
		this.bImg = new BufferedImage( exp.getWidth(), exp.getHeight(), BufferedImage.TYPE_INT_ARGB );
		this.argbBuf = new int[exp.getWidth()*exp.getHeight()];
		this.hdrImage = exp.getImage();
		this.hdrExposure = exp;
		
		if( recalculate ) {
			System.err.println("Calculating default exposure...");
			hdrImage.load(exp);
			if( hdrImage.max() > 0 ) {
				this.exposure = 100 / hdrImage.max();
			}
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
			while( hdrImage.getWidth()*scale <= getWidth() && hdrImage.getHeight()*scale <= getHeight() ) {
				++scale;
			}
			--scale;
			
			int left  = (getWidth()  - hdrImage.getWidth() *scale) / 2;
			int top   = (getHeight() - hdrImage.getHeight()*scale) / 2;
			int right = hdrImage.getWidth() *scale + left;
			int bottom= hdrImage.getHeight()*scale + top  ;
			
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, left, getHeight());
			g.fillRect(right, 0, getWidth()-right, getHeight());
			g.fillRect(left, 0, right - left, top);
			g.fillRect(left, bottom, right - left, getHeight() - bottom);
			
			g.drawImage(bImg, left, top, left+hdrImage.getWidth()*scale, top+hdrImage.getHeight()*scale, 0, 0, hdrImage.getWidth(), hdrImage.getHeight(), null);
		}

		int line = 1;
		
		if( overlayTextMode >= 1 ) {
			g.setColor(Color.WHITE);
			g.drawString(String.format("Exposure: %12.4f", exposure), 4, 16*line++ );
			g.drawString(String.format("Gamma:    %12.4f", gamma   ), 4, 16*line++ );
			g.drawString("Dithering: " +(dither ? "enabled" : "disabled"), 4, 16*line++ );
			
			if( extraStatusLines.length > 0 ) {
				++line;
				for( String text : extraStatusLines ) {
					g.drawString(text, 4, 16*line++ );
				}
			}
		}
		
		if( overlayTextMode == 1 ) {
			g.drawString("Hit F1 for help", 4, 16*line++ );
		}
		
		if( overlayTextMode >= 2 ) {
			++line;
			g.drawString("Keys:", 4, 16*line++);
			g.drawString("  F1 - toggle overlay text", 4, 16*line++);
			g.drawString("  X  - export image", 4, 16*line++);
			g.drawString("  E  - change exposure", 4, 16*line++);
			g.drawString("  G  - change gamma", 4, 16*line++);
			g.drawString("Hold shift to decrease exposure/gamma", 4, 16*line++);
			g.drawString("Hold control to change values more slowly", 4, 16*line++);
		}
	}
	
	@Override public void update( Graphics g ) {
		paint(g);
	}
	
	protected static HDRExposure addScaleyAndDestructively( HDRExposure a, HDRExposure b )
		throws IncompatibleImageException
	{
		if( a.width == b.width && a.height == b.height ) {
			a.add(b);
			return a;
		}
		
		if( !ExposureScaler.aspectRatioPreserved(a.width, a.height, b.width, b.height) ) {
			throw new IncompatibleImageException("Image aspect ratios mismatch; "+a.width+"x"+a.height+" vs "+b.width+"x"+b.height);
		}
		
		if( b.width > a.width ) {
			// Switch so that a is the larger one
			HDRExposure temp = b;
			b = a; a = temp;
		}
		
		a.add( ExposureScaler.scaleTo(b, a.width, a.height) );
		return a;
	}
	
	public static void main( String[] args ) throws Exception {
		String sceneName = null;
		HDRExposure sum = null;
		int chunkySpp = 0;
		for( String arg : args ) {
			if( arg.endsWith(".dump") ) {
				String dumpFilename = arg;
				File dumpFile = new File(dumpFilename);
				sceneName = dumpFile.getName();
				sceneName = sceneName.substring(0, sceneName.length()-5);
				System.err.println("Loading "+dumpFile+"...");
				HDRExposure exp = ChunkyDump.loadExposure(dumpFile);
				System.err.println("  -> "+exp.width+"x"+exp.height);
				if( exp.e.length > 0 ) {
					// Don't bother averaging; chunky dumps have the same spp everywhere
					chunkySpp += (int)exp.e[0];
				}
				if( sum == null ) {
					sum = exp;
				} else {
					sum = addScaleyAndDestructively(sum,exp);
				}
			}
		}
		
		if( sum == null ) {
			System.err.println("No dumps specified!");
			System.exit(1);
			return;
		}
		
		System.err.println("Combined image is "+sum.width+"x"+sum.height);
		
		final Frame f = new Frame("Image adjuster");
		AdjusterUI adj = new AdjusterUI();
		adj.exportFilenamePrefix = sceneName + (chunkySpp == 0 ? "" : "-"+chunkySpp);
		adj.setExposure(sum, true);
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
