package togos.solidtree.forth.error;

import togos.lang.SourceLocation;

public class StackUnderflowError extends ScriptRuntimeError
{
	private static final long serialVersionUID = 1L;
	public StackUnderflowError( SourceLocation sLoc ) {
		super("Stack underflow", sLoc);
	}
}