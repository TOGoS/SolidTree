package togos.solidtree.trace;

import java.awt.Canvas;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

import togos.hdrutil.AdjusterUI;
import togos.hdrutil.HDRExposure;
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
		int imageWidth = 384;
		int imageHeight = 256;
		
		Tracer t = new Tracer();
		
		SolidNode mirror = new SolidNode( new Material(DColor.BLACK, DColor.BLACK, 0.5, 1) );
		SolidNode light = new SolidNode( new Material(new DColor(1.0,1.0,1.0), new DColor(2,2,1.5), 0, 1.0) );
		SolidNode blue  = new SolidNode( new Material(new DColor(0.1,0.1,0.5), DColor.BLACK, 0.0, 1.0) );
		SolidNode green = new SolidNode( new Material(new DColor(0.1,0.5,0.1), DColor.BLACK, 0.0, 1.0) );
		SolidNode red   = new SolidNode( new Material(new DColor(0.5,0.1,0.1), DColor.BLACK, 0.0, 1.0) );
		SolidNode empty = SolidNode.EMPTY;
		
		SolidNode pillar = new SolidNode( Material.SPACE, 3, 1, 3, new SolidNode[] {
			empty, empty, empty,
			empty, green, empty,
			empty, empty, empty
		});

		SolidNode floorMirror = new SolidNode( Material.SPACE, 1, 3, 1, new SolidNode[] {
			empty, mirror, empty
		});
		
		SolidNode smallLight = new SolidNode( Material.SPACE, 3, 3, 3, new SolidNode[] {
			empty, empty, empty,
			empty, empty, empty,
			empty, empty, empty,
			
			empty, empty, empty,
			red  , light, red  ,
			empty, empty, empty,

			empty, empty, empty,
			empty, empty, empty,
			empty, empty, empty,
		});
		
		SolidNode n = new SolidNode( Material.SPACE, 3, 3, 3, new SolidNode[] {
			blue , blue , blue ,
			pillar, empty, pillar,
			red, red, red,
			
			blue , smallLight, blue,
			pillar, empty, pillar,
			red , floorMirror, red,
			
			blue , blue , blue ,
			pillar, pillar, pillar,
			red  , red, red
		});
		
		t.setRoot( n, 50);
		
		BufferedImage img = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
		
		//t.trace( 0, 1, 0, 0.61, -0.5, 0 );
		
		double fovY = Math.PI/2; 
		Projection projection = new FisheyeProjection(fovY*imageWidth/imageHeight, fovY);
		
		HDRExposure exp = new HDRExposure(imageWidth, imageHeight);
		AdjusterUI adj = new AdjusterUI();
		adj.setExposure(exp);
		
		final Frame f = new Frame();
		f.add(adj);
		f.pack();
		f.setVisible(true);
		f.addWindowListener(new WindowAdapter() {
			@Override public void windowClosing( WindowEvent evt ) {
				f.dispose();
				System.exit(0);
			}
		});
		
		int vectorSize = 1024;
		
		double[] screenX = new double[1024], screenY = new double[1024];
		double[] camPosX = new double[1024], camPosY = new double[1024], camPosZ = new double[1024];
		double[] camDirX = new double[1024], camDirY = new double[1024], camDirZ = new double[1024];
		
		int r = 0;
		int sx = 0, sy = 0;
		while( true ) {
			for( int j=0; j<vectorSize; ++j ) {
				screenX[j] = (sx+Math.random()- imageWidth/2.0)/imageWidth;
				screenY[j] = (sy+Math.random()-imageHeight/2.0)/imageHeight;
				//screenX[j] = (Math.random()-0.5);
				//screenY[j] = (Math.random()-0.5);
				
				++sx;
				if( sx > imageWidth ) {
					sx = 0;
					++sy;
				}
				if( sy >= imageHeight ) sy = 0;
			}
			
			projection.project(vectorSize, screenX, screenY, camPosX, camPosY, camPosZ, camDirX, camDirY, camDirZ);
			
			for( int j=0; j<vectorSize; ++j ) {
				t.trace(
					camPosX[j], camPosY[j], camPosZ[j] - 20,
					camDirX[j], camDirY[j], camDirZ[j]
				);
				
				int pixelX = (int)Math.round((screenX[j]+0.5)*imageWidth);
				int pixelY = (int)Math.round((screenY[j]+0.5)*imageHeight);
				pixelX = pixelX < 0 ? 0 : pixelX >= imageWidth  ? imageWidth  - 1 : pixelX;
				pixelY = pixelY < 0 ? 0 : pixelY >= imageHeight ? imageHeight - 1 : pixelY;
				int pixelI = pixelY * imageWidth + pixelX;
				exp.e.data[pixelI] += 1;
				exp.r.data[pixelI] += t.red;
				exp.g.data[pixelI] += t.green;
				exp.b.data[pixelI] += t.blue;
			}
			
			if( r % 40 == 0 ) {
				adj.exposureUpdated();
			}
			++r;
		}
	}
}
