package togos.solidtree;

public class SurfaceMaterial
{
	public static final SurfaceMaterial TRANSPARENT =
		new SurfaceMaterial( DColor.WHITE, DColor.BLACK, 0, 0, 0, 1 );
	
	/** Redirected rays will be multiplied by this color */
	public final DColor filterColor;
	/** This color will be emitted in all directions */ 
	public final DColor emissionColor;
	
	public final double mirrorRedirectFactor;
	public final double randomRedirectFactor;
	public final double normalRedirectFactor;
	public final double straightRedirectFactor;
	
	public SurfaceMaterial(
		DColor filterColor, DColor emissionColor,
		double scatterMirror, double scatterRandom,
		double scatterNormal, double scatterStraight
	) {
		this.filterColor   = filterColor;
		this.emissionColor = emissionColor;
		this.mirrorRedirectFactor   = scatterMirror;
		this.randomRedirectFactor   = scatterRandom;
		this.normalRedirectFactor   = scatterNormal;
		this.straightRedirectFactor = scatterStraight;
	}
}
