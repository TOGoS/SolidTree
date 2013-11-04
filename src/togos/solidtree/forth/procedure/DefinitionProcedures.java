package togos.solidtree.forth.procedure;

import java.util.List;
import java.util.Map;

import togos.lang.CompileError;
import togos.lang.ScriptError;
import togos.lang.SourceLocation;
import togos.solidtree.forth.Handler;
import togos.solidtree.forth.Interpreter;
import togos.solidtree.forth.Procedure;
import togos.solidtree.forth.StandardWordDefinition;
import togos.solidtree.forth.Token;
import togos.solidtree.forth.WordDefinition;

public class DefinitionProcedures
{
	public static void register( Map<String, ? super WordDefinition> ctx ) {
		// These should be defined in a more generic function library
		ctx.put(":", new StandardWordDefinition() {
			@Override
			public void run( final Interpreter interp, SourceLocation sLoc ) throws ScriptError {
				interp.tokenHandler = new Handler<Token,ScriptError>() {
					String newWordName;
					public void handle( Token t ) {
						newWordName = t.text;
						interp.tokenHandler = interp.compileModeTokenHandler;
						interp.wordDefinitions.put(";", new WordDefinition() {
							@Override
							public void compile( Interpreter interp, SourceLocation sLoc ) throws ScriptError {
								List<Procedure> wordList = interp.flushCompiledProcedure();
								interp.wordDefinitions.put(newWordName, new UserDefinedProcedure(wordList));
								interp.tokenHandler = interp.interpretModeTokenHandler;
							}

							@Override
							public void run( Interpreter interp, SourceLocation sLoc ) throws ScriptError {
								throw new CompileError("';' cannot be used in run-mode context", sLoc);
							}
						});
					}
				};
			}
		});
		ctx.put("def-value", new StandardWordDefinition() {
			@Override
            public void run( Interpreter interp, SourceLocation sLoc ) throws ScriptError {
				String name = interp.stackPop( String.class, sLoc );
				Object value = interp.stackPop( Object.class, sLoc );
				interp.wordDefinitions.put( name, new ConstantValue(value) ); 
            }
		});
	}
}
