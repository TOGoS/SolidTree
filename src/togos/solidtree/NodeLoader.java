package togos.solidtree;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import togos.lang.BaseSourceLocation;
import togos.lang.ScriptError;
import togos.lang.SourceLocation;
import togos.solidtree.forth.Interpreter;
import togos.solidtree.forth.StandardWordDefinition;
import togos.solidtree.forth.Tokenizer;
import togos.solidtree.forth.procedure.NodeProcedures;

public class NodeLoader
{
	public interface LoadContext<T>
	{
		public T get(String name);
	}
	
	public static class NullLoadContext<T> implements LoadContext<T>
	{
		@Override public T get(String name) { return null; }
	}
	
	public static class HashMapLoadContext<T> implements LoadContext<T>
	{
		protected final LoadContext<? extends T> parent;
		protected final HashMap<String,T> values = new HashMap<String, T>();
		
		public HashMapLoadContext() {
			this(new NullLoadContext<T>());
		}
		
		public HashMapLoadContext(LoadContext<? extends T> parent) {
			this.parent = parent;
		}
		
		@Override public T get(String name) {
			if( values.containsKey(name) ) {
				return values.get(name);
			}
			T v = parent.get(name);
			values.put(name, v);
			return v;
		}
		
		protected void put(String name, T value) {
			values.put(name, value);
		}
	}
		
	protected enum ReadMode {
		ROOT,
		DEFS,
		DATA
	};
	
	static final Pattern DEFINITION_PATTERN = Pattern.compile("(\\S+)\\s+(\\??=)\\s+(\\S+)");
	static final Pattern DIMENSIONS_PATTERN = Pattern.compile("=data ([+-][xyz][+-][xyz][+-][xyz]) (\\d+)x(\\d+)x(\\d+)");
	
	public ArrayList<File> includePath = new ArrayList<File>();
	
	public Object get( String name, LoadContext<?> ctx ) throws ScriptError, IOException {
		Object fromCtx = ctx.get(name);
		if( fromCtx != null ) return fromCtx;
		
		for( File d : includePath ) {
			File f = new File( d, name + ".tsn" );
			if( f.exists() ) return readTextNode(f, ctx);
			
			f = new File( d, name + ".fs" );
			if( f.exists() ) return readForthNode(f, ctx);
		}
		
		return null;
	}
	
	public Object getNotNull( String name, LoadContext<?> ctx, SourceLocation sLoc ) throws IOException, ScriptError {
		Object n = get(name, ctx);
		if( n == null ) throw new ScriptError("'"+name+"' cannot be resolved", sLoc);
		return n;
	}

	public SolidNode getNode( String name, LoadContext<?> ctx, SourceLocation sLoc ) throws IOException, ScriptError {
		Object n = getNotNull(name, ctx, sLoc);
		if( n instanceof SolidNode ) {
			return (SolidNode)n;
		}
		throw new ScriptError("'"+name+"' resolved to a "+n.getClass()+" instead of a SolidNode", sLoc);
	}
	
	public Object readForthNode( File f, LoadContext<?> parentContext ) throws ScriptError, IOException {
		final HashMapLoadContext<Object> ctx = new HashMapLoadContext<Object>(parentContext);
		
		Interpreter interp = new Interpreter();
		NodeProcedures.register(interp.wordDefinitions);
		interp.wordDefinitions.put("ctx-get", new StandardWordDefinition() {
			// name -> value
			@Override public void run( Interpreter interp, SourceLocation sLoc ) throws ScriptError {
				String name = interp.stackPop(String.class, sLoc);
				try {
					interp.stackPush( get( name, ctx ) );
				} catch(IOException e) {
					throw new RuntimeException(e);
				}
			}
		});
		interp.wordDefinitions.put("ctx-put", new StandardWordDefinition() {
			// value, name -> ()
			@Override public void run( Interpreter interp, SourceLocation sLoc ) throws ScriptError {
				String name = interp.stackPop(String.class, sLoc);
				Object value = interp.stackPop(Object.class, sLoc);
				ctx.put( name, value );
			}
		});
		Tokenizer tokenizer = new Tokenizer(f.getPath(), 1, 1, 4, interp.delegatingTokenHandler);
		
		try {
			FileReader scriptReader = new FileReader(f);
			char[] buf = new char[1024];
			int i;
			while( (i = scriptReader.read(buf)) > 0 ) {
				tokenizer.handle(buf, i);
			}
			tokenizer.end();
			scriptReader.close();
		} catch( ScriptError e ) {
			throw e;
		} catch( IOException e ) {
			throw e;
		} catch( Exception e ) {
			throw new RuntimeException(e);
		}
		
		return interp.stackPop( Object.class, BaseSourceLocation.NONE );
	}
	
	public SolidNode readTextNode( File f, LoadContext<?> ctx ) throws ScriptError, IOException
	{
		BufferedReader br = new BufferedReader(new FileReader(f));
		try {
			return readTextNode(br, f.getPath(), ctx);
		} finally {
			br.close();
		}
	}
	
	public SolidNode readTextNode(BufferedReader r, String filename, LoadContext<?> parentContext) throws ScriptError, IOException {
		String line;
		
		HashMapLoadContext<Object> context = new HashMapLoadContext<Object>(parentContext);
		
		char[] data = null;
		int dataSize = 0;
		int dataIdx = 0;
		
		ReadMode mode = ReadMode.ROOT;
		Matcher m;
		
		int[] dims = new int[3];
		int lineNum = 0;
		int dataLineNum = 0;
		
		outerLineReadLoop: while( (line = r.readLine()) != null ) {
			++lineNum;
			
			line = line.trim();
			if( "".equals(line) ) {
				// Comment!  Skip!
			} else if( "=defs".equals(line) ) {
				mode = ReadMode.DEFS;
			} else if( (m = DIMENSIONS_PATTERN.matcher(line)).matches() ) {
				if( data != null ) {
					throw new ScriptError("=data already given", new BaseSourceLocation(filename, lineNum, 0));
				}
				dataLineNum = lineNum+1;
				mode = ReadMode.DATA;
				String orientation = m.group(1);
				if( !"+x+z-y".equals(orientation) ) {
					throw new ScriptError("Only +x+z-y orientation supported; can't handle "+orientation, new BaseSourceLocation(filename, lineNum, 0));
				}
				dims[0] = Integer.parseInt(m.group(2));
				dims[1] = Integer.parseInt(m.group(3));
				dims[2] = Integer.parseInt(m.group(4));
				dataSize = dims[0]*dims[1]*dims[2];
				data = new char[dataSize];
				mode = ReadMode.DATA;
				while( dataIdx < dataSize ) {
					line = r.readLine();
					++lineNum;
					if( line == null ) {
						throw new ScriptError("Reached end of file before all "+dataSize+" data was read", new BaseSourceLocation(filename, lineNum, 0));
					}
					line = line.trim();
					if( line.startsWith("# ") ) continue;
					for( int i=0; i<line.length() && dataIdx<dataSize; ++i ) {
						char c = line.charAt(i);
						if( c == ' ' || c == '\r' || c == '\n' ) {
							continue;
						}
						data[dataIdx++] = c;
					}
				}
			} else if( !line.startsWith("=") ) {
				switch( mode ) {
				case DEFS:
					if( (m = DEFINITION_PATTERN.matcher(line)).matches() ) {
						String lName = m.group(1);
						String oper  = m.group(2);
						String rName = m.group(3);
						if( "?=".equals(oper) ) {
							if( context.get(lName) == null ) {
								context.put(lName, getNotNull(rName, context, new BaseSourceLocation(filename, lineNum, 0)));
							}
						} else {
							context.put(lName, getNotNull(rName, context, new BaseSourceLocation(filename, lineNum, 0)));
						}
					} else {
						throw new ScriptError("Read unexpected line in "+mode+" mode: "+line, new BaseSourceLocation(filename, lineNum, 0));
					}
					break;
				case ROOT: case DATA:
					if( line.startsWith("#") ) continue outerLineReadLoop;
					throw new ScriptError("Read unexpected line in "+mode+" mode: "+line, new BaseSourceLocation(filename, lineNum, 0));
				default:
					throw new RuntimeException("Bad state");
				}
			} else {
				throw new ScriptError("Unrecognized line: "+line, new BaseSourceLocation(filename, lineNum, 0));
			}
		}
		
		if( data == null ) throw new ScriptError("No data given", new BaseSourceLocation(filename, lineNum, 0));
		if( dataSize == 0 ) throw new ScriptError("Zero-sized node", new BaseSourceLocation(filename, lineNum, 0));
		if( dataSize == 1 ) return getNode(String.valueOf(data[0]), context, new BaseSourceLocation(filename, dataLineNum, 0));
		
		SolidNode[] snData = new SolidNode[dataSize];
		int w = dims[0], d = dims[1], h = dims[2];
		for( int j=0, y=h-1; y>=0; --y ) for( int z=0; z<d; ++z ) for( int x=0; x<w; ++x, ++j ) {
			snData[x+y*h+z*w*h] = getNode(String.valueOf(data[j]), context, new BaseSourceLocation(filename, dataLineNum, 0));
		}
		return new SolidNode(StandardMaterial.SPACE, w , h, d, snData);
	}
}
