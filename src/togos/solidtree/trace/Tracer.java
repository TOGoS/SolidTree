package togos.solidtree.trace;

import java.util.Random;

import togos.solidtree.DColor;
import togos.solidtree.NodeRoot;
import togos.solidtree.PathTraceMaterial;
import togos.solidtree.SimplexNoise;
import togos.solidtree.SurfaceMaterial;
import togos.solidtree.SurfaceMaterialLayer;
import togos.solidtree.matrix.Vector3D;
import togos.solidtree.matrix.VectorMath;
import togos.solidtree.trace.sky.CrappySkySphere;
import togos.solidtree.trace.sky.SkySphere;

public class Tracer
{
	static final class Cursor {
		TraceNode node;
		double x0, y0, z0, x1, y1, z1;
		boolean definite;
		
		public boolean mayContain( Vector3D pos ) {
			return mayContain( pos.x, pos.y, pos.z );
		}
		
		public boolean mayContain( double x, double y, double z ) {
			return
				x >= x0 && x <= x1 &&
				y >= y0 && y <= y1 &&
				z >= z0 && z <= z1;
		}
		
		public boolean definitelyContains( double x, double y, double z ) {
			return
				definite &&
				x >= x0 && x <= x1 &&
				y >= y0 && y <= y1 &&
				z >= z0 && z <= z1;
		}
		
		public boolean onEdge( double x, double y, double z ) {
			return mayContain(x,y,z) && (
				x == x0 || x == x1 ||
				y == y0 || y == y1 ||
				z == z0 || z == z1
			);
		}
		
		public void set(
			TraceNode node,
			double x0, double y0, double z0,
			double x1, double y1, double z1,
			boolean definite
		) {
			assert node != null;
			this.node = node;
			this.x0 = x0; this.y0 = y0; this.z0 = z0;
			this.x1 = x1; this.y1 = y1; this.z1 = z1;
			this.definite = definite;
		}
	}
	
	public enum Mode {
		FULL, QUICK
	}
	
	public Mode mode = Mode.FULL;
	
	Random random = new Random();
	
	// Current position
	private final Cursor[] cursors = new Cursor[128]; // Should be enough for anybody!
	private int cursorIdx = 1;
	private final Vector3D pos = new Vector3D();
	
	public Tracer() {
		for( int i=0; i<cursors.length; ++i ) cursors[i] = new Cursor();
		cursors[0].set( TraceNode.EMPTY,
			Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY,
			Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
			true
		);
		setRoot( TraceNode.EMPTY, 0, 0, 0, 0, 0, 0 );
	}
	
	public void setRoot( TraceNode root, double minX, double minY, double minZ, double maxX, double maxY, double maxZ ) {
		cursors[1].set( root, minX, minY, minZ, maxX, maxY, maxZ, true );
	}
	
	public void setRoot( NodeRoot<TraceNode> root ) {
		setRoot( root.node, root.x0, root.y0, root.z0, root.x1, root.y1, root.z1 );
	}
	
	public void setScene( Scene scene ) {
		setRoot( scene.nodeRoot );
		this.skySphere = scene.sky;
	}
	
	protected static final double min( double a, double b ) {
		return a < b ? a : b; 
	}
	protected static final double max( double a, double b ) {
		return a > b ? a : b; 
	}
	protected static final int min( int a, int b ) {
		return a < b ? a : b; 
	}
	
	protected static final void subdivide( Cursor c, double x, double y, double z, Cursor dest ) {
		assert c.mayContain(x, y, z);
		
		final TraceNode n = c.node;
		double mid;
		switch( n.division ) {
		case TraceNode.DIV_X:
			mid = c.x0 + n.splitPoint * (c.x1 - c.x0);
			if( x < mid ) {
				dest.set(n.subNodeA, c.x0, c.y0, c.z0, mid, c.y1, c.z1, c.definite);
			} else {
				dest.set(n.subNodeB, mid, c.y0, c.z0, c.x1, c.y1, c.z1, c.definite);
			}
			break;
		case TraceNode.DIV_Y:
			mid = c.y0 + n.splitPoint * (c.y1 - c.y0);
			if( y < mid ) {
				dest.set(n.subNodeA, c.x0, c.y0, c.z0, c.x1, mid, c.z1, c.definite);
			} else {
				dest.set(n.subNodeB, c.x0, mid, c.z0, c.x1, c.y1, c.z1, c.definite);
			}
			break;
		case TraceNode.DIV_Z:
			mid = c.z0 + n.splitPoint * (c.z1 - c.z0);
			if( z < mid ) {
				dest.set(n.subNodeA, c.x0, c.y0, c.z0, c.x1, c.y1, mid, c.definite);
			} else {
				dest.set(n.subNodeB, c.x0, c.y0, mid, c.x1, c.y1, c.z1, c.definite);
			}
			break;
		case TraceNode.DIV_FUNC_GLOBAL:
			dest.set( n.splitFunc.apply( x, y, z ) < 0 ? n.subNodeA : n.subNodeB, c.x0, c.y0, c.z0, c.x1, c.y1, c.z1, false );
			break;
		case TraceNode.DIV_FUNC_LOCAL:
			dest.set( n.splitFunc.apply(
				(x - c.x0) / (c.x1 - c.x0),
				(y - c.y0) / (c.y1 - c.y0),
				(z - c.z0) / (c.z1 - c.z0)
			) < 0 ? n.subNodeA : n.subNodeB, c.x0, c.y0, c.z0, c.x1, c.y1, c.z1, false );
			break;
		default:
			throw new RuntimeException("Invalid TraceNode division value: "+n.division);
		}
		
		assert dest.mayContain(x, y, z);
	}
	
	protected Cursor fixCursor(double x, double y, double z) {
		if( cursorIdx == 0 ) cursorIdx = 1;
		
		while( !cursors[cursorIdx].definitelyContains(x, y, z) ) {
			// Back out until we know we're in the right place
			--cursorIdx;
		}
		
		// As long as it's subdividible, subdivide!
		while( cursors[cursorIdx].node.isSubdivided() ) {
			subdivide( cursors[cursorIdx], x, y, z, cursors[++cursorIdx] );
		}
		
		return cursors[cursorIdx];
	}
	
	protected void setPosition( double x, double y, double z ) {
		pos.set(x,y,z);
		fixCursor(x, y, z);
	}
	
	protected void setPosition( Vector3D v ) {
		setPosition(v.x, v.y, v.z);
	}
	
	protected boolean findNextIntersectionOld( Vector3D p, Vector3D d, Vector3D dest ) {
		if( cursorIdx == 0 ) {
			// Outside the tree; we'll never hit anything
			return false;
		}
		
		Cursor c = cursors[cursorIdx];
		
		assert !d.isZero();
		assert c.mayContain(p);
		
		// TODO: There is some slowness when rays go directly along an edge.
		// Figure a good way to deal with that situation.
		if( c.onEdge(p.x, p.y, p.z) ) return false;
		
		if( !c.onEdge(p.x, p.y, p.z) ) {
			// Grow the direction vector until it
			// pokes out of the current box...
			while( c.mayContain(p.x+d.x, p.y+d.y, p.z+d.z) ) {
				d.scaleInPlace(2);
			}
			// Shrink the direction vector until it fits
			// within the current box...
			while( !c.mayContain(p.x+d.x, p.y+d.y, p.z+d.z) ) {
				d.scaleInPlace((double)0.5);
				
				assert !d.isZero();
			}
		}
		assert !d.isZero();
		// Adjust the direction vector to get as close
		// to the boundary as possible...
		double ddx = d.x/2, ddy = d.y/2, ddz = d.z/2;
		for( int i=0; i<10; ++i ) {
			if( c.mayContain(p.x+d.x, p.y+d.y, p.z+d.z) ) {
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
		while( c.mayContain(p.x+d.x, p.y+d.y, p.z+d.z) ) {
			assert ddx != 0 || ddy != 0 || ddz != 0;
			d.x += ddx;
			d.y += ddy;
			d.z += ddz;
		}
		
		dest.set( p.x+d.x, p.y+d.y, p.z+d.z );
		
		calculateNormal();
		
		return true;
	}
	
	protected static final double SMALL_VALUE = Math.pow(2, -10);
	
	Vector3D scaledDirection = new Vector3D();
	protected boolean findNextIntersectionNew( Vector3D p, Vector3D d, Vector3D justInsideDest, Vector3D justOutsideDest ) {
		if( cursorIdx == 0 ) {
			// Outside the tree; we'll never hit anything
			return false;
		}
		
		Cursor c = cursors[cursorIdx];
		
		// Not important to the calculation, but
		// makes d a non-ridiculously-small value,
		// which helps prevent underflows
		d.manhattanNormalizeInPlace(1);
		
		assert !d.isZero();
		assert c.mayContain(p);
		
		double scale = Double.POSITIVE_INFINITY;
		int side = -1;
		// Shrink or grow the vector to fit just within the current cube
		if( d.x != 0 ) {
			double xDist = d.x < 0 ? c.x0 - p.x : c.x1 - p.x;
			double xScale = xDist / d.x;
			if( xScale < scale ) {
				scale = xScale;
				side = d.x > 0 ? SIDE_POS_X : SIDE_NEG_X;
			} 
		}
		if( d.y != 0 ) {
			double yDist = d.y < 0 ? c.y0 - p.y : c.y1 - p.y;
			double yScale = yDist / d.y;
			if( yScale < scale ) {
				scale = yScale;
				side = d.y > 0 ? SIDE_POS_Y : SIDE_NEG_Y;				
			}
		}
		if( d.z != 0 ) {
			double zDist = d.z < 0 ? c.z0 - p.z : c.z1 - p.z;
			double zScale = zDist / d.z;
			if( zScale < scale ) {
				scale = zScale;
				side = d.z > 0 ? SIDE_POS_Z : SIDE_NEG_Z;				
			}
		}
		
		setNormalForSide( side );		
		
		assert !Double.isInfinite(scale);
		
		// To put it just past the edge:
		double outsideScale = scale == 0 ?  SMALL_VALUE : scale * (1+SMALL_VALUE);
		double insideScale  = scale == 0 ? -SMALL_VALUE : scale * (1-SMALL_VALUE);
		
		assert !d.isZero();
		if( scale == 0 ) return false; // It happens sometimes
		assert scale != 0;
		
		VectorMath.scale( d, outsideScale, scaledDirection );
		justOutsideDest.set( p.x+scaledDirection.x, p.y+scaledDirection.y, p.z+scaledDirection.z );
		VectorMath.scale( d, insideScale, scaledDirection );
		justInsideDest.set( p.x+scaledDirection.x, p.y+scaledDirection.y, p.z+scaledDirection.z );
		
		return true;
	}
	
	/**
	 * Find a point just past the next intersection and place in dest.
	 * Also populates normal vector.
	 * @param p position
	 * @param d direction
	 */
	protected boolean findNextIntersection( Vector3D p, Vector3D d, Vector3D justInsideDest, Vector3D justOutsideDest ) {
		return findNextIntersectionNew( p, d, justInsideDest, justOutsideDest );
	}
	
	public static final int SIDE_POS_X = 0;
	public static final int SIDE_NEG_X = 1;
	public static final int SIDE_POS_Y = 2;
	public static final int SIDE_NEG_Y = 3;
	public static final int SIDE_POS_Z = 4;
	public static final int SIDE_NEG_Z = 5;
	
	// Note that normals are opposite sides (side +x has normal -x)
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
	
	protected void setNormalForSide( int side ) {
		normal.set(
			normals[side*3+0],
			normals[side*3+1],
			normals[side*3+2]
		);
	}
	
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
		int side = -1;
		for( int i=0; i<6; ++i ) {
			assert distances[i] > 0;
			if( distances[i] < min ) {
				side = i;
				min = distances[i];
			}
		}
		
		assert side != -1;
		
		setNormalForSide(side);
		
		/*
		// Fake some waviness for now
		normal.x += (double)sn.apply((float)pos.x/2, (float)pos.y/3, (float)pos.z/1)*0.5;
		normal.y += (double)sn.apply((float)pos.x/3, (float)pos.y/2, (float)pos.z/2)*0.5;
		normal.z += (double)sn.apply((float)pos.x/1, (float)pos.y/1, (float)pos.z/3)*0.5;
		*/
	}
	
	SkySphere skySphere = new CrappySkySphere();
	
	SimplexNoise sn = new SimplexNoise();
	final Vector3D preIntersect = new Vector3D();
	final Vector3D postIntersect = new Vector3D();
	final Vector3D direction = new Vector3D();
	private double filterRed, filterGreen, filterBlue;
	public double red, green, blue;
	
	protected void setDirection( double x, double y, double z ) {
		direction.set(x,y,z);
	}
	
	private final Vector3D zeroVector = new Vector3D();
	
	private final Vector3D mirrorVector = new Vector3D();
	protected Vector3D mirrorVector( Vector3D incoming, Vector3D normal, double factor ) {
		if( factor == 0 ) return zeroVector;
		
		VectorMath.reflect( incoming, normal, mirrorVector );
		mirrorVector.normalizeInPlace(factor);
		return mirrorVector;
	}
	
	private final Vector3D randomVector = new Vector3D();
	protected Vector3D randomVector( double factor ) {
		if( factor == 0 ) return zeroVector;
		
		do {
			randomVector.x = random.nextGaussian();
			randomVector.y = random.nextGaussian();
			randomVector.z = random.nextGaussian();
		} while( randomVector.isZero() );
		randomVector.normalizeInPlace(factor);
		return randomVector;
	}
	
	private void applyMaterialColor( SurfaceMaterialLayer material ) {
		red   += filterRed   * material.emissionColor.r;
		green += filterGreen * material.emissionColor.g;
		blue  += filterBlue  * material.emissionColor.b;
		
		filterRed   *= material.filterColor.r;
		filterGreen *= material.filterColor.g;
		filterBlue  *= material.filterColor.b;
    }
	
	private final Vector3D normalTerm = new Vector3D();
	/**
	 * Redirect ray and apply material color
	 * according to the given normal and material properties.
	 */
	protected boolean onHit( Vector3D ray, Vector3D normal, SurfaceMaterialLayer sm ) {
		if( random.nextDouble() < sm.opacity ) {
			Vector3D randTerm = randomVector(sm.randomRedirectFactor);
			Vector3D mirrorTerm = mirrorVector(ray, normal, sm.mirrorRedirectFactor);
			VectorMath.normalize( normal, sm.normalRedirectFactor, normalTerm );
			
			ray.normalizeInPlace( sm.forwardRedirectFactor );
			VectorMath.add( ray, normalTerm, ray );
			VectorMath.add( ray, mirrorTerm, ray );
			VectorMath.add( ray, randTerm  , ray );
			assert ray.isRegular();
			
			applyMaterialColor( sm );
			return true;
		} else {
			return false;
		}
			
	}
	
	/** 
	 * @param ray input ray; will also be overwritten with new direction
	 * @param normal
	 * @param sm
	 * @return false if the ray bounces away from the surface, true if it passes through
	 */
	protected boolean processSurfaceInteraction( Vector3D ray, Vector3D normal, SurfaceMaterial sm ) {
		for( int li=sm.layers.length-1; li>=0; --li ) {
			SurfaceMaterialLayer l = sm.layers[li];
			if( onHit(ray, normal, l) && VectorMath.dotProduct(ray, normal) > 0 ) {
				return false;
			}
		}
		return true;
	}
	
	protected boolean filterIsBlack() {
		return filterRed == 0 && filterGreen == 0 && filterBlue == 0;
	}
	
	public int maxSteps = 4096;
	public int maxQuickTraceSteps = 256;
	public int maxBounces = 10;
	
	final Vector3D scratch = new Vector3D();
	
	/**
	 * Determines (probibalistically) if refraction or reflection will occur
	 * when a ray passes between media with different indexes of refraction and
	 * updates 'direction' in either case.
	 * 
	 * @param ni index of refraction of source (incident ray) material
	 * @param nr index of refraction in destination (refracted ray) material
	 * @return true if the ray passes through the surface, false if it bounces off
	 */
	private boolean refract( double ni, double nr ) {
		direction.normalizeInPlace(1);
		
		double dotProduct = -normal.normalize().dot(direction);
		if( dotProduct < 0 ) {
			// Happens when we get near edges
			red += filterRed;
			return true;
		}
		assert dotProduct >= 0;
		assert dotProduct <= 1;
		
		Vector3D s = normal.normalize( dotProduct ).add( direction );
		double sinAngleOfIncidence = s.magnitude();
		if( sinAngleOfIncidence >= 1 ) {
			System.err.println("Sin = "+sinAngleOfIncidence);
			green += filterGreen;
			return true;
		}
		assert sinAngleOfIncidence > 0;
		assert sinAngleOfIncidence < 1;
		
		double sinAngleOfRefraction = sinAngleOfIncidence * ni / nr;
		assert sinAngleOfRefraction >= 0;
		
		/*
		 * Note: Squaring of sinAngleOfRefraction is arbitrarily chosen;
		 * I don't yet know what the realistic method would be to determine
		 * the chance of reflection.
		 *
		 * It is important that reflection always occurs when sinAngleOfRefraction >= 1
		 * (This case is known as 'total internal reflection')
		 */
		if( random.nextDouble() < sinAngleOfRefraction*sinAngleOfRefraction ) {
			VectorMath.reflect( direction, normal, direction );
			return false;
		}
				
		assert sinAngleOfRefraction < 1;
		
		double angleOfRefraction = Math.asin( sinAngleOfRefraction );
		assert angleOfRefraction >= 0;
		
		double cosAngleOfRefraction = Math.cos(angleOfRefraction);
		
		direction.set( s.normalize( sinAngleOfRefraction ).subtract(normal.normalize(cosAngleOfRefraction)) );
		
		return true;
	}
	
	/*
	 * Assuming that light added by passing through a material that both glows and filters
	 * = glow (per meter) * integral from x=0 to distance of filter**x
	 * 
	 * which, according to Wolfram Alpha
	 * http://www.wolframalpha.com/input/?i=Integrate%5Bf%5Ex%2C+%7Bx%2C+0%2C+d%7D%5D
	 * 
	 * means it = glow * (f**d - 1) / ln(f) 
	 */
	protected final double filterGlow( double glow, double filter, double filterExpDistance, double distance ) {
		if( glow == 0 ) return 0;
		if( filter == 1 ) return glow * distance;
		
		assert glow >= 0;
		double add = glow * (filterExpDistance - 1) / Math.log(filter);
		if( add < 0 || Double.isNaN(add) || Double.isInfinite(add) ) {
			System.err.println("glow="+glow);
			System.err.println("filter="+filter);
			System.err.println("distance="+distance);
			System.err.println("filter**dist="+filterExpDistance);
			System.err.println("ln(filter)="+Math.log(filter));
			throw new RuntimeException("Bad value for filterGlow result: "+add);
		}
		return add;
	}
	
	/**
	 * Temporary storage for sky color coming from sky sphere
	 */
	private DColor skyColor = new DColor(0,0,0);
	
	public void fullTrace( double x, double y, double z, double dx, double dy, double dz ) {
		setPosition( x, y, z );
		setDirection( dx, dy, dz );
		
		filterRed = filterGreen = filterBlue = (double)1.0;
		red = green = blue = (double)0.0;
		
		step: for( int steps=0, bounces=0; steps<maxSteps && bounces<maxBounces && !filterIsBlack(); ++steps ) {
			final PathTraceMaterial material = cursors[cursorIdx].node.material;
			boolean scattered = false;
			
			if( !findNextIntersection( pos, direction, preIntersect, postIntersect ) ) {
				direction.normalizeInPlace(1);
				skySphere.getSkyColor(direction, skyColor);
				red   += filterRed   * skyColor.r;
				green += filterGreen * skyColor.g;
				blue  += filterBlue  * skyColor.b;
				return;
			}
			
			double dist = VectorMath.dist( pos, postIntersect );
			
			if( material.getParticleInteractionChance() > 0 ) {
				// Could run this even when scat = 0 but it would be pointless.
				
				// probability(distance) = 1 - (1 - probability(1)) ** distance  
				// distance(rand(0..1)) = inverse of probability(distance) or of 1 - probability(distance)
				double scatterDist = -Math.log(random.nextDouble()) / Math.log( 1 / (1-material.getParticleInteractionChance()) );
				if( scatterDist < dist ) {
					// Scattered!
					// Let's say for now it's at some random point (which is terribly wrong).
					direction.normalizeInPlace(scatterDist);
					VectorMath.add( pos, direction, postIntersect );
					processSurfaceInteraction( direction, randomVector(1), material.getParticleSurfaceMaterial() );
					++bounces;
					
					scattered = true;
				}
			}
			
			DColor internalEmissionColor = material.getInternalEmissionColor();
			DColor internalFilterColor = material.getInternalFilterColor();
			
			double fExpDistRed   = Math.pow(internalFilterColor.r, dist);
			double fExpDistGreen = Math.pow(internalFilterColor.g, dist);
			double fExpDistBlue  = Math.pow(internalFilterColor.b, dist);
			
			red   += filterRed   * filterGlow(internalEmissionColor.r, internalFilterColor.r, fExpDistRed  , dist);
			green += filterGreen * filterGlow(internalEmissionColor.g, internalFilterColor.g, fExpDistGreen, dist);
			blue  += filterBlue  * filterGlow(internalEmissionColor.b, internalFilterColor.b, fExpDistBlue , dist);
			
			filterRed   *= fExpDistRed;
			filterGreen *= fExpDistGreen;
			filterBlue  *= fExpDistBlue;
			
			// Otherwise it just passes through!
			
			setPosition(postIntersect);
			// If scattered can skip surface check since we know newSurface = surface 
			if( scattered ) {
				continue step;
			}
			
			PathTraceMaterial newMaterial = cursors[cursorIdx].node.material;
			
			if( newMaterial != material ) {
				double ni = material.getIndexOfRefraction();
				double nr = newMaterial.getIndexOfRefraction();
				
				assert direction.isRegular();
				if(
					processSurfaceInteraction( direction, normal, newMaterial.getSurfaceMaterial() )
					&& (nr == ni || refract( ni, nr ))
				) {
					// Move to new position
				} else {
					// Move back to old node
					setPosition(preIntersect);
					++bounces;
				}
				assert direction.isRegular();
			}
		}
	}
	
	public void quickTrace( double x, double y, double z, double dx, double dy, double dz ) {
		setPosition( x, y, z );
		setDirection( dx, dy, dz );
		
		red = green = blue = (double)0.0;

		for( int i=0; i<maxQuickTraceSteps; ++i ) {
			if( !findNextIntersection( pos, direction, preIntersect, postIntersect ) ) {
				direction.normalizeInPlace(1);
				skySphere.getSkyColor(direction, skyColor);
				red   = skyColor.r;
				green = skyColor.g;
				blue  = skyColor.b;
				return;
			}
			setPosition(postIntersect);
			final PathTraceMaterial newMaterial = cursors[cursorIdx].node.material;
			final SurfaceMaterial surfaceMaterial = newMaterial.getSurfaceMaterial();
			if( surfaceMaterial.layers.length > 0 ) {				
				double adjust = normal.x*0.1 + normal.y*0.2 + normal.z*0.3;
				
				for( int li=surfaceMaterial.layers.length-1; li>=0; --li ) {
					SurfaceMaterialLayer l = surfaceMaterial.layers[li];
					red   = l.emissionColor.r + l.filterColor.r * (0.5 + adjust);
					green = l.emissionColor.g + l.filterColor.g * (0.5 + adjust);
					blue  = l.emissionColor.b + l.filterColor.b * (0.5 + adjust);
				}
				return;
			}
		}
	}
	
	public void trace( double ox, double oy, double oz, double dx, double dy, double dz ) {
		switch( mode ) {
		case FULL: fullTrace(ox,oy,oz,dx,dy,dz); break;
		case QUICK: quickTrace(ox,oy,oz,dx,dy,dz); break;
		default: assert false;
		}
	}
	
	public void trace( Vector3D o, Vector3D d ) {
		trace( o.x, o.y, o.z, d.x, d.y, d.z );
	}
}
