package togos.solidtree.trace;

import java.awt.Frame;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;

import togos.hdrutil.AdjusterUI;
import togos.hdrutil.ChunkyDump;
import togos.hdrutil.HDRExposure;
import togos.solidtree.DColor;
import togos.solidtree.SurfaceMaterial;
import togos.solidtree.VolumetricMaterial;
import togos.solidtree.SolidNode;

public class TraceDemo
{
	enum SampleMethod {
		LINE,
		RANDOM
	}
	
	static class Camera {
		boolean preview = true;
		// TOOD: May eventually want to use quaternions instead of pitch/yaw/etc
		double x, y, z;
		double yaw, pitch, roll;
	}
	
	static class Interrupt<V> {
		V value = null;
		public synchronized V set( V v ) {
			V oldValue = value;
			this.value = v;
			return oldValue;
		}
	}
	
	enum TracerInstruction {
		CONTINUE,
		RESET
	}
	
	protected static SolidNode mkNode( int w, int h, int d, SolidNode...parts ) {
		return new SolidNode( VolumetricMaterial.SPACE, w, h, d, parts );
	}
	
	protected static File getNewOutputFile( String prefix, String suffix ) {
		File f;
		for( int i=0; (f = new File(prefix+i+suffix)).exists(); ++i );
		return f;
	}
	
	public static final VolumetricMaterial opaqueVolumetricMaterial( DColor filterColor, DColor emissionColor ) {
		return VolumetricMaterial.opaque(
			new SurfaceMaterial( filterColor, emissionColor, 0, 1, 1, 0 )
		);
	}
	
	public static final VolumetricMaterial opaqueVolumetricMaterial( DColor filterColor ) {
		return opaqueVolumetricMaterial( filterColor, DColor.BLACK );
	}
	
	public static final VolumetricMaterial mirrorVolumetricMaterial( DColor filterColor ) {
		return VolumetricMaterial.opaque(
			new SurfaceMaterial( filterColor, DColor.BLACK, 1, 0, 0, 0 )
		);
	}
	
	public static void main( String[] args ) {
		final int imageWidth  = 512;
		final int imageHeight = 384;
		final String sceneName = "test"; 
		SampleMethod sampleMethod = SampleMethod.LINE;
		
		Tracer t = new Tracer();
		final Interrupt<TracerInstruction> tii = new Interrupt<TracerInstruction>();
		
		VolumetricMaterial gray = opaqueVolumetricMaterial( new DColor(0.3, 0.3, 0.3) );
		
		SolidNode light = new SolidNode( opaqueVolumetricMaterial(DColor.WHITE /*should be black!*/, new DColor(2,2,2) ));
		SolidNode mirrr = new SolidNode( mirrorVolumetricMaterial(new DColor(0.1,0.5,0.1)) );
		SolidNode rlite = new SolidNode( gray );
		SolidNode glite = new SolidNode( gray );
		SolidNode blite = new SolidNode( gray );
		SolidNode sblue = new SolidNode( opaqueVolumetricMaterial(new DColor(0.1,0.1,0.4)) );
		SolidNode green = new SolidNode( opaqueVolumetricMaterial(new DColor(0.1,0.4,0.1)) );
		SolidNode red   = new SolidNode( opaqueVolumetricMaterial(new DColor(0.4,0.1,0.1)) );
		SolidNode empty = new SolidNode( VolumetricMaterial.SPACE );
		SolidNode walll  = mkNode( 4, 4, 4,
			sblue, sblue, sblue, sblue,
			empty, sblue, empty, empty,
			empty, sblue, empty, empty,
			sblue, sblue, sblue, sblue,
			
			sblue, sblue, sblue, sblue,
			empty, sblue, sblue, sblue,
			empty, sblue, sblue, sblue,
			sblue, sblue, sblue, sblue,

			sblue, sblue, sblue, sblue,
			sblue, sblue, sblue, empty,
			sblue, sblue, sblue, empty,
			sblue, sblue, sblue, sblue,

			sblue, sblue, sblue, sblue,
			empty, empty, sblue, empty,
			empty, empty, sblue, empty,
			sblue, sblue, sblue, sblue
		);
		
		SolidNode whiteTile = new SolidNode( mirrorVolumetricMaterial(new DColor(0.5,0.5,0.5)) );
		SolidNode blackTile = new SolidNode( mirrorVolumetricMaterial(new DColor(0.1,0.1,0.1)) );
		SolidNode dirtyWhiteTile = mkNode( 2, 1, 2,
			mkNode( 2, 1, 2, whiteTile, whiteTile, whiteTile, blackTile ),
			mkNode( 2, 1, 2, whiteTile, whiteTile, blackTile, whiteTile ),
			mkNode( 2, 1, 2, whiteTile, whiteTile, whiteTile, whiteTile ),
			mkNode( 2, 1, 2, blackTile, whiteTile, whiteTile, blackTile )
		);
		SolidNode dirtyBlackTile = mkNode( 2, 1, 2,
			mkNode( 2, 1, 2, blackTile, blackTile, blackTile, dirtyWhiteTile ),
			mkNode( 2, 1, 2, dirtyWhiteTile, blackTile, blackTile, blackTile ),
			mkNode( 2, 1, 2, blackTile, blackTile, dirtyWhiteTile, blackTile ),
			mkNode( 2, 1, 2, blackTile, dirtyWhiteTile, blackTile, blackTile )
		);
		dirtyBlackTile = mkNode( 2, 1, 2,
			mkNode( 2, 1, 2, dirtyBlackTile, dirtyBlackTile, dirtyBlackTile, dirtyWhiteTile ),
			mkNode( 2, 1, 2, dirtyWhiteTile, dirtyBlackTile, dirtyBlackTile, dirtyBlackTile ),
			mkNode( 2, 1, 2, dirtyBlackTile, dirtyBlackTile, dirtyWhiteTile, dirtyBlackTile ),
			mkNode( 2, 1, 2, dirtyBlackTile, dirtyWhiteTile, dirtyBlackTile, dirtyBlackTile )
		);
		
		SolidNode floor = mkNode( 2, 1, 2, dirtyBlackTile, dirtyWhiteTile, dirtyWhiteTile, dirtyBlackTile );
		floor = mkNode( 2, 1, 2, floor, floor, floor, floor );
		floor = mkNode( 2, 1, 2, floor, floor, floor, floor );
		floor = mkNode( 2, 1, 2, floor, floor, floor, floor );
		SolidNode clite = mkNode( 4, 4, 4,
			green, green, green, green,
			green, green, green, green,
			green, green, green, green,
			green, green, green, green,
			
			green, empty, empty, green,
			green, light, light, green,
			green, light, light, green,
			green, light, light, green,
			
			green, empty, empty, green,
			green, light, light, green,
			green, light, light, green,
			green, light, light, green,
			
			green, green, green, green,
			green, green, green, green,
			green, green, green, green,
			green, green, green, green
		);
		SolidNode ceiling = mkNode( 2, 1, 2, green, clite, clite, green );
		ceiling = mkNode( 2, 1, 2, ceiling, ceiling, ceiling, ceiling );
		ceiling = mkNode( 2, 1, 2, ceiling, ceiling, ceiling, ceiling );
		ceiling = mkNode( 2, 1, 2, ceiling, ceiling, ceiling, ceiling );
		
		SolidNode maze = mkNode( 16, 1, 16,
			walll, walll, walll, walll, walll, walll, walll, walll, walll, walll, walll, walll, walll, walll, walll, walll,
			walll, empty, empty, empty, empty, empty, walll, walll, walll, walll, walll, walll, walll, walll, walll, walll,
			walll, empty, walll, empty, empty, empty, walll, walll, walll, walll, walll, walll, walll, walll, walll, walll,
			walll, empty, walll, empty, walll, empty, walll, walll, walll, walll, walll, walll, walll, walll, walll, walll,
			walll, empty, walll, empty, empty, empty, walll, walll, walll, walll, walll, walll, walll, walll, walll, walll,
			walll, empty, walll, empty, empty, mirrr, walll, walll, walll, walll, walll, walll, walll, walll, walll, walll,
			walll, empty, walll, empty, empty, empty, empty, red  , walll, walll, walll, walll, walll, walll, walll, walll,
			walll, empty, walll, walll, empty, walll, empty, empty, empty, empty, walll, walll, walll, walll, walll, walll,
			walll, empty, walll, empty, empty, walll, empty, empty, empty, empty, walll, walll, walll, walll, walll, walll,
			walll, empty, walll, walll, empty, walll, empty, empty, walll, mirrr, walll, walll, walll, walll, walll, walll,
			walll, empty, walll, walll, empty, walll, walll, empty, empty, empty, walll, walll, walll, walll, walll, walll,
			walll, empty, walll, empty, empty, walll, walll, empty, empty, empty, walll, walll, walll, walll, walll, walll,
			walll, empty, walll, empty, walll, walll, walll, empty, red  , empty, walll, walll, walll, walll, walll, walll,
			walll, empty, empty, empty, walll, red  , mirrr, empty, empty, empty, walll, walll, walll, walll, walll, walll,
			walll, empty, empty, empty, empty, empty, empty, empty, empty, empty, walll, walll, walll, walll, walll, walll,
			walll, walll, walll, walll, walll, walll, walll, walll, walll, walll, walll, walll, walll, walll, walll, walll
		);
		
		SolidNode m = mkNode( 1, 3, 1, floor, maze, ceiling );
		
		SolidNode grating = new SolidNode( VolumetricMaterial.SPACE, 2, 1, 2, new SolidNode[] {
			empty, green, green, green
		});
		
		//SolidNode empty = new SolidNode( new Material(new DColor(0.95,0.95,0.95), new DColor(0.01,0.01,0.01), 0.0, 0, new DColor(1.0,1.0,1.0)) );
		
		/*
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
			red, floorMirror, red,
			cornerPillarSpace, empty, cornerPillarSpace,
			blue , blue , blue ,
			
			red , floorMirror, red,
			nsPillarSpace, empty, nsPillarSpace,
			blue , empty, blue,
			
			red  , red  , red,
			cornerPillarSpace, ewPillarSpace, cornerPillarSpace,
			blue , blue , blue
		});
		*/
		
		SolidNode sgren = new SolidNode( VolumetricMaterial.SPACE, 1, 3, 1, new SolidNode[] {
			green, empty, empty
		});
		
		SolidNode mgren = new SolidNode( VolumetricMaterial.SPACE, 1, 3, 1, new SolidNode[] {
			mirrr, green, empty
		});
 
		SolidNode field = new SolidNode( VolumetricMaterial.SPACE, 5, 1, 5, new SolidNode[] {
			empty, empty, green, empty, empty,
			mgren, sgren, empty, red  , empty,
			empty, empty, empty, empty, empty,
			empty, sgren, empty, empty, mgren,
			green, empty, empty, sgren, empty,
		});
		
		SolidNode pcol = new SolidNode( VolumetricMaterial.SPACE, 1,3,1, new SolidNode[] {
			rlite, glite, blite
		});
		pcol = new SolidNode( VolumetricMaterial.SPACE, 1,3,1, new SolidNode[] {
			pcol, sblue, red
		});

		
		SolidNode pcell = new SolidNode( VolumetricMaterial.SPACE, 3,1,3, new SolidNode[] {
			empty, empty, empty,
			empty, pcol , empty,
			empty, empty, empty,
		});
		
		SolidNode flite = new SolidNode( VolumetricMaterial.SPACE, 1,3,1, new SolidNode[] {
			green,
			light,
			new SolidNode( VolumetricMaterial.SPACE, 1,3,1, new SolidNode[] {
				empty, empty, new SolidNode( VolumetricMaterial.SPACE, 3,1,3, new SolidNode[] {
					grating,grating,grating,
					grating,grating,grating,
					grating,grating,grating
				})
			})
		});
		
		SolidNode n = new SolidNode( VolumetricMaterial.SPACE, 3, 3, 3, new SolidNode[] {
			green, green, green,
			field, field, field,
			empty, empty, empty,
			green, flite, green,
			field, empty, field,
			empty, empty, empty,
			green, green, green,
			field, field, field,
			empty, empty, empty,
		});
		
		t.setRoot( m, 160, 30, 160 );
		
		final Camera cam = new Camera();
		cam.yaw = 0;//Math.PI/8;
		final double fovY = (double)(Math.PI*0.5); 
		Projection projection = new FisheyeProjection(fovY*imageWidth/imageHeight, fovY);
		
		final HDRExposure exp = new HDRExposure(imageWidth, imageHeight);
		final AdjusterUI adj = new AdjusterUI();
		adj.setExposure(exp);
		adj.addKeyListener(new KeyAdapter() {
			@Override public void keyPressed( KeyEvent kevt ) {
				double dir = 1;
				
				switch( kevt.getKeyCode() ) {
				case KeyEvent.VK_D:
					String baseName = sceneName+"-"+(int)exp.getAverageExposure();
					File saveFile = getNewOutputFile(baseName+"-", ".dump");
					try {
						System.err.println("Saving as "+saveFile);
						ChunkyDump.saveExposure(exp, saveFile);
						System.err.println("Saved!");
					} catch( IOException e ) {
						System.err.println("ERROR SAVING DUMP!");
						e.printStackTrace(System.err);
					}
					break;
				case KeyEvent.VK_P:
					cam.preview ^= true;
					tii.set( TracerInstruction.RESET );
					break;
				case KeyEvent.VK_HOME:
					cam.x += dir * Math.cos(cam.yaw);
					cam.z -= dir * Math.sin(cam.yaw);
					tii.set( TracerInstruction.RESET );
					break;
				case KeyEvent.VK_END:
					cam.x -= dir * Math.cos(cam.yaw);
					cam.z += dir * Math.sin(cam.yaw);
					tii.set( TracerInstruction.RESET );
					break;
				case KeyEvent.VK_DOWN:
					dir = -1;
				case KeyEvent.VK_UP:
					cam.x += dir * Math.sin(cam.yaw);
					cam.z += dir * Math.cos(cam.yaw);
					tii.set( TracerInstruction.RESET );
					break;
				case KeyEvent.VK_PAGE_UP:
					cam.y += dir;
					tii.set( TracerInstruction.RESET );
					break;
				case KeyEvent.VK_PAGE_DOWN:
					cam.y -= dir;
					tii.set( TracerInstruction.RESET );
					break;
				case KeyEvent.VK_LEFT:
					cam.yaw += Math.PI / 16;
					tii.set( TracerInstruction.RESET );
					break;
				case KeyEvent.VK_RIGHT:
					cam.yaw -= Math.PI / 16;
					tii.set( TracerInstruction.RESET );
					break;
				}
			}
		});
		
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
			TracerInstruction ti = tii.set( TracerInstruction.CONTINUE );
			if( ti == TracerInstruction.RESET ) {
				r = 0;
				exp.clear();
				/*
				for( int i=exp.width*exp.height-1; i>=0; --i ) {
					double e = exp.e.data[i];
					if( e != 0 ) {
						exp.r.data[i] /= e;
						exp.g.data[i] /= e;
						exp.b.data[i] /= e;
					}
					exp.e.data[i] = 1;
				}
				*/
			}
			
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
				camPosX[j] += cam.x;
				camPosY[j] += cam.y;
				camPosZ[j] += cam.z;
				
				double sinYaw = Math.sin(cam.yaw);
				double cosYaw = Math.cos(cam.yaw);
				
				double cdx = camDirX[j];
				double cdz = camDirZ[j];
				camDirX[j] = cosYaw*cdx + sinYaw*cdz;  
				camDirZ[j] = cosYaw*cdz - sinYaw*cdx;
				
				if( cam.preview ) {
					t.quickTrace(
						camPosX[j], camPosY[j], camPosZ[j],
						camDirX[j], camDirY[j], camDirZ[j]
					);
				} else {
					t.trace(
						camPosX[j], camPosY[j], camPosZ[j],
						camDirX[j], camDirY[j], camDirZ[j]
					);
				}
				
				int pixelX = (int)Math.round((screenX[j]+0.5)*imageWidth);
				int pixelY = (int)Math.round((screenY[j]+0.5)*imageHeight);
				pixelX = pixelX < 0 ? 0 : pixelX >= imageWidth  ? imageWidth  - 1 : pixelX;
				pixelY = pixelY < 0 ? 0 : pixelY >= imageHeight ? imageHeight - 1 : pixelY;
				
				// Since z = forward, y = up, x = left, need to invert some things:
				pixelX = imageWidth - pixelX - 1;
				pixelY = imageHeight - pixelY - 1;
				
				int pixelI = pixelY * imageWidth + pixelX;
				exp.e.data[pixelI] += 1;
				exp.r.data[pixelI] += t.red;
				exp.g.data[pixelI] += t.green;
				exp.b.data[pixelI] += t.blue;
			}
			
			if( r % 10 == 0 ) {
				adj.exposureUpdated();
			}
			++r;
		}
	}
}
