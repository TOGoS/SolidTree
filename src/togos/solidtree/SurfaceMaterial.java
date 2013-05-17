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
	public final double forwardRedirectFactor;
	
	public SurfaceMaterial(
		DColor filterColor, DColor emissionColor,
		double mirrorRF, double randomRF,
		double normalRF, double forwardRF
	) {
		this.filterColor   = filterColor;
		this.emissionColor = emissionColor;
		this.mirrorRedirectFactor   = mirrorRF;
		this.randomRedirectFactor   = randomRF;
		this.normalRedirectFactor   = normalRF;
		this.forwardRedirectFactor  = forwardRF;
	}
}
