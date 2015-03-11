package togos.solidtree.shape;

import togos.lazy.HardHandle;
import togos.lazy.Ref;
import togos.solidtree.DereferenceException;
import togos.solidtree.HomogeneousSolidNode;
import togos.solidtree.NodeDereffer;
import togos.solidtree.RegularlySubdividedSolidNode;
import togos.solidtree.SolidNode;
import togos.solidtree.SolidNode.Type;
import togos.solidtree.shape.Shape.Containment;

public class NodeShaper
{
	public int divX = 3, divY = 3, divZ = 3;
	
	protected NodeDereffer nodeDereffer;
	
	public Ref<SolidNode> add( Ref<SolidNode> originalRef, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, Shape s, Ref<SolidNode> leafRef, int maxRecursion )
		throws DereferenceException
	{
		switch( s.contains(minX, minY, minZ, maxX, maxY, maxZ) ) {
		case NONE: return originalRef;
		case ALL: return leafRef;
		case SOME:
			if( maxRecursion == 0 ) return originalRef;
			
			SolidNode original = nodeDereffer.deref(originalRef, SolidNode.class);
			
			int divCount = divX*divY*divZ; 
			@SuppressWarnings("unchecked")
			Ref<SolidNode>[] newKids = new Ref[divCount];
			if( original.getType() == Type.REGULARLY_SUBDIVIDED ) {
				if( original.getDivX() != divX || original.getDivY() != divY || original.getDivZ() != divZ ) {
					throw new RuntimeException(
						"Can't subdivide node; already subdivided with different divisions!"
					);
				}
				for(int i=divCount-1; i>=0; --i ) {
					newKids[i] = original.subNode(i);
				}
			} else {
				for(int i=divCount-1; i>=0; --i ) {
					newKids[i] = originalRef;
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
							newKids[i] = add( newKids[i], sMinX, sMinY, sMinZ, sMaxX, sMaxY, sMaxZ, s, leafRef, maxRecursion - 1 );
						}
					}
				}
			}
			
			return anythingChanged ? new HardHandle<SolidNode>(RegularlySubdividedSolidNode.build(
				divX, divY, divZ, newKids
			)) : originalRef;
		default:
			throw new RuntimeException("Invalid containment");
		}
	}
	
	////
	
	public Ref<SolidNode> rootRef = new HardHandle<SolidNode>(HomogeneousSolidNode.EMPTY);
	public double rootMinX = -1, rootMinY = -1, rootMinZ = -1, rootMaxX = 1, rootMaxY = 1, rootMaxZ = 1;
	
	public void setRoot( Ref<SolidNode> r, double minX, double minY, double minZ, double maxX, double maxY, double maxZ ) {
		rootRef = r;
		rootMinX = minX; rootMaxX = maxX;
		rootMinY = minY; rootMaxY = maxY;
		rootMinZ = minZ; rootMaxZ = maxZ;
	}
	
	public void setRoot( Ref<SolidNode> r, double radius ) {
		setRoot( r, -radius, -radius, -radius, radius, radius, radius );
	}
	
	public void add( Shape s, Ref<SolidNode> leafRef, int maxRecursion ) throws DereferenceException {
		rootRef = add( rootRef, rootMinX, rootMinY, rootMinZ, rootMaxX, rootMaxY, rootMaxZ, s, leafRef, maxRecursion );
	}
}
