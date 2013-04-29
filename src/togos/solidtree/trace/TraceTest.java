package togos.solidtree.trace;

import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

import togos.solidtree.DColor;
import togos.solidtree.Material;
import togos.solidtree.SolidNode;

public class TraceTest
{
	static class TraceUI extends Canvas {
		BufferedImage img;
		
		@Override public void paint( Graphics g ) {
			g.drawImage(img, 0, 0, null);
		}
	}
	
	public static void main( String[] args ) {
		int imageWidth = 256;
		int imageHeight = 256;
		
		Tracer t = new Tracer();
		
		SolidNode green = new SolidNode( new Material(new DColor(0,1,0), new DColor(0,1,0), 1) );
		SolidNode blue = new SolidNode( new Material(new DColor(0,0,1), new DColor(0,0,1), 1) );
		SolidNode empty = SolidNode.EMPTY;
		
		SolidNode n = blue;
		for( int i=0; i<5; ++i ) {
			n = new SolidNode( Material.SPACE, 3, 3, 3, new SolidNode[] {
				n    , green, blue,
				green, empty, blue,
				green, green, n   ,
				empty, empty, empty,
				empty, empty, empty,
				empty, empty, empty,
				green, empty, green,
				empty, empty, empty,
				n    , empty, green,
			});
		}
		
		t.setRoot( n, 50);
		
		BufferedImage img = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
		
		//t.trace( 0, 1, 0, 0.61, -0.5, 0 );
		
		for( int y=0; y<imageHeight; ++y ) {
			System.err.println("Line "+y+"...");
			for( int x=0; x<imageWidth; ++x ) {
				t.trace(0, 20, 0, 0.5, -(double)(y+0.5-imageHeight/2)/imageHeight, (double)(x+0.5-imageWidth/2)/imageWidth);
				img.setRGB(x, y, DColor.toArgb(t.red, t.green, t.blue, t.random));
			}
		}
		
		TraceUI tui = new TraceUI();
		tui.img = img;
		final Frame f = new Frame();
		tui.setPreferredSize( new Dimension(img.getWidth(), img.getHeight()) );
		f.add(tui);
		f.pack();
		f.setVisible(true);
		f.addWindowListener(new WindowAdapter() {
			@Override public void windowClosing( WindowEvent evt ) {
				f.dispose();
			}
		});
	}
}
