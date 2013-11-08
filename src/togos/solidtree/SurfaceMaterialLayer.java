package togos.solidtree;

import java.io.Serializable;

public class SurfaceMaterialLayer implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	public static final SurfaceMaterialLayer TRANSPARENT =
		new SurfaceMaterialLayer( 0, DColor.WHITE, DColor.BLACK, 0, 0, 0, 1 );
	
	/**
	 * Chance that a ray will be affected by this layer.
	 */
	public final double opacity;
	
	/** Redirected rays will be multiplied by this color */
	public final DColor filterColor;
	/** This color will be emitted in all directions */ 
	public final DColor emissionColor;
	
	public final double mirrorRedirectFactor;
	public final double randomRedirectFactor;
	public final double normalRedirectFactor;
	public final double forwardRedirectFactor;
	
	public SurfaceMaterialLayer(
		double opacity,
		DColor filterColor, DColor emissionColor,
		double mirrorRF, double randomRF,
		double normalRF, double forwardRF
	) {
		this.opacity       = opacity;
		this.filterColor   = filterColor;
		this.emissionColor = emissionColor;
		this.mirrorRedirectFactor   = mirrorRF;
		this.randomRedirectFactor   = randomRF;
		this.normalRedirectFactor   = normalRF;
		this.forwardRedirectFactor  = forwardRF;
	}
}
