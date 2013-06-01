package togos.solidtree.forth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import togos.lang.CompileError;
import togos.lang.ScriptError;
import togos.lang.SourceLocation;
import togos.solidtree.forth.error.StackUnderflowError;
import togos.solidtree.forth.procedure.ConstantValue;

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
	
	Pattern DEC_INT_PATTERN = Pattern.compile("([+-])?(\\d+)");
	Pattern DEC_FLOAT_PATTERN = Pattern.compile("([+-])?(\\d+\\.\\d+)");
	
	WordDefinition getWord(Token t) throws CompileError {
		switch( t.type ) {
		case DOUBLE_QUOTED_STRING:
			return new ConstantValue(t.text);
		default:
			Matcher m;
			if( (m = DEC_INT_PATTERN.matcher(t.text)).matches() ) {
				return new ConstantValue(Long.valueOf(("-".equals(m.group(1)) ? -1 : 1) * Long.parseLong(m.group(2)))); 
			} else if( (m = DEC_FLOAT_PATTERN.matcher(t.text)).matches() ) {
				return new ConstantValue( Double.valueOf( ("-".equals(m.group(1)) ? -1 : 1) * Double.parseDouble(m.group(2)) ));
			} else {
				return getWordOrError(t.text, t);
			}
		}
	}
	
	public final Handler<Token,ScriptError> compileModeTokenHandler = new Handler<Token,ScriptError>() {
		public void handle( Token t ) throws ScriptError {
			getWord(t).compile(Interpreter.this, t);
		}
	};
	
	public final Handler<Token,ScriptError> interpretModeTokenHandler = new Handler<Token,ScriptError>() {
		public void handle( Token t ) throws ScriptError {
			getWord(t).run(Interpreter.this, t);
		}
	};
	
	public Handler<Token,ScriptError> tokenHandler = interpretModeTokenHandler;
	
	public final Handler<Token,ScriptError> delegatingTokenHandler = new Handler<Token,ScriptError>() {
		public void handle( Token t ) throws ScriptError {
			tokenHandler.handle(t);
		}
	};
	
	//// Compiling functions
	
	List<Procedure> procList = new ArrayList<Procedure>();
	
	public List<Procedure> flushCompiledProcedure() {
		List<Procedure> procList = this.procList;
		this.procList = new ArrayList<Procedure>();
		return procList;
	}
	
	public void addToInstructionList( Procedure proc, SourceLocation sLoc ) {
		procList.add(proc);
    }
	
	//// Stack functions
	
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

	public Object stackPeek() {
		return stack.size() > 0 ? stack.get(stack.size()-1) : null;
	}
}
