package togos.solidtree.trace;

import java.util.Random;

import togos.solidtree.Material;
import togos.solidtree.SolidNode;

public class Tracer
{
	static final class Cursor {
		SolidNode node;
		double x0, y0, z0, x1, y1, z1;
		public boolean contains( double x, double y, double z ) {
			return
				x >= x0 && x <= x1 &&
				y >= y0 && y <= y1 &&
				z >= z0 && z <= z1;
		}
		
		public void set(
			SolidNode node,
			double x0, double y0, double z0,
			double x1, double y1, double z1
		) {
			assert node != null;
			this.node = node;
			this.x0 = x0; this.y0 = y0; this.z0 = z0;
			this.x1 = x1; this.y1 = y1; this.z1 = z1;
		}
		
		public void set( SolidNode node, double min, double max ) {
			set( node, min, min, min, max, max, max );
		}
	}
	
	private SolidNode rootNode = SolidNode.EMPTY;
	private double rootScale; // At 1, rootNode's edges are 0.5 m from the origin
	
	Random random = new Random();
	
	// Current position
	private final Cursor[] cursors = new Cursor[128]; // Should be enough for anybody!
	private int cursorIdx = 1;
	private double x, y, z; // Ray origin
	
	public Tracer() {
		for( int i=0; i<cursors.length; ++i ) cursors[i] = new Cursor();
		cursors[0].set( SolidNode.EMPTY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY );
		setRoot( SolidNode.EMPTY, 0 );
	}
	
	public void setRoot( SolidNode n, double scale ) {
		this.rootNode = n;
		this.rootScale = scale;
		cursors[1].set( rootNode, -rootScale/2, rootScale/2 );
	}
	
	protected Cursor fixCursor() {
		if( cursorIdx == 0 ) cursorIdx = 1;
		
		while( !cursors[cursorIdx].contains(x,y,z) ) {
			// Back out until we fit
			--cursorIdx;
		}
		
		treeTraversal: while( cursors[cursorIdx].node.divX != 0 ) {
			Cursor c = cursors[cursorIdx];
			SolidNode n = c.node;
			
			double sw = (c.x1 - c.x0) / n.divX;
			double sh = (c.y1 - c.y0) / n.divY;
			double sd = (c.z1 - c.z0) / n.divZ;
			
			for( int sz=0; sz<n.divZ; ++sz ) {
				if( z < c.z0+sd*sz || z > c.z0+sd*(sz+1) ) continue;
				for( int sy=0; sy<n.divY; ++sy ) {
					if( y < c.y0+sh*sy || y > c.y0+sh*(sy+1) ) continue;
					for( int sx=0; sx<n.divX; ++sx ) {
						if( x < c.x0+sw*sx || x > c.x0+sw*(sx+1) ) continue;
						
						cursors[++cursorIdx].set(
							n.subNode(sx,sy,sz),
							c.x0+sw*sx, c.y0+sh*sy, c.z0+sd*sz,
							c.x0+sw*(sx+1), c.y0+sh*(sy+1), c.z0+sd*(sz+1)
						);

						continue treeTraversal;
					}
				}
			}
			
			throw new RuntimeException("Sub-node not found in node!  Argh!");
		}
		
		return cursors[cursorIdx];
	}
	
	protected void setPosition( double x, double y, double z ) {
		this.x = x;
		this.y = y;
		this.z = z;
		fixCursor();
	}
	
	/**
	 * Reposition the cursor to the point of next intersection
	 */
	protected void findNextIntersection( double dx, double dy, double dz ) {
		if( cursorIdx == 0 ) {
			if( x*dx >= 0 && y*dy >= 0 && z*dz >= 0 ) {
				// Heading out; next intersection = infinity!
				return;
			} else {
				throw new UnsupportedOperationException("Tracing rays from outside-in is not supported! ("+x+","+y+","+z+") ("+dx+","+dy+","+dz+")");
			}
		}
		
		// Shrink until it fits...
		Cursor c = cursors[cursorIdx];
		
		assert dx != 0 || dy != 0 || dz != 0;
		assert c.contains(x, y, z);
		
		while( c.contains(x+dx, y+dy, z+dz) ) {
			dx *= 2;
			dy *= 2;
			dz *= 2;
		}
		while( !c.contains(x+dx, y+dy, z+dz) ) {
			dx /= 2;
			dy /= 2;
			dz /= 2;
			
			assert dx != 0 || dy != 0 || dz != 0;
		}
		
		double ddx = dx/2, ddy = dy/2, ddz = dz/2;
		for( int i=0; i<10; ++i ) {
			if( c.contains(x+dx, y+dy, z+dz) ) {
				dx += ddx;
				dy += ddy;
				dz += ddz;
			} else {
				dx -= ddx;
				dy -= ddy;
				dz -= ddz;
			}
			ddx /= 2; ddy /= 2; ddz /= 2;
		}
		// Make sure it's just past the boundary
		while( c.contains(x+dx, y+dy, z+dz) ) {
			dx += ddx;
			dy += ddy;
			dz += ddz;
		}
		
		setPosition( x+dx, y+dy, z+dz );
	}
	
	double nx, ny, nz; // Normal vector
	private double[] distances = new double[6];
	private static double[] normals = {
		-1,  0,  0,
		+1,  0,  0,
		 0, -1,  0,
		 0, +1,  0,
		 0,  0, -1,
		 0,  0, +1,
	};
	protected void calculateNormal() {
		Cursor c = cursors[cursorIdx];
		
		distances[0] = x - c.x0;
		distances[1] = c.x1 - x;
		distances[2] = y - c.y0;
		distances[3] = c.y1 - y;
		distances[4] = z - c.z0;
		distances[5] = c.z1 - z;
		
		double max = 0;
		for( int i=0; i<6; ++i ) {
			if( distances[i] > max ) {
				nx = normals[i*3+0];
				ny = normals[i*3+1];
				nz = normals[i*3+2];
				max = distances[i];
			}
		}
	}
	
	double red, green, blue;
	public void trace( double x, double y, double z, double dx, double dy, double dz ) {
		setPosition( x, y, z );
		Material material = cursors[cursorIdx].node.material; 
		
		red = green = blue = 1.0;
		while( material.opacity == 0 ) {
			findNextIntersection( dx, dy, dz );
			material = cursors[cursorIdx].node.material;
			if( cursorIdx == 0 ) break;
		}
		calculateNormal();
		
		Material m = cursors[cursorIdx].node.material;
		red   = m.ambientColor.r * (0.5 + nx*0.25);
		green = m.ambientColor.g * (0.5 + ny*0.25);
		blue  = m.ambientColor.b * (0.5 + nz*0.25);
	}
}
