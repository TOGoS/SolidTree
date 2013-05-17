package togos.solidtree;

public class VolumetricMaterial
{
	public static VolumetricMaterial SPACE = new VolumetricMaterial(
		0, SurfaceMaterial.TRANSPARENT,
		1, DColor.WHITE, DColor.BLACK,
		0, SurfaceMaterial.TRANSPARENT
	);
	
	public static VolumetricMaterial opaque( SurfaceMaterial surface ) {
		return new VolumetricMaterial(
			1, surface,
			1, DColor.WHITE, DColor.BLACK,
			0, SurfaceMaterial.TRANSPARENT
		);
	}
	
	/** Chance that the redirection of a given ray will be determined by the surface material */
	public final double surfaceInteractionChance;
	public final SurfaceMaterial surfaceMaterial;
	
	public final double indexOfRefraction;
	public final DColor internalFilterColor;
	public final DColor internalEmissionColor;
	
	/** Chance per meter if interacting with a particle within the material */
	public final double particleInteractionChance;
	public final SurfaceMaterial particleMaterial;
	
	public VolumetricMaterial(
		double surfaceInteractionChance, SurfaceMaterial surfaceMaterial,
		double indexOfRefraction, DColor internalFilterColor, DColor internalEmissionColor,
		double particleInteractionChance, SurfaceMaterial particleMaterial
	) {
		this.surfaceInteractionChance = surfaceInteractionChance;
		this.surfaceMaterial = surfaceMaterial;
		this.indexOfRefraction = indexOfRefraction;
		this.internalFilterColor = internalFilterColor;
		this.internalEmissionColor = internalEmissionColor;
		this.particleInteractionChance = particleInteractionChance;
		this.particleMaterial = particleMaterial;
	}
}
