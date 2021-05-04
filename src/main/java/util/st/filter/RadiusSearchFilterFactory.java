package util.st.filter;

import net.imglib2.IterableRealInterval;
import net.imglib2.KDTree;

public abstract class RadiusSearchFilterFactory< S, T > implements FilterFactory< S, T >
{
	@Override
	public Filter< T > createFilter( final IterableRealInterval< S > data )
	{
		return createFilter( new KDTree< S >( data ) );
	}

	public abstract Filter< T > createFilter( final KDTree< S > tree );
}
