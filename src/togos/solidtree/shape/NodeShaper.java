package togos.solidtree.shape;

import togos.solidtree.SolidNode;
import togos.solidtree.shape.Shape.Containment;

public class NodeShaper
{
	int divX = 3, divY = 3, divZ = 3;
	
	public SolidNode add( SolidNode original, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, Shape s, SolidNode leaf, int maxRecursion ) {
		switch( s.contains(minX, minY, minZ, maxX, maxY, maxZ) ) {
		case NONE: return original;
		case ALL: return leaf;
		case SOME:
			if( maxRecursion == 0 ) return original;
			
			int divCount = divX*divY*divZ; 
			SolidNode[] newKids = new SolidNode[divCount];
			if( original.isSubdivided() ) {
				if( original.divX != divX || original.divY != divY || original.divZ != divZ ) {
					throw new RuntimeException(
						"Can't subdivide node; already subdivided with different divisions!"
					);
				}
				for(int i=divCount-1; i>=0; --i ) {
					newKids[i] = original.subNode(i);
				}
			} else {
				for(int i=divCount-1; i>=0; --i ) {
					newKids[i] = original;
				}
			}
			
			double ssX = (maxX-minX)/divX;
			double ssY = (maxY-minY)/divY;
			double ssZ = (maxZ-minZ)/divZ;
			
			boolean anythingChanged = false;
			for( int i=0, z=0; z<divZ; ++z ) {
				double sMinZ = minZ + ssZ*z;
				double sMaxZ = minZ + ssZ*(z+1);
				for( int y=0; y<divZ; ++y ) {
					double sMinY = minY + ssY*y;
					double sMaxY = minY + ssY*(y+1);
					for( int x=0; x<divX; ++x, ++i ) {
						double sMinX = minX + ssX*x;
						double sMaxX = minX + ssX*(x+1);
						if( s.contains(sMinX, sMinY, sMinZ, sMaxX, sMaxY, sMaxZ) != Containment.NONE ) {
							anythingChanged = true;
							newKids[i] = add( newKids[i], sMinX, sMinY, sMinZ, sMaxX, sMaxY, sMaxZ, s, leaf, maxRecursion - 1 );
						}
					}
				}
			}
			
			return anythingChanged ? new SolidNode(
				original.material, divX, divY, divZ, newKids
			) : original;
		default:
			throw new RuntimeException("Invalid containment");
		}
	}
	
	////
	
	public SolidNode root;
	public double rootMinX, rootMinY, rootMinZ, rootMaxX, rootMaxY, rootMaxZ;
	
	public void setRoot( SolidNode r, double minX, double minY, double minZ, double maxX, double maxY, double maxZ ) {
		root = r;
		rootMinX = minX; rootMaxX = maxX;
		rootMinY = minY; rootMaxY = maxY;
		rootMinZ = minZ; rootMaxZ = maxZ;
	}
	
	public void setRoot( SolidNode r, double radius ) {
		setRoot( r, -radius, -radius, -radius, radius, radius, radius );
	}
	
	public void add( Shape s, SolidNode leaf, int maxRecursion ) {
		root = add( root, rootMinX, rootMinY, rootMinZ, rootMaxX, rootMaxY, rootMaxZ, s, leaf, maxRecursion );
	}
}
