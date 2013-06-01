package togos.solidtree.forth.procedure;

import togos.lang.SourceLocation;
import togos.solidtree.forth.Interpreter;
import togos.solidtree.forth.StandardWordDefinition;

public class ConstantValue extends StandardWordDefinition
{
	final Object value;
	public ConstantValue( Object v ) {
		this.value = v;
	}
	
	@Override public void run( Interpreter interp, SourceLocation sLoc ) {
		interp.stackPush(value);
    }
}
