package util.st.filter;

import net.imglib2.RealLocalizable;
import net.imglib2.neighborsearch.RadiusNeighborSearch;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;

public class MedianFilter< T extends RealType< T > > extends RadiusSearchFilter< T, T >
{
	final T outofbounds;

	public MedianFilter( final RadiusNeighborSearch< T > search, final double radius, final T outofbounds )
	{
		super( search, radius );

		this.outofbounds = outofbounds;
	}

	@Override
	public void filter( final RealLocalizable position, final T output )
	{
		search.search( position, radius, false );

		if ( search.numNeighbors() > 1 )
		{
			final double[] values = new double[ search.numNeighbors() ];

			for ( int i = 0; i < search.numNeighbors(); ++i )
				values[ i ] = search.getSampler( i ).get().getRealDouble();

			output.setReal( Util.median( values ) );
		}
		else if ( search.numNeighbors() == 1 )
		{
			output.set( search.getSampler( 0 ).get() );
		}
		else
		{
			output.set( outofbounds );
		}
	}
}
