package togos.solidtree.forth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import togos.lang.CompileError;
import togos.lang.ScriptError;
import togos.lang.SourceLocation;

public class Interpreter
{
	ArrayList<Object> stack = new ArrayList<Object>();
	public HashMap<String, WordDefinition> wordDefinitions = new HashMap<String, WordDefinition>();
	
	protected WordDefinition getWordOrError( String word, SourceLocation sLoc ) throws CompileError {
		WordDefinition d = wordDefinitions.get(word);
		if( d == null ) {
			throw new CompileError("Undefined word '"+word+"'", sLoc );
		}
		return d;
	}
	
	Handler<Token,ScriptError> interpretModeTokenHandler = new Handler<Token,ScriptError>() {
		Pattern DEC_INT_PATTERN = Pattern.compile("([+-])?(\\d+)");
		Pattern DEC_FLOAT_PATTERN = Pattern.compile("([+-])?(\\d+\\.\\d+)");
		
		public void handle( Token t ) throws ScriptError {
			switch( t.type ) {
			case DOUBLE_QUOTED_STRING:
				stack.add(t.text);
				break;
			default:
				Matcher m;
				if( (m = DEC_INT_PATTERN.matcher(t.text)).matches() ) {
					stack.add( Long.valueOf( ("-".equals(m.group(1)) ? -1 : 1) * Long.parseLong(m.group(2)) ));
				} else if( (m = DEC_FLOAT_PATTERN.matcher(t.text)).matches() ) {
					stack.add( Double.valueOf( ("-".equals(m.group(1)) ? -1 : 1) * Double.parseDouble(m.group(2)) ));
				} else {
					getWordOrError(t.text, t).run(Interpreter.this, t);
				}
			}
		}
	};
	
	Handler<Token,ScriptError> tokenHandler = interpretModeTokenHandler;
	
	public final Handler<Token,ScriptError> delegatingTokenHandler = new Handler<Token,ScriptError>() {
		public void handle( Token t ) throws ScriptError {
			tokenHandler.handle(t);
		}
	};
	
	////
	
	public void addToInstructionList( WordDefinition word, SourceLocation sLoc ) {
		throw new UnsupportedOperationException("Compiling not yet worky");
    }
	
	static class ScriptRuntimeError extends ScriptError
	{
		private static final long serialVersionUID = 1L;
		public ScriptRuntimeError( String msg, SourceLocation sLoc ) {
			super( msg, sLoc );
		}
	}
	
	static class StackUnderflowError extends ScriptRuntimeError
	{
		private static final long serialVersionUID = 1L;
		public StackUnderflowError( SourceLocation sLoc ) {
			super("Stack underflow", sLoc);
		}
	}
	
	public Object stackRemove( int fromTop, SourceLocation sLoc ) throws StackUnderflowError {
		int index = stack.size()-fromTop-1;
		if( index < 0 ) {
			throw new StackUnderflowError(sLoc);
		}
		return stack.remove(index);
	}
	
	public <V> V stackPop( Class<V> requiredType, SourceLocation sLoc ) throws ScriptError {
		Object v = stackRemove(0, sLoc);
		if( !requiredType.isAssignableFrom(v.getClass()) ) {
			throw new ScriptError("Required type "+requiredType+" on top of stack, but found a "+v, sLoc);
		}
		return requiredType.cast(v);
    }
	
	public void stackPush( Object o ) {
		stack.add(o);
	}
}
