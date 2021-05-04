package util.st.filter;

import net.imglib2.RealLocalizable;
import net.imglib2.neighborsearch.RadiusNeighborSearch;
import net.imglib2.type.numeric.RealType;

/**
 * Visualizes the density at a certain location given the radius
 * 
 * @author spreibi
 *
 * @param <T> - a RealType
 */
public class DensityFilter< T extends RealType< T > > extends RadiusSearchFilter< T, T >
{
	public DensityFilter( final RadiusNeighborSearch< T > search, final double radius )
	{
		super( search, radius );
	}

	@Override
	public void filter( final RealLocalizable position, final T output )
	{
		search.search( position, radius, false );

		output.setReal( search.numNeighbors() );
	}
}
