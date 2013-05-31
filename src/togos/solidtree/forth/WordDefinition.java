package togos.solidtree.forth;

import togos.lang.ScriptError;
import togos.lang.SourceLocation;

public interface WordDefinition
{
	public void compile( Interpreter interp, SourceLocation sLoc ) throws ScriptError;
	public void run( Interpreter interp, SourceLocation sLoc ) throws ScriptError;
}
