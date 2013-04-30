package togos.solidtree.trace;

import java.awt.Frame;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import togos.hdrutil.AdjusterUI;
import togos.hdrutil.HDRExposure;
import togos.solidtree.DColor;
import togos.solidtree.Material;
import togos.solidtree.SolidNode;

public class TraceDemo
{
	enum SampleMethod {
		LINE,
		RANDOM
	}
	
	public static void main( String[] args ) {
		int imageWidth  = 512;
		int imageHeight = 512;
		SampleMethod sampleMethod = SampleMethod.RANDOM;
		
		Tracer t = new Tracer();
		
		SolidNode mirror = new SolidNode( new Material(DColor.BLACK, DColor.BLACK, 0.5, 1) );
		SolidNode light = new SolidNode( new Material(new DColor(1.0,1.0,1.0), new DColor(2,2,1.5), 0, 1.0) );
		SolidNode blue  = new SolidNode( new Material(new DColor(0.1,0.1,0.5), DColor.BLACK, 0.0, 1.0) );
		SolidNode green = new SolidNode( new Material(new DColor(0.1,0.5,0.1), DColor.BLACK, 0.0, 1.0) );
		SolidNode red   = new SolidNode( new Material(new DColor(0.5,0.1,0.1), DColor.BLACK, 0.0, 1.0) );
		SolidNode empty = SolidNode.EMPTY;
		
		SolidNode fencing = new SolidNode( Material.SPACE, 1, 7, 1, new SolidNode[] {
			empty, green, empty, green, empty, green, empty
		});
		
		SolidNode nsFence = new SolidNode( Material.SPACE, 3, 1, 1, new SolidNode[] {
			empty, fencing, empty
		});
		SolidNode ewFence = new SolidNode( Material.SPACE, 1, 1, 3, new SolidNode[] {
			empty, fencing, empty
		});
		
		SolidNode ewPillar = new SolidNode( Material.SPACE, 3, 3, 3, new SolidNode[] {
			green, green, green,
			green, empty, green,
			green, green, green,
			
			green, green, green,
			green, light, green,
			green, green, green,
			
			green, green, green,
			green, empty, green,
			green, green, green,
		});
		
		SolidNode nsPillar = new SolidNode( Material.SPACE, 3, 3, 3, new SolidNode[] {
			green, green, green,
			green, green, green,
			green, green, green,
			
			green, green, green,
			empty, light, empty,
			green, green, green,
			
			green, green, green,
			green, green, green,
			green, green, green,
		});
		
		SolidNode nsPillarSpace = new SolidNode( Material.SPACE, 3, 1, 3, new SolidNode[] {
			empty, nsFence, empty,
			empty, nsPillar, empty,
			empty, nsFence, empty
		});

		SolidNode ewPillarSpace = new SolidNode( Material.SPACE, 3, 1, 3, new SolidNode[] {
			empty, empty, empty,
			ewFence, ewPillar, ewFence,
			empty, empty, empty
		});
		
		SolidNode cornerPillarSpace = new SolidNode( Material.SPACE, 3, 1, 3, new SolidNode[] {
			empty, nsFence, empty,
			ewFence, green, ewFence,
			empty, nsFence, empty
		});

		SolidNode cornerLightFixture = new SolidNode( Material.SPACE, 1, 3, 1, new SolidNode[] {
			red, light, empty	
		});
		
		SolidNode overpoolLights = new SolidNode( Material.SPACE, 8, 1, 8, new SolidNode[] {
			cornerLightFixture, empty, empty, empty, empty, empty, empty, empty,
			 empty, empty, empty, empty, empty, empty, empty, empty,
			 empty, empty, empty, empty, empty, empty, empty, empty,
			 empty, empty, empty, empty, empty, empty, empty, cornerLightFixture,
			 cornerLightFixture, empty, empty, empty, empty, empty, empty, empty,
			 empty, empty, empty, empty, empty, empty, empty, empty,
			 empty, empty, empty, empty, empty, empty, empty, empty,
			 empty, empty, empty, empty, empty, empty, empty, cornerLightFixture
		});
		
		SolidNode floorMirror = new SolidNode( Material.SPACE, 1, 4, 1, new SolidNode[] {
			empty, mirror, empty, empty
		});
		
		SolidNode ceilingLightFixture = new SolidNode( Material.SPACE, 3, 3, 3, new SolidNode[] {
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
			cornerPillarSpace, empty, cornerPillarSpace,
			red, floorMirror, red,
			
			blue , ceilingLightFixture, blue,
			nsPillarSpace, empty, nsPillarSpace,
			red , floorMirror, red,
			
			blue , blue , blue ,
			cornerPillarSpace, ewPillarSpace, cornerPillarSpace,
			red  , red, red
		});
		
		t.setRoot( n, 50);
			
		double fovY = (double)(Math.PI*0.75); 
		Projection projection = new FisheyeProjection(fovY*imageWidth/imageHeight, fovY);
		
		HDRExposure exp = new HDRExposure(imageWidth, imageHeight);
		AdjusterUI adj = new AdjusterUI();
		adj.setExposure(exp);
		
		final Frame f = new Frame();
		f.add(adj);
		f.pack();
		f.addWindowListener(new WindowAdapter() {
			@Override public void windowClosing( WindowEvent evt ) {
				f.dispose();
				System.exit(0);
			}
		});
		f.setVisible(true);
		adj.requestFocus();
		
		int vectorSize = 1024;
		
		double[] screenX = new double[1024], screenY = new double[1024];
		double[] camPosX = new double[1024], camPosY = new double[1024], camPosZ = new double[1024];
		double[] camDirX = new double[1024], camDirY = new double[1024], camDirZ = new double[1024];
		
		int r = 0;
		int sx = 0, sy = 0;
		while( true ) {
			for( int j=0; j<vectorSize; ++j ) {
				switch( sampleMethod ) {
				case RANDOM:
					screenX[j] = (double)(t.random.nextDouble()-0.5);
					screenY[j] = (double)(t.random.nextDouble()-0.5);
					break;
				default:
					screenX[j] = (double)(sx+t.random.nextDouble()- imageWidth/2.0)/imageWidth;
					screenY[j] = (double)(sy+t.random.nextDouble()-imageHeight/2.0)/imageHeight;
				}
				
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
					camPosX[j], camPosY[j]+5, camPosZ[j] - 10,
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
