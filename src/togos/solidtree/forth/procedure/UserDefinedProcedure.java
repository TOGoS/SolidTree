package togos.solidtree.forth.procedure;

import java.util.List;

import togos.lang.ScriptError;
import togos.lang.SourceLocation;
import togos.solidtree.forth.Interpreter;
import togos.solidtree.forth.Procedure;
import togos.solidtree.forth.StandardWordDefinition;

public class UserDefinedProcedure extends StandardWordDefinition
{
	final List<Procedure> procs;
	public UserDefinedProcedure( List<Procedure> procs ) {
		this.procs = procs;
	}

	@Override public void run( Interpreter interp, SourceLocation sLoc ) throws ScriptError {
		for( Procedure p : procs ) p.run(interp, sLoc);
	}
}
