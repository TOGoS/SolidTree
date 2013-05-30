package togos.solidtree;

public interface PathTraceMaterial
{
	SurfaceMaterial getSurfaceMaterial();
	
	double getParticleInteractionChance();
	SurfaceMaterial getParticleSurfaceMaterial();
	
	DColor getInternalEmissionColor();
	DColor getInternalFilterColor();
	double getIndexOfRefraction();
}
