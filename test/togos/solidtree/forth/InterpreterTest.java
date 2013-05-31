package togos.solidtree.forth;

import junit.framework.TestCase;
import togos.lang.BaseSourceLocation;

public class InterpreterTest extends TestCase
{
	Interpreter interp;
	Tokenizer tokenizer;
	
	@Override public void setUp() {
		interp = new Interpreter();
		tokenizer = new Tokenizer( BaseSourceLocation.NONE, 4, interp.delegatingTokenHandler );
	}
	
	public void testPutSomeValuesOnTheStack() throws Exception {
		tokenizer.handle( "1 -2 +3 \"chunky\"" );
		assertEquals( 4, interp.stack.size() );
		assertEquals( Integer.valueOf( 1), interp.stack.get(0) );
		assertEquals( Integer.valueOf(-2), interp.stack.get(1) );
		assertEquals( Integer.valueOf( 3), interp.stack.get(2) );
		assertEquals( "chunky"           , interp.stack.get(3) );
	}
}
