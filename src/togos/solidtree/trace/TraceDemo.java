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
import togos.lang.BaseSourceLocation;
import togos.solidtree.DColor;
import togos.solidtree.SolidNode;
import togos.solidtree.StandardMaterial;
import togos.solidtree.SurfaceMaterial;
import togos.solidtree.SurfaceMaterialLayer;
import togos.solidtree.forth.Interpreter;
import togos.solidtree.forth.NodeFunctions;
import togos.solidtree.forth.Tokenizer;
import togos.solidtree.shape.NodeShaper;
import togos.solidtree.shape.Sphere;

public class TraceDemo
{
	enum SampleMethod {
		LINE,
		RANDOM
	}
	
	static class Camera {
		public boolean preview = true;
		public int imageWidth, imageHeight;
		// TOOD: May eventually want to use quaternions instead of pitch/yaw/etc
		public double x, y, z;
		public double yaw, pitch, roll;
		public Projection projection;
		public HDRExposure exp;
		
		HDRExposure getExposure() {
			if( exp == null || exp.width != imageWidth && exp.height != imageHeight ) {
				exp = new HDRExposure(imageWidth, imageHeight);
			}
			return exp;
		}

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
		return new SolidNode( StandardMaterial.SPACE, w, h, d, parts );
	}
	
	protected static File getNewOutputFile( String prefix, String suffix ) {
		File f;
		for( int i=0; (f = new File(prefix+i+suffix)).exists(); ++i );
		return f;
	}
	
	public static void main( String[] args ) throws Exception {
		final String sceneName = "GBall1d"; 
		SampleMethod sampleMethod = SampleMethod.RANDOM;
		
		Tracer t = new Tracer();
		final Interrupt<TracerInstruction> tii = new Interrupt<TracerInstruction>();
		
		System.err.println("Building world...");
		
		NodeShaper ns = new NodeShaper();
		ns.divX = 2; ns.divY = 2; ns.divZ = 2;
		ns.setRoot( SolidNode.EMPTY, 2 );
		ns.add( new Sphere(0,0,0,1), new SolidNode(StandardMaterial.opaque(new SurfaceMaterial(new SurfaceMaterialLayer(1, DColor.WHITE, DColor.BLACK, 1, 1, 0, 0)))), 7 );
		
		Interpreter interp = new Interpreter();
		interp.wordDefinitions.put("sphere-center-node", new NodeFunctions.Constant(ns.root));
		NodeFunctions.register(interp.wordDefinitions);
		Tokenizer tokenizer = new Tokenizer("text", 1, 1, 4, interp.delegatingTokenHandler);
		tokenizer.handle( "1 1 1 make-color 2 2 2 make-color 0 0.5 2 make-simple-visual-material make-solid-material-node \"light-node\" def-value\n" );
		tokenizer.handle( "0.6 0.5 0.4 make-color 0 0 0 make-color 0.1 0.9 2 make-simple-visual-material make-solid-material-node \"white-node\" def-value\n" );
		tokenizer.handle( "white-node empty-node empty-node white-node empty-node white-node white-node empty-node 2 2 2 make-composite-node \"checker-node\" def-value\n");
		
		for( int i=0; i<16; ++i ) tokenizer.handle( "empty-node\n");
		tokenizer.handle( "empty-node empty-node empty-node empty-node\n");
		tokenizer.handle( "empty-node white-node sphere-center-node empty-node\n");
		tokenizer.handle( "empty-node sphere-center-node white-node empty-node\n");
		tokenizer.handle( "empty-node empty-node empty-node empty-node\n");
		tokenizer.handle( "empty-node empty-node empty-node empty-node\n");
		tokenizer.handle( "empty-node sphere-center-node white-node empty-node\n");
		tokenizer.handle( "empty-node white-node sphere-center-node empty-node\n");
		tokenizer.handle( "empty-node empty-node empty-node empty-node\n");
		for( int i=0; i<16; ++i ) tokenizer.handle( "empty-node\n");
		tokenizer.handle( "4 4 4 make-composite-node \"checker-center-node\" def-value\n");
		
		tokenizer.handle( "light-node checker-center-node empty-node white-node checker-center-node white-node checker-center-node empty-node 2 2 2 make-composite-node\n");
		tokenizer.flush();
		SolidNode node = interp.stackPop( SolidNode.class, BaseSourceLocation.NONE );
		
		System.err.println("World built!");
		
		node = new SolidNode( StandardMaterial.SPACE, 3, 3, 3, new SolidNode[] {
			SolidNode.EMPTY, SolidNode.EMPTY, SolidNode.EMPTY,
			SolidNode.EMPTY, SolidNode.EMPTY, SolidNode.EMPTY,
			SolidNode.EMPTY, SolidNode.EMPTY, SolidNode.EMPTY,
			
			SolidNode.EMPTY, SolidNode.EMPTY, SolidNode.EMPTY,
			SolidNode.EMPTY, node, SolidNode.EMPTY,
			SolidNode.EMPTY, SolidNode.EMPTY, SolidNode.EMPTY,
			
			SolidNode.EMPTY, SolidNode.EMPTY, SolidNode.EMPTY,
			SolidNode.EMPTY, SolidNode.EMPTY, SolidNode.EMPTY,
			SolidNode.EMPTY, SolidNode.EMPTY, SolidNode.EMPTY,
		});
		
		t.setRoot( node, -128, -128, -128, 128, 128, 128 );
		
		final Camera cam = new Camera();
		cam.imageWidth = 128;
		cam.imageHeight = 128;
		cam.x = 10;
		cam.z = -40;
		cam.yaw = 0;//Math.PI/8;
		final double fovY = (double)(Math.PI*0.3); 
		cam.projection = new FisheyeProjection(fovY*cam.imageWidth/cam.imageHeight, fovY);
		
		final AdjusterUI adj = new AdjusterUI();
		adj.addKeyListener(new KeyAdapter() {
			@Override public void keyPressed( KeyEvent kevt ) {
				double dir = 1;
				
				switch( kevt.getKeyCode() ) {
				case KeyEvent.VK_D:
					String baseName = sceneName+"-"+(int)cam.getExposure().getAverageExposure();
					File saveFile = getNewOutputFile(baseName+"-", ".dump");
					try {
						System.err.println("Saving as "+saveFile);
						ChunkyDump.saveExposure(cam.getExposure(), saveFile);
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
		
		long startTime = System.currentTimeMillis();
		long samplesTaken = 0;
		long prevSamplesTaken = 0;
		long prevTime = startTime;
		double samplesPerSecond = 0;
		int sx = 0, sy = 0;
		HDRExposure exp = null;
		tii.set( TracerInstruction.RESET );
		while( true ) {
			TracerInstruction ti = tii.set( TracerInstruction.CONTINUE );
			if( ti == TracerInstruction.RESET ) {
				startTime = System.currentTimeMillis();
				samplesTaken = 0;
				exp = cam.getExposure();
				exp.clear();
				adj.setExposure(exp);

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
				sampleMethod = cam.preview ? SampleMethod.RANDOM : SampleMethod.LINE;
				
				switch( sampleMethod ) {
				case RANDOM:
					screenX[j] = (double)(t.random.nextDouble()-0.5);
					screenY[j] = (double)(t.random.nextDouble()-0.5);
					break;
				default:
					screenX[j] = (double)(sx+t.random.nextDouble()- cam.imageWidth/2.0)/cam.imageWidth;
					screenY[j] = (double)(sy+t.random.nextDouble()-cam.imageHeight/2.0)/cam.imageHeight;
				}
				
				++sx;
				if( sx > exp.width ) {
					sx = 0;
					++sy;
				}
				if( sy >= exp.height ) sy = 0;
			}
			
			cam.projection.project(vectorSize, screenX, screenY, camPosX, camPosY, camPosZ, camDirX, camDirY, camDirZ);
			
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
				
				int pixelX = (int)((screenX[j]+0.5)*exp.width);
				int pixelY = (int)((screenY[j]+0.5)*exp.height);
				pixelX = pixelX < 0 ? 0 : pixelX >= exp.width  ? exp.width  - 1 : pixelX;
				pixelY = pixelY < 0 ? 0 : pixelY >= exp.height ? exp.height - 1 : pixelY;
				
				// Since z = forward, y = up, x = left, need to invert some things:
				pixelX = exp.width - pixelX - 1;
				pixelY = exp.height - pixelY - 1;
				
				int pixelI = pixelY * exp.width + pixelX;
				exp.e.data[pixelI] += 1;
				exp.r.data[pixelI] += t.red;
				exp.g.data[pixelI] += t.green;
				exp.b.data[pixelI] += t.blue;
				
				if( samplesTaken % 4096 == 0 ) {
					String baseName = sceneName+"-"+(int)exp.getAverageExposure();
					adj.exportFilenamePrefix = baseName;
					adj.exposureUpdated();
				}
				if( samplesTaken % 16384 == 0 ) {
					long time = System.currentTimeMillis();
					samplesPerSecond =
						0.8 * samplesPerSecond +
						0.2 * (samplesTaken - prevSamplesTaken) * 1000 / (time - prevTime);
					System.err.println( samplesPerSecond + " samples/s");
					
					prevSamplesTaken = samplesTaken;
					prevTime = System.currentTimeMillis();
				}
				++samplesTaken;
			}
		}
	}
}
