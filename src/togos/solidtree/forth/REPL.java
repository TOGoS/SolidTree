package togos.solidtree.forth;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Map;

import togos.lang.ScriptError;
import togos.lang.SourceLocation;
import togos.solidtree.forth.procedure.SafeProcedures;

public class REPL
{
	public boolean run = true;
	public final Interpreter interp = new Interpreter();
	public PrintStream feedbackStream = System.err;
	public InputStream commandStream = System.in;
	
	protected void showPrompt( Tokenizer t, Interpreter interp ) {
		if( interp.stackPeek() != null ) System.err.print(interp.stackPeek() + "; ");
		feedbackStream.print( t.lineNumber + "> ");
	}
	
	protected void registerReplWords( Map<String, ? super WordDefinition> ctx ) {
		ctx.put("print", new StandardWordDefinition() {
			@Override public void run( Interpreter interp, SourceLocation sLoc ) throws ScriptError {
				feedbackStream.print( interp.stackPop(Object.class, sLoc ) );
			}
		});
		ctx.put("exit", new StandardWordDefinition() {
			@Override public void run( Interpreter interp, SourceLocation sLoc ) throws ScriptError {
				run = false;
			}
		});
	}
	
	public void registerReplWords() {
		registerReplWords(interp.wordDefinitions);
	}
	
	public void run() throws IOException, Exception {
		Tokenizer tokenizer = new Tokenizer("-", 1, 1, 4, interp.delegatingTokenHandler);
		
		BufferedReader lineReader = new BufferedReader(new InputStreamReader(commandStream));
		String line;
		showPrompt(tokenizer, interp);
		while( run && (line = lineReader.readLine()) != null ) {
			tokenizer.handle(line + "\n");
			if( run && interp.tokenHandler == interp.interpretModeTokenHandler ) {
				showPrompt(tokenizer, interp);
			}
		}
	}
	
	public static void main( String[] args ) throws Exception {
		REPL repl = new REPL();
		SafeProcedures.register(repl.interp.wordDefinitions);
		repl.registerReplWords();
		repl.run();
	}
}
