package togos.solidtree.forth;

import togos.lang.ScriptError;
import togos.lang.SourceLocation;

public interface WordDefinition extends Procedure
{
	public void compile( Interpreter interp, SourceLocation sLoc ) throws ScriptError;
}
