package togos.solidtree;

public class StandardMaterial implements GeneralMaterial
{
	private static final long serialVersionUID = 1L;
	
	public static StandardMaterial SPACE = new StandardMaterial(
		SurfaceMaterial.TRANSPARENT,
		1, DColor.WHITE, DColor.BLACK,
		0, SurfaceMaterial.TRANSPARENT
	);
	
	public static StandardMaterial opaque( SurfaceMaterial surface ) {
		return new StandardMaterial(
			surface,
			1, DColor.WHITE, DColor.BLACK,
			0, SurfaceMaterial.TRANSPARENT
		);
	}
	
	/** Chance that the redirection of a given ray will be determined by the surface material */
	public final SurfaceMaterial surfaceMaterial;
	
	public final double indexOfRefraction;
	public final DColor internalFilterColor;
	public final DColor internalEmissionColor;
	
	/** Chance per meter if interacting with a particle within the material */
	public final double particleInteractionChance;
	public final SurfaceMaterial particleMaterial;
	
	public StandardMaterial(
		SurfaceMaterial surfaceMaterial,
		double indexOfRefraction, DColor internalFilterColor, DColor internalEmissionColor,
		double particleInteractionChance, SurfaceMaterial particleMaterial
	) {
		this.surfaceMaterial = surfaceMaterial;
		this.indexOfRefraction = indexOfRefraction;
		this.internalFilterColor = internalFilterColor;
		this.internalEmissionColor = internalEmissionColor;
		this.particleInteractionChance = particleInteractionChance;
		this.particleMaterial = particleMaterial;
	}

	@Override public SurfaceMaterial getSurfaceMaterial() { return surfaceMaterial; }
	@Override public double getParticleInteractionChance() { return particleInteractionChance; }
	@Override public SurfaceMaterial getParticleSurfaceMaterial() { return particleMaterial; }
	@Override public DColor getInternalEmissionColor() { return internalEmissionColor; }
	@Override public DColor getInternalFilterColor() { return internalFilterColor; }
	@Override public double getIndexOfRefraction() { return indexOfRefraction; }
}
