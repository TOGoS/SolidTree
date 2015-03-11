package togos.solidtree;

import togos.lazy.Ref;


public interface SolidNode
{
	enum Type {
		HOMOGENEOUS,
		REGULARLY_SUBDIVIDED,
		DENSITY_FUNCTION_SUBDIVIDED
	}
	
	public Type getType();
	// For homogeneous nodes...
	public GeneralMaterial getHomogeneousMaterial();
	// For regularly subdivided nodes...
	public int getDivX();
	public int getDivY();
	public int getDivZ();
	public Ref<SolidNode> subNode(int idx);
	public Ref<SolidNode> subNode(int x, int y, int z);
	// For nodes, split by a density function
	// subNode(0 or 1) will be re-used
	public DensityFunction getSubdivisionFunction();
}
