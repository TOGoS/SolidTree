package togos.solidtree.forth;

import togos.lang.SourceLocation;

public abstract class StandardWordDefinition implements WordDefinition
{
	@Override public void compile( Interpreter interp, SourceLocation sLoc ) {
		interp.addToInstructionList(this, sLoc);
	}
}
