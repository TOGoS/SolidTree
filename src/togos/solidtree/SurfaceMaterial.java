package togos.solidtree;

public class SurfaceMaterial
{
	public static final SurfaceMaterial TRANSPARENT = new SurfaceMaterial();
	
	public final SurfaceMaterialLayer[] layers;
	
	public SurfaceMaterial( SurfaceMaterialLayer...layers ) {
		this.layers = layers;
	}
}
