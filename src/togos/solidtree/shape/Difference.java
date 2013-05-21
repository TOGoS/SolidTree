package togos.solidtree.shape;

public class Difference implements Shape
{
	final Shape minuend, subtrahend;
	
	public Difference( Shape minuend, Shape subtrahend ) {
		this.minuend = minuend;
		this.subtrahend = subtrahend;
	}

	@Override
	public Containment contains(
		double minX, double minY, double minZ,
		double maxX, double maxY, double maxZ
	) {
		Containment mc = minuend.contains(minX, minY, minZ, maxX, maxY, maxZ); 
		if( mc == Containment.NONE ) return Containment.NONE;
		
		Containment sc = subtrahend.contains(minX, minY, minZ, maxX, maxY, maxZ);
		if( sc == Containment.ALL  ) return Containment.NONE;
		if( sc == Containment.NONE ) return mc;
		
		return Containment.SOME;
	}
}
