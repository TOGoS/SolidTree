package togos.solidtree.shape;

public interface Shape
{
	enum Containment {
		NONE, SOME, ALL
	}
	
	public Containment contains( double minX, double minY, double minZ, double maxX, double maxY, double maxZ );
}
