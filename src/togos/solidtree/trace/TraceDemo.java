package togos.solidtree.trace;

import java.awt.Frame;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.Random;

import togos.hdrutil.AdjusterUI;
import togos.hdrutil.ChunkyDump;
import togos.hdrutil.HDRExposure;
import togos.solidtree.DColor;
import togos.solidtree.SurfaceMaterial;
import togos.solidtree.SurfaceMaterialLayer;
import togos.solidtree.VolumetricMaterial;
import togos.solidtree.SolidNode;
import togos.solidtree.shape.Difference;
import togos.solidtree.shape.NodeShaper;
import togos.solidtree.shape.Sphere;

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
			new SurfaceMaterial(new SurfaceMaterialLayer( 1, filterColor, emissionColor, 0, 1, 1, 0 ))
		);
	}
	
	public static final VolumetricMaterial opaqueVolumetricMaterial( DColor filterColor ) {
		return opaqueVolumetricMaterial( filterColor, DColor.BLACK );
	}
	
	public static final VolumetricMaterial mirrorVolumetricMaterial( double scattering, DColor filterColor ) {
		return VolumetricMaterial.opaque(
			new SurfaceMaterial(
				new SurfaceMaterialLayer( scattering, filterColor, DColor.BLACK, 0, 1, 1, 0 ),
				new SurfaceMaterialLayer( 1, filterColor, DColor.BLACK, 1, 0, 0, 0 )
			)
		);
	}
	
	public static void main( String[] args ) {
		final int imageWidth  = 128;
		final int imageHeight = 128;
		final String sceneName = "test"; 
		SampleMethod sampleMethod = SampleMethod.LINE;
		
		Tracer t = new Tracer();
		final Interrupt<TracerInstruction> tii = new Interrupt<TracerInstruction>();
		
		VolumetricMaterial gray = opaqueVolumetricMaterial( new DColor(0.3, 0.3, 0.3) );
		
		SolidNode wlite = new SolidNode( opaqueVolumetricMaterial(DColor.BLACK, new DColor(2,2,2) ));
		SolidNode rlite = new SolidNode( opaqueVolumetricMaterial(DColor.BLACK, new DColor(1.0,0.1,0.1) ));
		SolidNode mirrr = new SolidNode( mirrorVolumetricMaterial(0.1, new DColor(0.1,0.5,0.1)) );
		SolidNode sgray = new SolidNode( gray );
		
		SolidNode empty = new SolidNode( VolumetricMaterial.SPACE );
		
		NodeShaper s = new NodeShaper();
		s.setRoot(empty, -100, -100, -100, 100, 100, 100 );
		s.add( new Sphere(40, 10, 0, 10), wlite, 6 );
		/*
		s.add( new Sphere(40, 30, 0, 10), rlite, 6 );
		s.add( new Sphere(40, 20, 15, 10), mirrr, 6 );
		s.add( new Sphere(40, 20, -15, 10), sgray, 6 );
		s.add( new Sphere(40, 20, 0, 10), sgray, 6 );
		*/
		s.add( new Difference( new Sphere(0, 0, 0, 100), new Sphere(0, 0, 0, 75) ), sgray, 6 );
		Random rand = new Random();
		for( int i=0; i<50; ++i ) {
			s.add(
				new Sphere(rand.nextDouble() * 80 - 40, rand.nextDouble() * 20 - 60, rand.nextDouble() * 80 - 40, rand.nextDouble()*10+5),
				rand.nextBoolean() ? mirrr : sgray,
				6
			);
		}
		
		t.setRoot( s.root, s.rootMinX, s.rootMinY, s.rootMinZ, s.rootMaxX, s.rootMaxY, s.rootMaxZ );
		
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
