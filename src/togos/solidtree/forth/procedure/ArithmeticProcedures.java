package togos.solidtree.forth.procedure;

import java.util.Map;

import togos.lang.ScriptError;
import togos.lang.SourceLocation;
import togos.solidtree.forth.Interpreter;
import togos.solidtree.forth.StandardWordDefinition;
import togos.solidtree.forth.WordDefinition;

public class ArithmeticProcedures
{
	protected static boolean isInteger( Number a ) {
		return a instanceof Byte || a instanceof Short || a instanceof Integer || a instanceof Long;
	}
	protected static boolean useIntegerMath( Number a, Number b ) {
		return isInteger(a) && isInteger(b);
	}
	
	protected static Number add( Number a, Number b ) {
		if( useIntegerMath(a, b) ) {
			return Long.valueOf(a.longValue() + b.longValue());
		} else {
			return Double.valueOf(a.doubleValue() + b.doubleValue());
		}
	}
	
	public static StandardWordDefinition ADD = new StandardWordDefinition() {
		@Override public void run( Interpreter interp, SourceLocation sLoc ) throws ScriptError {
			interp.stackPush(
				add( interp.stackPop( Number.class, sLoc ), interp.stackPop( Number.class, sLoc ) )
			);
		}
	};
	
	public static void register( Map<String,? super WordDefinition> ctx ) {
		ctx.put("+", ADD);
	}
}
