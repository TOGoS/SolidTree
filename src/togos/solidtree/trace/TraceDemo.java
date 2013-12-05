package togos.solidtree.trace;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.util.HashSet;
import java.util.Set;

import togos.hdrutil.AdjusterUI;
import togos.hdrutil.ChunkyDump;
import togos.hdrutil.ExposureScaler;
import togos.hdrutil.FileUtil;
import togos.hdrutil.HDRExposure;
import togos.lang.ScriptError;
import togos.lang.SourceLocation;
import togos.solidtree.NodeLoader;
import togos.solidtree.NodeLoader.HashMapLoadContext;
import togos.solidtree.NodeLoader.LoadContext;
import togos.solidtree.DColor;
import togos.solidtree.NodeRoot;
import togos.solidtree.SimplexNoise;
import togos.solidtree.SolidNode;
import togos.solidtree.StandardMaterial;
import togos.solidtree.SurfaceMaterial;
import togos.solidtree.SurfaceMaterialLayer;
import togos.solidtree.forth.Interpreter;
import togos.solidtree.forth.REPL;
import togos.solidtree.forth.StandardWordDefinition;
import togos.solidtree.forth.procedure.SafeProcedures;
import togos.solidtree.matrix.Matrix;
import togos.solidtree.matrix.MatrixMath;
import togos.solidtree.trace.job.DistributingRenderServer;
import togos.solidtree.trace.job.InfiniteXYOrderedPixelRayIteratorIterator;
import togos.solidtree.trace.job.LocalRenderServer;
import togos.solidtree.trace.job.RenderResult;
import togos.solidtree.trace.job.RenderResultIterator;
import togos.solidtree.trace.job.RenderTask;
import togos.solidtree.trace.job.inet.InetRenderServer;
import togos.solidtree.trace.sky.AdditiveSkySphere;

public class TraceDemo
{
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
	
	protected static TraceNode toTraceNode( Object o ) {
		if( o instanceof TraceNode ) {
			return (TraceNode)o;
		} else if( o instanceof SolidNode ) {
			return new NodeConverter().toTraceNode((SolidNode)o);
		} else {
			throw new RuntimeException("Don't know how to turn "+o+" into a trace node");
		}
	}
	
	protected static NodeRoot<TraceNode> toTraceNodeRoot( Object o ) {
		if( o instanceof NodeRoot ) {
			@SuppressWarnings("rawtypes")
			NodeRoot r = (NodeRoot)o;
			return new NodeRoot<TraceNode>( toTraceNode(r.node), r.x0, r.y0, r.z0, r.x1, r.y1, r.z1);
		} else {
			return new NodeRoot<TraceNode>( toTraceNode(o), 1024, 1024, 1024 );
		}
	}
	
	public static void main( String[] args ) throws Exception {
		final String renderDir = "renders";
		final String sceneName = "testrender"+System.currentTimeMillis();
		
		final Interrupt<TracerInstruction> tii = new Interrupt<TracerInstruction>();
		
		System.err.println("Building world...");
		
		LoadContext<SolidNode> loadCtx = new HashMapLoadContext<SolidNode>();
		
		NodeLoader nl = new NodeLoader();
		nl.includePath.add(new File("world"));
		//NodeRoot<TraceNode> root = toTraceNodeRoot( nl.get("world", loadCtx) );
		final SimplexNoise sn = new SimplexNoise();
		//final StandardMaterial brick = (StandardMaterial)nl.get("black-plastic", loadCtx);
		final StandardMaterial noiseMaterial = StandardMaterial.opaque(
			new SurfaceMaterial( new SurfaceMaterialLayer(
				1, new DColor(0.4,0.3,0.2), DColor.BLACK, 0, 1, 1, 0
			), new SurfaceMaterialLayer(
				0.5, DColor.WHITE, DColor.BLACK, 1, 0, 0, 0
			))
		);
		NodeRoot<TraceNode> root = new NodeRoot<TraceNode>(
			new TraceNode(TraceNode.DIV_Y,
				0.1,
				new TraceNode(
					(StandardMaterial)nl.get("light", loadCtx)
				), new TraceNode(TraceNode.DIV_Y,
					0.5,
					new TraceNode(
						StandardMaterial.SPACE
					),
					new TraceNode(TraceNode.DIV_FUNC_GLOBAL, new TraceNode.DensityFunction() {
						@Override public double getMaxGradient() {
							return 2;
						}
						@Override public double apply(double x, double y, double z) {
							return sn.apply((float)x, (float)y, (float)z);
							//return (y - 1)/2 + sn.apply((float)x, 0, (float)z) * x;
						}
					}, new TraceNode(StandardMaterial.SPACE), new TraceNode(noiseMaterial))
				)
			), 10
		);
		
		final Camera cam = new Camera();
		cam.imageWidth = 96;
		cam.imageHeight = 48;
		cam.x = 0.1;
		cam.y = 0.1;
		cam.z = 0.1;
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
				case KeyEvent.VK_HOME: case KP_1:
					dir = -1;
				case KeyEvent.VK_END: case KP_3:
					cam.x += dir * movedist * Math.cos(-cam.yaw);
					cam.z -= dir * movedist * Math.sin(-cam.yaw);
					tii.set( TracerInstruction.RESET );
					dumpCameraPosition(cam);
					break;
				case KeyEvent.VK_UP: case KP_5:
					dir = -1;
				case KeyEvent.VK_DOWN: case KP_0:
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
				case KeyEvent.VK_KP_UP: case KP_8:
					dir = -1;
				case KeyEvent.VK_KP_DOWN: case KP_2:
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
		
		final Set<RenderResultChannel> desiredChannels = new HashSet<RenderResultChannel>();
		desiredChannels.add(RenderResultChannel.RED);
		desiredChannels.add(RenderResultChannel.GREEN);
		desiredChannels.add(RenderResultChannel.BLUE);
		desiredChannels.add(RenderResultChannel.EXPOSURE);
		
		final DistributingRenderServer renderServer = new DistributingRenderServer();
		
		int localRenderThreadCount = 1; //Runtime.getRuntime().availableProcessors();
		
		for( int i=localRenderThreadCount; i>0; --i ) {
			Thread t = new Thread("Render worker "+i) {
				@Override public void run() {
					int maxTaskRepititions = 1;
					RenderTask task;
					LocalRenderServer lrs = new LocalRenderServer();
					try {
						while( (task = renderServer.takeTask()) != null ) {
							RenderResultIterator rri = lrs.start(task);
							RenderResult res;
							for( int r=0; r<maxTaskRepititions && (res = rri.nextResult()) != null; ++r ) {
								renderServer.putTaskResult(task.taskId, res);
							}
						}
					} catch( InterruptedException e ) {
						Thread.currentThread().interrupt();
					} catch( IOException e ) {
						System.err.println("Exiting task runner due to exception:");
						e.printStackTrace();
					}
				}
			};
			t.start();
		}
		
		InetRenderServer irs = new InetRenderServer("local Inet render server", renderServer, InetRenderServer.DEFAULT_PORT);
		irs.start();
		
		long startTime = System.currentTimeMillis();
		long prevTime = startTime-1;
		long samplesTaken = 0;
		double samplesPerSecond = 0;
		long samplesTakenAtLastUpdate = 0;
		HDRExposure exp = cam.getExposure();
		RenderResultIterator renderResultIterator = null;
		Scene scene = null;
		boolean restartWorker = false;
		tii.set( TracerInstruction.RESET );
		int innerIterations = 1;
		long lastDump = System.currentTimeMillis();
		long lastRedraw = 0;
		while( true ) {
			TracerInstruction ti = tii.set( TracerInstruction.CONTINUE );
			if( ti == TracerInstruction.RESET ) {
				restartWorker = true;
				innerIterations = 1;
				startTime = System.currentTimeMillis();
				exp = cam.getExposure();
				exp.clear();
				adj.setExposure(exp);
				scene = new Scene(root, new AdditiveSkySphere());
				
				File sceneFile = new File(renderDir+"/"+sceneName+"/"+sceneName+".scene");
				System.err.println("Saving scene to "+sceneFile);
				FileUtil.mkParentDirs(sceneFile);
				boolean successfullySavedScene = false;
				ObjectOutputStream sceneOs = new ObjectOutputStream(new FileOutputStream(sceneFile));
				try {
					sceneOs.writeObject(scene);
					successfullySavedScene = true;
				} catch( NotSerializableException e ) {
					System.err.println("Warning: Scene not serializable: "+e.getMessage());
				} finally {
					sceneOs.close();
				}
				if( !successfullySavedScene ) {
					System.err.println("Deleting "+sceneFile);
					sceneFile.delete();
				}
			} else if( ti == TracerInstruction.DOUBLE ) {
				restartWorker = true;
				innerIterations = 1;
				exp = ExposureScaler.scaleUp(exp);
				System.err.println("Doubling resolution to "+exp.width+"x"+exp.height);
				adj.setExposure(exp);
				cam.setExposure(exp);
			} else if( ti == TracerInstruction.HALVE ) {
				restartWorker = true;
				innerIterations = 1;
				exp = ExposureScaler.scaleDown(exp, 2);
				System.err.println("Halved resolution to "+exp.width+"x"+exp.height);
				adj.setExposure(exp);
				cam.setExposure(exp);
			}
			
			if( restartWorker ) {
				if( renderResultIterator != null ) renderResultIterator.close();
				renderResultIterator = null;
				restartWorker = false;
			}
			
			final int imageWidth = cam.imageWidth;
			final int imageHeight = cam.imageHeight;
			
			if( renderResultIterator == null ) {
				final Matrix cameraTranslation = new Matrix(4,4);
				final Matrix cameraRotation = new Matrix(4,4);
				final Matrix cameraTransform = new Matrix(4,4);
				final Matrix scratchA = new Matrix(4,4);
				final Matrix scratchB = new Matrix(4,4);
				
				MatrixMath.yawPitchRoll( cam.yaw, cam.pitch, cam.roll, scratchA, scratchB, cameraRotation );
				MatrixMath.translation( cam.x, cam.y, cam.z, cameraTranslation );
				MatrixMath.multiply( cameraTranslation, cameraRotation, cameraTransform );
				
				RenderTask task = new RenderTask(
					cam.preview ? Tracer.Mode.QUICK : Tracer.Mode.FULL, 
					scene,
					new InfiniteXYOrderedPixelRayIteratorIterator(imageWidth, imageHeight, cam.projection, cameraTransform, innerIterations),
					desiredChannels);
				
				renderResultIterator = renderServer.start(task);
			}
			
			RenderResult nextResult = renderResultIterator.nextResult();
			HDRExposure nextResultExposure = RenderUtil.toHdrExposure(nextResult, imageWidth, imageHeight);
			samplesTaken += nextResult.sampleCount;
			
			//// Update UI
			
			long currentTime = System.currentTimeMillis();
			if( currentTime - lastRedraw > 200 ) {
				lastRedraw = currentTime;
				exp.add(nextResultExposure);
				adj.setExposure(exp);
			}
			
			// Autosave every 30 minutes!
			if( currentTime - lastDump > 1800*1000 ) {
				File autoDumpFile = new File(renderDir+"/"+sceneName+"/"+sceneName+"-"+(int)exp.getAverageExposure()+".dump");
				FileUtil.mkParentDirs(autoDumpFile);
				System.err.print("Auto-saving "+autoDumpFile+"...");
				ChunkyDump.saveExposure(exp, autoDumpFile);
				System.err.println("done");
				lastDump = currentTime;
			}
			
			samplesPerSecond =
				0.8 * samplesPerSecond +
				0.2 * (samplesTaken - samplesTakenAtLastUpdate) * 1000 / (currentTime - prevTime);
			
			samplesTakenAtLastUpdate = samplesTaken;
			prevTime = currentTime;
			
			adj.extraStatusLines = new String[] {
				"Total samples taken: " + samplesTaken,
				"Samples per second: " + samplesPerSecond,
				"Average samples per pixel: " + exp.getAverageExposure()
			};
			
			String baseName = renderDir+"/"+sceneName+"/"+sceneName+"-"+(int)exp.getAverageExposure();
			
			adj.exportFilenamePrefix = baseName;
			adj.setExposure(exp);
			
			if( innerIterations == 1 ) { 
				innerIterations = 10;
				restartWorker = true;
			}
		}
	}
}
