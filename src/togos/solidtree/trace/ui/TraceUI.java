package togos.solidtree.trace.ui;

import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;

import togos.hdrutil.AdjusterUI;
import togos.hdrutil.HDRExposure;
import togos.lang.ScriptError;
import togos.lang.SourceLocation;
import togos.solidtree.DereferenceException;
import togos.solidtree.NodeDereffer;
import togos.solidtree.NodeLoader;
import togos.solidtree.NodeLoader.HashMapLoadContext;
import togos.solidtree.NodeLoader.LoadContext;
import togos.solidtree.NodeRoot;
import togos.solidtree.SolidNode;
import togos.solidtree.forth.Interpreter;
import togos.solidtree.forth.StandardWordDefinition;
import togos.solidtree.forth.WordDefinition;
import togos.solidtree.forth.procedure.SafeProcedures;
import togos.solidtree.matrix.Matrix;
import togos.solidtree.matrix.MatrixMath;
import togos.solidtree.trace.FisheyeProjection;
import togos.solidtree.trace.NodeConverter;
import togos.solidtree.trace.Projection;
import togos.solidtree.trace.Scene;
import togos.solidtree.trace.TraceNode;
import togos.solidtree.trace.Tracer;
import togos.solidtree.trace.sky.AdditiveSkySphere;
import togos.solidtree.trace.ui.UIRenderThread.RenderSettings;
import togos.solidtree.trace.ui.UIRenderThread.View;

public class TraceUI
{
	protected final UIRenderThread renderThread = new UIRenderThread();
	protected final AdjusterUI adjusterPanel = new AdjusterUI();
	
	protected Scene scene;	
	protected int imageWidth = 192, imageHeight = 96;
	protected Projection projection = new FisheyeProjection(1, 0.5);
	protected double cameraX = 0.1, cameraY = 30.1, cameraZ = 0.1;
	protected Matrix cameraTransform = new Matrix(4,4);
	protected Tracer.Mode traceMode = Tracer.Mode.QUICK;
	
	public TraceUI() {
		updateCameraTransform();
	}
	
	public void start() {
		final Frame f = new Frame();
		f.add(adjusterPanel);
		f.pack();
		f.addWindowListener(new WindowAdapter() {
			@Override public void windowClosing( WindowEvent evt ) {
				f.dispose();
				System.exit(0);
			}
		});
		f.setVisible(true);
		adjusterPanel.requestFocus();
		
		renderThread.setExposureUpdateListener(new ExposureUpdateListener() {
			@Override public void exposureUpdated(HDRExposure exp) {
				adjusterPanel.setExposure(exp);
			}
		});
		renderThread.setDaemon(true);
		renderThread.start();
	}
	
	protected View getView() {
		if( scene == null || projection == null || cameraTransform == null || traceMode == null ) return null;
		return new View(scene, projection, cameraTransform, traceMode);
	}
	
	protected void updateSettings() {
		renderThread.setRenderSettings(new RenderSettings(
			getView(), imageWidth, imageHeight
		));
	}
	
	public void setResolution(int w, int h) {
		this.imageWidth = w;
		this.imageHeight = h;
		updateSettings();
	}
	
	public void setScene(Scene s) {
		scene = s;
		updateSettings();
	}
	
	protected void updateCameraTransform() {
		final Matrix scratchA = new Matrix(4,4);
		final Matrix scratchB = new Matrix(4,4);
		final Matrix cameraRotation = new Matrix(4,4);
		final Matrix cameraTranslation = new Matrix(4,4);
		MatrixMath.yawPitchRoll( 1, 0.5, 0, scratchA, scratchB, cameraRotation );
		MatrixMath.translation( cameraX, cameraY, cameraZ, cameraTranslation );
		MatrixMath.multiply( cameraTranslation, cameraRotation, cameraTransform );
		updateSettings();
	}
	
	public void registerUiCommands(Map<String,WordDefinition> ctx) {
		ctx.put("set-camera-position", new StandardWordDefinition() {
			@Override public void run(Interpreter interp, SourceLocation sLoc) throws ScriptError {
				cameraZ = interp.stackPop(Number.class, sLoc).doubleValue();
				cameraY = interp.stackPop(Number.class, sLoc).doubleValue();
				cameraX = interp.stackPop(Number.class, sLoc).doubleValue();
				updateCameraTransform();
			}
		});
		ctx.put("set-trace-mode", new StandardWordDefinition() {
			@Override public void run(Interpreter interp, SourceLocation sLoc) throws ScriptError {
				String modeName = interp.stackPop(String.class, sLoc);
				if( "quick".equals(modeName) ) {
					traceMode = Tracer.Mode.QUICK;
				} else if( "full".equals(modeName) ) {
					traceMode = Tracer.Mode.FULL;
				} else {
					throw new ScriptError("Invalid trace mode name: '"+modeName+"'", sLoc);
				}
				updateSettings();
			}
		});
	}
	
	NodeDereffer nodeDereffer = new NodeDereffer(); 
	
	// TODO: Move these into some utility class or something
	
	protected TraceNode toTraceNode( Object o ) throws DereferenceException {
		if( o instanceof TraceNode ) {
			return (TraceNode)o;
		} else if( o instanceof SolidNode ) {
			return new NodeConverter(nodeDereffer).toTraceNode((SolidNode)o);
		} else {
			throw new RuntimeException("Don't know how to turn "+o+" into a trace node");
		}
	}
	
	protected NodeRoot<TraceNode> toTraceNodeRoot( Object o ) throws DereferenceException {
		if( o instanceof NodeRoot ) {
			@SuppressWarnings("rawtypes")
			NodeRoot r = (NodeRoot)o;
			return new NodeRoot<TraceNode>( toTraceNode(r.node), r.x0, r.y0, r.z0, r.x1, r.y1, r.z1);
		} else {
			return new NodeRoot<TraceNode>( toTraceNode(o), 1024, 1024, 1024 );
		}
	}
	
	public void _main(String[] args) throws DereferenceException, IOException, ScriptError {
		TraceUI traceUi = new TraceUI();
		
		final Frame f = new Frame("TraceUI");
		f.add(traceUi.adjusterPanel);
		f.addWindowListener(new WindowAdapter() {
			@Override public void windowClosing(WindowEvent arg0) {
				f.dispose();
			}
		});
		f.setVisible(true);
		
		LoadContext<SolidNode> loadCtx = new HashMapLoadContext<SolidNode>();
		NodeLoader nl = new NodeLoader();
		nl.includePath.add(new File("world"));
		NodeRoot<TraceNode> root = toTraceNodeRoot( nl.get("world", loadCtx) );
		traceUi.setScene(new Scene(root, new AdditiveSkySphere()));
		
		traceUi.start();
		
		Interpreter uiCommandInterpreter = new Interpreter();
		SafeProcedures.register(uiCommandInterpreter.wordDefinitions);
		traceUi.registerUiCommands(uiCommandInterpreter.wordDefinitions);
		uiCommandInterpreter.wordDefinitions.put("exit", new StandardWordDefinition() {
			@Override public void run(Interpreter interp, SourceLocation sLoc) throws ScriptError {
				System.exit(0);
			}
		});
		
		Reader r = new InputStreamReader(System.in);
		while( true ) {
			try {
				uiCommandInterpreter.runScript(r, "-");
			} catch( Exception e ) {
				e.printStackTrace();
				System.err.println("Resuming interpreter");
			}
		}
	}
	
	public static void main(String[] args) {
		try {
			new TraceUI()._main(args);
		} catch( DereferenceException e ) {
			throw new RuntimeException(e);
		} catch( IOException e ) {
			throw new RuntimeException(e);
		} catch( ScriptError e ) {
			throw new RuntimeException(e);
		}
	}
}
