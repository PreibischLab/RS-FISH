package util.st.render;

import net.imglib2.RealInterval;
import net.imglib2.interpolation.InterpolatorFactory;

public class IntegratingNeighborSearchInterpolatorFactory< T > implements InterpolatorFactory< T, IntegratingNeighborSearch< T > >
{
	@Override
	public IntegratingNeighborSearchInterpolator< T > create( final IntegratingNeighborSearch< T > search )
	{
		return new IntegratingNeighborSearchInterpolator< T >( search.copy() );
	}

	@Override
	public IntegratingNeighborSearchInterpolator< T > create( final IntegratingNeighborSearch< T > search, final RealInterval interval )
	{
		return create( search );
	}
}
