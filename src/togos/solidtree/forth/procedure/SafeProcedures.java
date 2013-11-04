package togos.solidtree.forth.procedure;

import java.util.Map;

import togos.solidtree.forth.WordDefinition;

/**
 * All procedures that do not interact with the user's system
 */
public class SafeProcedures
{
	public static void register( Map<String, ? super WordDefinition> ctx ) {
		StackProcedures.register(ctx);
		ArithmeticProcedures.register(ctx);
		DefinitionProcedures.register(ctx);
		NodeProcedures.register(ctx);
	}
}
