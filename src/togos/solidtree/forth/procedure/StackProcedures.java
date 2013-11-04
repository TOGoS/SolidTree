package togos.solidtree.forth.procedure;

import java.util.Map;

import togos.lang.ScriptError;
import togos.lang.SourceLocation;
import togos.solidtree.forth.Interpreter;
import togos.solidtree.forth.StandardWordDefinition;
import togos.solidtree.forth.WordDefinition;

public class StackProcedures
{
	public static void register( Map<String,? super WordDefinition> ctx ) {
		ctx.put("dup", new StandardWordDefinition() {
			@Override
            public void run( Interpreter interp, SourceLocation sLoc ) throws ScriptError {
				Object o = interp.stackPop(Object.class, sLoc);
				interp.stackPush(o);
				interp.stackPush(o);
            }
		});
		ctx.put("swap", new StandardWordDefinition() {
			@Override
            public void run( Interpreter interp, SourceLocation sLoc ) throws ScriptError {
				Object a = interp.stackPop(Object.class, sLoc);
				Object b = interp.stackPop(Object.class, sLoc);
				interp.stackPush(a);
				interp.stackPush(b);
            }
		});
	}
}
