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
	HashMap<String, WordDef> wordDefinitions = new HashMap<String, WordDef>();
	
	protected WordDef getWordOrError( String word, SourceLocation sLoc ) throws CompileError {
		WordDef d = wordDefinitions.get(word);
		if( d == null ) {
			throw new CompileError("Undefined word '"+word+"'", sLoc );
		}
		return d;
	}
	
	Handler<Token,ScriptError> interpretModeTokenHandler = new Handler<Token,ScriptError>() {
		Pattern DEC_INT_PATTERN = Pattern.compile("([+-])?(\\d+)");
		
		public void handle( Token t ) throws ScriptError {
			switch( t.type ) {
			case DOUBLE_QUOTED_STRING:
				stack.add(t.text);
				break;
			default:
				Matcher m = DEC_INT_PATTERN.matcher(t.text);
				if( m.matches() ) {
					stack.add( Integer.valueOf( ("-".equals(m.group(1)) ? -1 : 1) * Integer.parseInt(m.group(2)) ));
				} else {
					getWordOrError(t.text, t).run(Interpreter.this);
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
}
