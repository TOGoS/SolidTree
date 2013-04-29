package togos.solidtree;

public class Material
{
	public static Material SPACE = new Material(DColor.BLACK, DColor.BLACK, 0);
	
	public final DColor diffuseColor;
	public final DColor ambientColor;
	public final double opacity;    // Per meter!
	public final double scattering; // Chance, per meter
	
	public Material( DColor diffuseColor, DColor ambientColor, double opacity  ) {
		this.diffuseColor = diffuseColor;
		this.ambientColor = ambientColor;
		this.opacity = opacity;
		this.scattering = 0;
	}
}
