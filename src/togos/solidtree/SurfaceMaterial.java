package togos.solidtree;

import java.io.Serializable;

public class SurfaceMaterial implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	public static final SurfaceMaterial TRANSPARENT = new SurfaceMaterial();
	
	// Layers from innermost to outermost --
	// a ray hitting a surface with this material will have a chance to interact
	// with the last layer in this list, then the second last, etc.
	public final SurfaceMaterialLayer[] layers;
	
	public SurfaceMaterial( SurfaceMaterialLayer...layers ) {
		this.layers = layers;
	}
	
	public static SurfaceMaterial combine( SurfaceMaterial m, SurfaceMaterialLayer...additionalLayers ) {
		// Adds layers on top of m
		SurfaceMaterialLayer[] combinedLayers = new SurfaceMaterialLayer[additionalLayers.length + m.layers.length];
		int j = 0;
		for( int i=0; i<m.layers.length; ++i, ++j ) {
			combinedLayers[j] = m.layers[i];
		}
		for( int i=0; i<additionalLayers.length; ++i, ++j ) {
			combinedLayers[j] = additionalLayers[i];
		}
		return new SurfaceMaterial(combinedLayers);
	}
}
