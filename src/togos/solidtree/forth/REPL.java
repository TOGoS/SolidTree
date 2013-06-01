package togos.solidtree.forth;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import togos.lang.ScriptError;
import togos.lang.SourceLocation;
import togos.solidtree.forth.procedure.ArithmeticProcedures;
import togos.solidtree.forth.procedure.NodeProcedures;

public class REPL
{
	protected static void showPrompt( Tokenizer t, Interpreter interp ) {
		if( interp.stackPeek() != null ) System.err.print(interp.stackPeek() + "; ");
		System.err.print( t.lineNumber + "> ");
	}
	
	public static void main( String[] args ) throws Exception {
		Interpreter interp = new Interpreter();
		NodeProcedures.register(interp.wordDefinitions);
		ArithmeticProcedures.register(interp.wordDefinitions);
		interp.wordDefinitions.put("print", new StandardWordDefinition() {
			@Override public void run( Interpreter interp, SourceLocation sLoc ) throws ScriptError {
				System.out.print( interp.stackPop(Object.class, sLoc ) );
			}
		});
		interp.wordDefinitions.put("exit", new StandardWordDefinition() {
			@Override public void run( Interpreter interp, SourceLocation sLoc ) throws ScriptError {
				System.exit(0);
			}
		});
		Tokenizer tokenizer = new Tokenizer("-", 1, 1, 4, interp.delegatingTokenHandler);
		
		BufferedReader lineReader = new BufferedReader(new InputStreamReader(System.in));
		String line;
		showPrompt(tokenizer, interp);
		while( (line = lineReader.readLine()) != null ) {
			tokenizer.handle(line + "\n");
			if( interp.tokenHandler == interp.interpretModeTokenHandler ) {
				showPrompt(tokenizer, interp);
			}
		}
	}
}
