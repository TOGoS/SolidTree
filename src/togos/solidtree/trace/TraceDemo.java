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
import togos.hdrutil.ExposureScaler;
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
import togos.solidtree.matrix.Matrix;
import togos.solidtree.matrix.MatrixMath;
import togos.solidtree.matrix.Vector3D;
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
		protected HDRExposure exp;
		
		public void setExposure(HDRExposure exp) {
			this.exp = exp;
			this.imageHeight = exp.height;
			this.imageWidth = exp.width;
		}
		
		public HDRExposure getExposure() {
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
		DOUBLE,
		HALVE,
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
	
	static class LensSettings {
		public double fovY = Math.PI*0.3;
		public double apertureSize = 0;
		public double focalOffset = 4;
		
		public void apply( Camera cam ) {
			cam.projection = new FisheyeProjection(fovY*cam.imageWidth/cam.imageHeight, fovY);
			if( apertureSize > 0 ) {
				cam.projection = new ApertureProjection( cam.projection, apertureSize, focalOffset );
			}
		}
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
		cam.imageWidth = 96;
		cam.imageHeight = 48;
		cam.x = 0;
		cam.y = -1127;
		cam.z = 0;
		cam.yaw = Math.PI/8;
		
		final LensSettings lensSettings = new LensSettings();
		lensSettings.apply(cam);
		
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
				repl.interp.wordDefinitions.put("double-camera-resolution", new StandardWordDefinition() {
					@Override public void run(Interpreter interp, SourceLocation sLoc) throws ScriptError {
						tii.set( TracerInstruction.DOUBLE );
					}
				});
				repl.interp.wordDefinitions.put("halve-camera-resolution", new StandardWordDefinition() {
					@Override public void run(Interpreter interp, SourceLocation sLoc) throws ScriptError {
						tii.set( TracerInstruction.HALVE );
					}
				});
				repl.interp.wordDefinitions.put("set-camera-aperture-size", new StandardWordDefinition() {
					@Override public void run(Interpreter interp, SourceLocation sLoc) throws ScriptError {
						lensSettings.apertureSize = interp.stackPop(Number.class, sLoc).doubleValue();
						lensSettings.apply(cam);
						tii.set( TracerInstruction.RESET );
					}
				});
				repl.interp.wordDefinitions.put("set-camera-focal-offset", new StandardWordDefinition() {
					@Override public void run(Interpreter interp, SourceLocation sLoc) throws ScriptError {
						lensSettings.focalOffset = interp.stackPop(Number.class, sLoc).doubleValue();
						lensSettings.apply(cam);
						tii.set( TracerInstruction.RESET );
					}
				});
				repl.interp.wordDefinitions.put("set-camera-fov-y", new StandardWordDefinition() {
					@Override public void run(Interpreter interp, SourceLocation sLoc) throws ScriptError {
						lensSettings.fovY = interp.stackPop(Number.class, sLoc).doubleValue();
						lensSettings.apply(cam);
						tii.set( TracerInstruction.RESET );
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
			static final int KP_0 =  96;
			static final int KP_1 =  97;
			static final int KP_2 =  98;
			static final int KP_3 =  99;
			static final int KP_4 = 100;
			static final int KP_5 = 101;
			static final int KP_6 = 102;
			static final int KP_7 = 103;
			static final int KP_8 = 104;
			static final int KP_9 = 105;
			
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
				case KeyEvent.VK_END: case KP_3:
					dir = -1;
				case KeyEvent.VK_HOME: case KP_1:
					cam.x += dir * movedist * Math.cos(-cam.yaw);
					cam.z -= dir * movedist * Math.sin(-cam.yaw);
					tii.set( TracerInstruction.RESET );
					dumpCameraPosition(cam);
					break;
				case KeyEvent.VK_DOWN: case KP_0:
					dir = -1;
				case KeyEvent.VK_UP: case KP_5:
					cam.x += dir * movedist * Math.sin(-cam.yaw) * Math.cos(cam.pitch);
					cam.z += dir * movedist * Math.cos(-cam.yaw) * Math.cos(cam.pitch);
					cam.y += dir * movedist * Math.sin(-cam.pitch);
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
				case KeyEvent.VK_LEFT: case KeyEvent.VK_KP_LEFT: case KP_4:
					dir = -1;
				case KeyEvent.VK_RIGHT: case KeyEvent.VK_KP_RIGHT: case KP_6:
					cam.yaw += dir * movedist * Math.PI / 16;
					tii.set( TracerInstruction.RESET );
					dumpCameraPosition(cam);
					break;
				case KeyEvent.VK_KP_DOWN: case KP_2:
					dir = -1;
				case KeyEvent.VK_KP_UP: case KP_8:
					cam.pitch += dir * movedist * Math.PI / 16;
					tii.set( TracerInstruction.RESET );
					dumpCameraPosition(cam);
					break;
				case KP_7:
					dir = -1;
				case KP_9:
					cam.roll += dir * movedist * Math.PI / 16;
					tii.set( TracerInstruction.RESET );
					dumpCameraPosition(cam);
					break;
				default:
					System.err.println("Key "+kevt.getKeyCode());
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
		
		Matrix cameraTranslation = new Matrix(4,4);
		Matrix cameraRotation = new Matrix(4,4);
		Matrix cameraTransform = new Matrix(4,4);
		Matrix scratchA = new Matrix(4,4);
		Matrix scratchB = new Matrix(4,4);
		Vector3D pixelOffset = new Vector3D();
		Vector3D pixelDirection = new Vector3D();
		Vector3D rayOffset = new Vector3D();
		Vector3D rayDirection = new Vector3D();
		
		long startTime = System.currentTimeMillis();
		long samplesTaken = 0;
		long samplesTakenAtLastUpdate = 0;
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
				samplesTakenAtLastUpdate = 0;
				exp = cam.getExposure();
				exp.clear();
				adj.setExposure(exp, false);
			} else if( ti == TracerInstruction.DOUBLE ) {
				exp = ExposureScaler.scaleUp(exp);
				System.err.println("Doubling resolution to "+exp.width+"x"+exp.height);
				adj.setExposure(exp, false);
				cam.setExposure(exp);
			} else if( ti == TracerInstruction.HALVE ) {
				exp = ExposureScaler.scaleDown(exp, 2);
				System.err.println("Halved resolution to "+exp.width+"x"+exp.height);
				adj.setExposure(exp, false);
				cam.setExposure(exp);
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
			
			// pixel transform matrix = camera matrix * projection matrix(pixel x, y)
			// position vector  = pixel transform matrix * [0,0,0,1]
			// direction vector = pixel transform matrix * [0,0,1,1]
			
			MatrixMath.yawPitchRoll( cam.yaw, cam.pitch, cam.roll, scratchA, scratchB, cameraRotation );
			MatrixMath.translation( cam.x, cam.y, cam.z, cameraTranslation );
			MatrixMath.multiply( cameraTranslation, cameraRotation, cameraTransform );
			
			cam.projection.project(vectorSize, screenX, screenY, camPosX, camPosY, camPosZ, camDirX, camDirY, camDirZ);
			
			int samplesPerRedraw = exp.width*exp.height / 4;
			if( samplesPerRedraw < 2048 ) samplesPerRedraw = 2048;
			
			for( int j=0; j<vectorSize; ++j ) {
				pixelOffset.set(camPosX[j], camPosY[j], camPosZ[j]);
				pixelDirection.set(camDirX[j], camDirY[j], camDirZ[j]);
				MatrixMath.multiply( cameraTransform, pixelOffset, rayOffset );
				MatrixMath.multiply( cameraRotation, pixelDirection, rayDirection );
				
				if( cam.preview ) {
					t.quickTrace( rayOffset, rayDirection );
				} else {
					t.trace( rayOffset, rayDirection );
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
				
				if( samplesTaken - samplesTakenAtLastUpdate >= samplesPerRedraw && adj.isShowing() ) {
					long time = System.currentTimeMillis();
					samplesPerSecond =
						0.8 * samplesPerSecond +
						0.2 * (samplesTaken - samplesTakenAtLastUpdate) * 1000 / (time - prevTime);
					
					samplesTakenAtLastUpdate = samplesTaken;
					prevTime = System.currentTimeMillis();
					
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
