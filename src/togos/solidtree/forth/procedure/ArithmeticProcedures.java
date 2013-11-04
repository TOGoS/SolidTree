package togos.solidtree.forth.procedure;

import java.util.Map;

import togos.lang.ScriptError;
import togos.lang.SourceLocation;
import togos.solidtree.forth.Interpreter;
import togos.solidtree.forth.StandardWordDefinition;
import togos.solidtree.forth.WordDefinition;

public class ArithmeticProcedures
{
	public static StandardWordDefinition ADD = new StandardWordDefinition() {
		@Override public void run( Interpreter interp, SourceLocation sLoc ) throws ScriptError {
			double b = interp.stackPop(Number.class, sLoc).doubleValue();
			double a = interp.stackPop(Number.class, sLoc).doubleValue();
			interp.stackPush( a + b );
		}
	};
	public static StandardWordDefinition SUBTRACT = new StandardWordDefinition() {
		@Override public void run( Interpreter interp, SourceLocation sLoc ) throws ScriptError {
			double b = interp.stackPop(Number.class, sLoc).doubleValue();
			double a = interp.stackPop(Number.class, sLoc).doubleValue();
			interp.stackPush( a - b );
		}
	};
	public static StandardWordDefinition MULTIPLY = new StandardWordDefinition() {
		@Override public void run( Interpreter interp, SourceLocation sLoc ) throws ScriptError {
			double b = interp.stackPop(Number.class, sLoc).doubleValue();
			double a = interp.stackPop(Number.class, sLoc).doubleValue();
			interp.stackPush( a * b );
		}
	};
	public static StandardWordDefinition DIVIDE = new StandardWordDefinition() {
		@Override public void run( Interpreter interp, SourceLocation sLoc ) throws ScriptError {
			double b = interp.stackPop(Number.class, sLoc).doubleValue();
			double a = interp.stackPop(Number.class, sLoc).doubleValue();
			interp.stackPush( a / b );
		}
	};
	public static StandardWordDefinition EXPONENTIATE = new StandardWordDefinition() {
		@Override public void run( Interpreter interp, SourceLocation sLoc ) throws ScriptError {
			double b = interp.stackPop(Number.class, sLoc).doubleValue();
			double a = interp.stackPop(Number.class, sLoc).doubleValue();
			interp.stackPush( Math.pow(a, b) );
		}
	};
	
	public static void register( Map<String,? super WordDefinition> ctx ) {
		ctx.put("+", ADD);
		ctx.put("-", SUBTRACT);
		ctx.put("*", MULTIPLY);
		ctx.put("/", DIVIDE);
		ctx.put("**", EXPONENTIATE);
	}
}
