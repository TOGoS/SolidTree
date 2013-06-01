package togos.solidtree.forth;

import togos.lang.ScriptError;
import togos.lang.SourceLocation;

public interface Procedure
{
	public void run( Interpreter interp, SourceLocation sLoc ) throws ScriptError;
}
