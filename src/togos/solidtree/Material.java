package togos.solidtree;

public class Material
{
	public static Material SPACE = new Material(DColor.BLACK, DColor.BLACK, 0, 0);

	public final DColor filterColor;
	public final DColor ambientColor;

	/**
	 * Chance that rays will bounce normally off the surface.
	 */
	public final double mirrosity;

	/**
	 * Chance per meter that rays passing through will be scattered
	 */
	public final double scattering;
	
	public Material( DColor filterColor, DColor ambientColor, double mirrosity, double scattering  ) {
		this.filterColor  = filterColor;
		this.ambientColor = ambientColor;
		this.mirrosity    = mirrosity;
		this.scattering   = scattering;
	}
}
