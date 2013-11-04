package togos.solidtree.trace;

import java.awt.Dimension;
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
import togos.lang.BaseSourceLocation;
import togos.lang.ScriptError;
import togos.lang.SourceLocation;
import togos.solidtree.NodeLoader;
import togos.solidtree.NodeLoader.HashMapLoadContext;
import togos.solidtree.NodeLoader.LoadContext;
import togos.solidtree.NodeRoot;
import togos.solidtree.SolidNode;
import togos.solidtree.StandardMaterial;
import togos.solidtree.forth.Interpreter;
import togos.solidtree.forth.REPL;
import togos.solidtree.forth.StandardWordDefinition;
import togos.solidtree.forth.procedure.SafeProcedures;
import togos.solidtree.trace.sky.AdditiveSkySphere;
import togos.solidtree.trace.sky.RadialSkySphere;

public class TraceDemo
{
	enum SampleMethod {
		LINE,
		RANDOM
	}
	
	static class Camera {
		public boolean preview = true;
		public int imageWidth, imageHeight;
		public double x, y, z;
		// TOOD: May eventually want to use quaternions instead of pitch/yaw/etc
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
		return SolidNode.build( StandardMaterial.SPACE, w, h, d, parts );
	}
	
	protected static File getNewOutputFile( String prefix, String suffix ) {
		File f;
		for( int i=0; (f = new File(prefix+i+suffix)).exists(); ++i );
		return f;
	}
	
	protected static String cameraPositionScript( Camera c ) {
		return
			c.x + " " + c.y + " " + c.z + " set-camera-position " +
			c.yaw + " set-camera-yaw " +
			c.pitch + " set-camera-pitch " +
			c.roll + " set-camera-roll ";
	}
	
	protected static void dumpCameraPosition( Camera c ) {
		System.err.println(cameraPositionScript(c));
	}
	
	public static void main( String[] args ) throws Exception {
		final String renderDir = "renders";
		final String sceneName = "testrender"+System.currentTimeMillis(); 
		SampleMethod sampleMethod = SampleMethod.RANDOM;
		
		Tracer t = new Tracer();
		final Interrupt<TracerInstruction> tii = new Interrupt<TracerInstruction>();
		
		System.err.println("Building world...");
		
		LoadContext<SolidNode> loadCtx = new HashMapLoadContext<SolidNode>();
		
		NodeLoader nl = new NodeLoader();
		nl.includePath.add(new File("world"));
		Object _root = nl.get("world", loadCtx);
		
		NodeRoot root;
		if( _root instanceof NodeRoot ) {
			root = (NodeRoot)_root;
		} else if( _root instanceof SolidNode ) {
			root = new NodeRoot( (SolidNode)_root, 1024, 1024, 1024 );
		} else {
			throw new ScriptError("Script returned neither a SolidNode nor a NodeRoot, but a "+_root.getClass().getName(), BaseSourceLocation.NONE);
		}
		
		double sunintensity = 100;
		
		t.setRoot( root );
		t.skySphere = new AdditiveSkySphere(
			new RadialSkySphere(0, Math.sin(Math.PI/8), Math.cos(Math.PI/8), 16, 1*sunintensity, 0.9*sunintensity, 0.8*sunintensity)
			// new RadialSkySphere(0, Math.sin(Math.PI/4)*0.8, Math.cos(Math.PI/4)*0.8, 1, 0.2, 0.2, 0.5),
			// new RadialSkySphere(0, Math.sin(Math.PI/4)*0.8, Math.cos(Math.PI/4)*0.8, 5, 2.0, 2.0, 2.0),
			// new CrappySkySphere()
		);
		
		final Camera cam = new Camera();
		cam.imageWidth = 384;
		cam.imageHeight = 192;
		cam.x = 0;
		cam.y = 0;
		cam.z = 0;
		cam.yaw = 0;//Math.PI/8;
		final double fovY = (double)(Math.PI*0.3); 
		cam.projection = new FisheyeProjection(fovY*cam.imageWidth/cam.imageHeight, fovY);
		// cam.projection = new ApertureProjection( cam.projection, 0.05, 4 );
		
		final AdjusterUI adj = new AdjusterUI();
		
		// Figure a nice default window size:
		int scaledWidth = cam.imageWidth;
		int scaledHeight = cam.imageHeight;
		if( scaledHeight <= 192 && scaledHeight <= 384 ) {
			scaledWidth *= 2;
			scaledHeight *= 2;
		}
		adj.setPreferredSize( new Dimension(scaledWidth + 32, scaledHeight + 32) );
		
		Thread commandReader = new Thread("interactive command reader") {
			@Override public void run() {
				REPL repl = new REPL();
				SafeProcedures.register(repl.interp.wordDefinitions);
				repl.registerReplWords();
				// TODO: extract to a 'scene' object with registerSceneWords(...)
				repl.interp.wordDefinitions.put("print-camera-position", new StandardWordDefinition() {
					@Override public void run(Interpreter interp, SourceLocation sLoc) throws ScriptError {
						dumpCameraPosition(cam);
					}
				});
				repl.interp.wordDefinitions.put("set-camera-position", new StandardWordDefinition() {
					@Override public void run(Interpreter interp, SourceLocation sLoc) throws ScriptError {
						Number z = interp.stackPop(Number.class, sLoc);
						Number y = interp.stackPop(Number.class, sLoc);
						Number x = interp.stackPop(Number.class, sLoc);
						cam.x = x.doubleValue();
						cam.y = y.doubleValue();
						cam.z = z.doubleValue();
						tii.set( TracerInstruction.RESET );
						dumpCameraPosition(cam);
					}
				});
				repl.interp.wordDefinitions.put("set-camera-yaw", new StandardWordDefinition() {
					@Override public void run(Interpreter interp, SourceLocation sLoc) throws ScriptError {
						cam.yaw = interp.stackPop(Number.class, sLoc).doubleValue();
						tii.set( TracerInstruction.RESET );
						dumpCameraPosition(cam);
					}
				});
				repl.interp.wordDefinitions.put("set-camera-pitch", new StandardWordDefinition() {
					@Override public void run(Interpreter interp, SourceLocation sLoc) throws ScriptError {
						cam.pitch = interp.stackPop(Number.class, sLoc).doubleValue();
						tii.set( TracerInstruction.RESET );
						dumpCameraPosition(cam);
					}
				});
				repl.interp.wordDefinitions.put("set-camera-roll", new StandardWordDefinition() {
					@Override public void run(Interpreter interp, SourceLocation sLoc) throws ScriptError {
						cam.roll = interp.stackPop(Number.class, sLoc).doubleValue();
						tii.set( TracerInstruction.RESET );
						dumpCameraPosition(cam);
					}
				});
				while( repl.run ) {
					try {
						repl.run();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				System.err.println("REPL Exited.  Exiting program.");
				System.exit(0);
			}
		};
		commandReader.start();
		
		adj.addKeyListener(new KeyAdapter() {
			@Override public void keyPressed( KeyEvent kevt ) {
				double dir = 1;
				
				boolean shifted = kevt.isShiftDown();
				boolean controlled = kevt.isControlDown();
				
				double movedist = controlled ? 0.2 : shifted ? 5 : 1; 
				
				switch( kevt.getKeyCode() ) {
				case KeyEvent.VK_D:
					String baseName = renderDir+"/"+sceneName+"/"+sceneName+"-"+(int)cam.getExposure().getAverageExposure();
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
				
				// Camera movement
				case KeyEvent.VK_END:
					dir = -1;
				case KeyEvent.VK_HOME:
					cam.x += dir * movedist * Math.cos(cam.yaw);
					cam.z -= dir * movedist * Math.sin(cam.yaw);
					tii.set( TracerInstruction.RESET );
					dumpCameraPosition(cam);
					break;
				case KeyEvent.VK_DOWN:
					dir = -1;
				case KeyEvent.VK_UP:
					cam.x += dir * movedist * Math.sin(cam.yaw);
					cam.z += dir * movedist * Math.cos(cam.yaw);
					tii.set( TracerInstruction.RESET );
					dumpCameraPosition(cam);
					break;
				case KeyEvent.VK_PAGE_DOWN:
					dir = -1;
				case KeyEvent.VK_PAGE_UP:
					cam.y += movedist * dir;
					tii.set( TracerInstruction.RESET );
					dumpCameraPosition(cam);
					break;
				case KeyEvent.VK_RIGHT:
					dir = -1;
				case KeyEvent.VK_LEFT:
					cam.yaw += dir * movedist * Math.PI / 16;
					tii.set( TracerInstruction.RESET );
					dumpCameraPosition(cam);
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
		HDRExposure exp = cam.getExposure();
		tii.set( TracerInstruction.RESET );
		while( true ) {
			TracerInstruction ti = tii.set( TracerInstruction.CONTINUE );
			if( ti == TracerInstruction.RESET ) {
				startTime = System.currentTimeMillis();
				samplesTaken = 0;
				exp = cam.getExposure();
				exp.clear();
				adj.setExposure(exp);
			}
			
			for( int j=0; j<vectorSize; ++j ) {
				sampleMethod = cam.preview && cam.imageWidth * cam.imageHeight > vectorSize ?
					SampleMethod.RANDOM : SampleMethod.LINE;
				
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
				
				if( samplesTaken % 16384 == 0 ) {
					long time = System.currentTimeMillis();
					samplesPerSecond =
						0.8 * samplesPerSecond +
						0.2 * (samplesTaken - prevSamplesTaken) * 1000 / (time - prevTime);
					
					prevSamplesTaken = samplesTaken;
					prevTime = System.currentTimeMillis();
				}
				if( samplesTaken % 4096 == 0 ) {
					String baseName = renderDir+"/"+sceneName+"/"+sceneName+"-"+(int)exp.getAverageExposure();
					
					adj.extraStatusLines = new String[] {
						"Total samples taken: " + samplesTaken,
						"Samples per second: " + samplesPerSecond,
						"Average samples per pixel: " + exp.getAverageExposure()
					};
					
					adj.exportFilenamePrefix = baseName;
					adj.exposureUpdated();
				}
				++samplesTaken;
			}
		}
	}
}
