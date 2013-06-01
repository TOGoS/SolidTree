package togos.solidtree.forth.error;

import togos.lang.ScriptError;
import togos.lang.SourceLocation;

public class ScriptRuntimeError extends ScriptError
{
	private static final long serialVersionUID = 1L;
	public ScriptRuntimeError( String msg, SourceLocation sLoc ) {
		super( msg, sLoc );
	}
}