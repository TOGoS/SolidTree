package togos.lang;

public class ParseError extends ScriptError
{
	private static final long serialVersionUID = 1L;

	public ParseError( String message, SourceLocation sloc ) {
		super(message, sloc);
	}
}
