package util.st.filter;

import net.imglib2.neighborsearch.RadiusNeighborSearch;

public abstract class RadiusSearchFilter< S, T > implements Filter< T >
{
	final RadiusNeighborSearch< S > search;
	final double radius;

	public RadiusSearchFilter(
			final RadiusNeighborSearch< S > search,
			final double radius )
	{
		this.search = search;
		this.radius = radius;
	}
}
