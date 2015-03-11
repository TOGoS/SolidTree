package togos.solidtree;

import java.util.concurrent.ExecutionException;

import togos.lazy.HardHandle;
import togos.lazy.Ref;
import togos.lazy.SoftHandle;

public class NodeDereffer
{
	public <A> A deref( Ref<A> ref, Class<A> targetClass ) throws DereferenceException {
		if( ref instanceof HardHandle ) {
			return ((HardHandle<A>)ref).get();
		} else if( ref instanceof SoftHandle ) {
			try {
				return ((SoftHandle<A>)ref).get();
			} catch( ExecutionException e ) {
				throw new DereferenceException(e);
			}
		}
		return null;
	}
}
