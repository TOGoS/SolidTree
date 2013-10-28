package togos.solidtree.forth;

import junit.framework.TestCase;
import togos.lang.BaseSourceLocation;

public class InterpreterTest extends TestCase
{
	Interpreter interp;
	Tokenizer tokenizer;
	
	protected void eval( String s ) throws Exception {
		tokenizer.handle(s);
		tokenizer.end();
	}
	
	@Override public void setUp() {
		interp = new Interpreter();
		tokenizer = new Tokenizer( BaseSourceLocation.NONE, 4, interp.delegatingTokenHandler );
	}
	
	public void testLiteralIntegers() throws Exception {
		eval( "1 -2 +3" );
		assertEquals( 3, interp.stack.size() );
		assertEquals( Long.valueOf( 1), interp.stack.get(0) );
		assertEquals( Long.valueOf(-2), interp.stack.get(1) );
		assertEquals( Long.valueOf( 3), interp.stack.get(2) );
	}
	
	public void testLiteralFloats() throws Exception {
		eval( "1.25 -2.25 +3.25" );
		assertEquals( 3, interp.stack.size() );
		assertEquals( Double.valueOf( 1.25), interp.stack.get(0) );
		assertEquals( Double.valueOf(-2.25), interp.stack.get(1) );
		assertEquals( Double.valueOf( 3.25), interp.stack.get(2) );
	}
	
	public void testLiteralStrings() throws Exception {
		eval( "\"chunky\" \"\"" );
		assertEquals( 2, interp.stack.size() );
		assertEquals( "chunky", interp.stack.get(0) );
		assertEquals( "", interp.stack.get(1) );
	}
}
