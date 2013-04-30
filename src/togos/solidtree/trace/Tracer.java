package togos.solidtree.trace;

import java.util.Random;

import togos.solidtree.Material;
import togos.solidtree.SimplexNoise;
import togos.solidtree.SolidNode;

public class Tracer
{
	static final class Cursor {
		SolidNode node;
		double x0, y0, z0, x1, y1, z1;
		
		public boolean contains( Vector3D pos ) {
			return contains( pos.x, pos.y, pos.z );
		}
		
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
	private final Vector3D pos = new Vector3D();
	
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
		
		while( !cursors[cursorIdx].contains(pos) ) {
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
				if( pos.z < c.z0+sd*sz || pos.z > c.z0+sd*(sz+1) ) continue;
				for( int sy=0; sy<n.divY; ++sy ) {
					if( pos.y < c.y0+sh*sy || pos.y > c.y0+sh*(sy+1) ) continue;
					for( int sx=0; sx<n.divX; ++sx ) {
						if( pos.x < c.x0+sw*sx || pos.x > c.x0+sw*(sx+1) ) continue;
						
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
		pos.set(x,y,z);
		fixCursor();
	}
	
	protected void setPosition( Vector3D v ) {
		pos.set(v);
		fixCursor();
	}
	
	/**
	 * Reposition the cursor to the point of next intersection
	 * @param p position
	 * @param d direction
	 */
	protected boolean findNextIntersection( Vector3D p, Vector3D d, Vector3D dest ) {
		if( cursorIdx == 0 ) {
			if( p.x*d.x >= 0 && p.y*d.y >= 0 && p.z*d.z >= 0 ) {
				// Heading out; next intersection = infinity!
				return false;
			} else {
				//throw new UnsupportedOperationException("Tracing rays from outside-in is not supported! ("+x+","+y+","+z+") ("+dx+","+dy+","+dz+")");
				return false;
			}
		}
		
		// Shrink until it fits...
		Cursor c = cursors[cursorIdx];
		
		assert !d.isZero();
		assert c.contains(p);
		
		while( c.contains(p.x+d.x, p.y+d.y, p.z+d.z) ) {
			d.scale(2);
		}
		while( !c.contains(p.x+d.x, p.y+d.y, p.z+d.z) ) {
			d.scale((double)0.5);
			
			assert !d.isZero();
		}
		
		double ddx = d.x/2, ddy = d.y/2, ddz = d.z/2;
		for( int i=0; i<10; ++i ) {
			if( c.contains(p.x+d.x, p.y+d.y, p.z+d.z) ) {
				d.x += ddx;
				d.y += ddy;
				d.z += ddz;
			} else {
				d.x -= ddx;
				d.y -= ddy;
				d.z -= ddz;
			}
			ddx /= 2; ddy /= 2; ddz /= 2;
		}
		// Make sure it's just past the boundary
		while( c.contains(p.x+d.x, p.y+d.y, p.z+d.z) ) {
			d.x += ddx;
			d.y += ddy;
			d.z += ddz;
		}
		
		dest.set( p.x+d.x, p.y+d.y, p.z+d.z );
		return true;
	}
	
	final Vector3D normal = new Vector3D(); 
	private double[] distances = new double[6];
	private static double[] normals = {
		-1,  0,  0,
		+1,  0,  0,
		 0, -1,  0,
		 0, +1,  0,
		 0,  0, -1,
		 0,  0, +1,
	};
	
	/**
	 * Sets the normal vector based on which side of
	 * the containing node position is closest to.
	 */
	protected void calculateNormal() {
		Cursor c = cursors[cursorIdx];
		
		distances[0] = pos.x - c.x0;
		distances[1] = c.x1 - pos.x;
		distances[2] = pos.y - c.y0;
		distances[3] = c.y1 - pos.y;
		distances[4] = pos.z - c.z0;
		distances[5] = c.z1 - pos.z;
		
		double min = Double.POSITIVE_INFINITY;
		for( int i=0; i<6; ++i ) {
			assert distances[i] > 0;
			if( distances[i] < min ) {
				normal.set(
					normals[i*3+0],
					normals[i*3+1],
					normals[i*3+2]
				);
				min = distances[i];
			}
		}
		
		// Fake some waviness for now
		normal.x += (double)sn.apply((float)pos.x/2, (float)pos.y/3, (float)pos.z/1)*0.1;
		normal.y += (double)sn.apply((float)pos.x/3, (float)pos.y/2, (float)pos.z/2)*0.1;
		normal.z += (double)sn.apply((float)pos.x/1, (float)pos.y/1, (float)pos.z/3)*0.1;
	}
	
	SimplexNoise sn = new SimplexNoise();
	final Vector3D newPos = new Vector3D();
	final Vector3D direction = new Vector3D();
	private double filterRed, filterGreen, filterBlue;
	double red, green, blue;
	
	protected void setDirection( double x, double y, double z ) {
		direction.set(x,y,z);
		fixCursor();
	}
	
	public void trace( double x, double y, double z, double dx, double dy, double dz ) {
		setPosition( x, y, z );
		setDirection( dx, dy, dz );
		
		filterRed = filterGreen = filterBlue = (double)1.0;
		red = green = blue = (double)0.0;
		
		process: for( int i=0; i<24; ++i ) {
			Material material = cursors[cursorIdx].node.material;
			if( !findNextIntersection( pos, direction, newPos ) ) {
				return;
			} else if( material.scattering > 0 && false ) {
				// Chance of not scattering in 1 meter = (1-S)
				// Chance of not scattering in 2 meter = (1-S)**2 
				double dist = VectorMath.dist( pos, newPos );
				double transmission = (double)Math.pow( 1-material.scattering, dist );
				if( random.nextDouble() > transmission ) {
					// Scatter!
					// Let's say for now it's at some random point (which is terribly wrong).
					dist *= random.nextDouble();
					direction.normalize(dist);
					VectorMath.add( pos, direction, pos );
					fixCursor();
					
					direction.x = random.nextDouble()-(double)0.5;
					direction.y = random.nextDouble()-(double)0.5;
					direction.z = random.nextDouble()-(double)0.5;
					
					filterRed   *= material.filterColor.r;
					filterGreen *= material.filterColor.g;
					filterBlue  *= material.filterColor.b;
					
					red   += material.ambientColor.r * filterRed;
					green += material.ambientColor.g * filterGreen;
					blue  += material.ambientColor.b * filterBlue;
					
					continue process;
				}
			}
			
			// Otherwise it just passes through!
			
			setPosition(newPos);
			material = cursors[cursorIdx].node.material;
			
			calculateNormal();
			
			if( random.nextDouble() < material.mirrosity ) {
				VectorMath.reflect(direction, normal, direction);
			} else if( material.scattering == 1 ) {
				direction.x = normal.x + (double)(random.nextGaussian()*0.5);
				direction.y = normal.y + (double)(random.nextGaussian()*0.5);
				direction.z = normal.z + (double)(random.nextGaussian()*0.5);
				
				filterRed   *= material.filterColor.r;
				filterGreen *= material.filterColor.g;
				filterBlue  *= material.filterColor.b;
				
				red   += material.ambientColor.r * filterRed;
				green += material.ambientColor.g * filterGreen;
				blue  += material.ambientColor.b * filterBlue;
			}
			// Special case for opaque scattering?
		}
		
		/*
		Material m = cursors[cursorIdx].node.material;
		
		double adjust = normal.x*0.1 + normal.y*0.2 + normal.z*0.3;
		
		red   = m.ambientColor.r * (0.5 + adjust);
		green = m.ambientColor.g * (0.5 + adjust);
		blue  = m.ambientColor.b * (0.5 + adjust);
		*/
	}
}
