package util.st.filter;

import net.imglib2.KDTree;
import net.imglib2.neighborsearch.RadiusNeighborSearchOnKDTree;
import net.imglib2.type.numeric.RealType;

public class MedianFilterFactory< T extends RealType< T > > extends RadiusSearchFilterFactory< T, T >
{
	final T outofbounds;
	final double radius;

	public MedianFilterFactory(
			final T outofbounds,
			final double radius )
	{
		this.radius = radius;
		this.outofbounds = outofbounds;
	}

	@Override
	public Filter< T > createFilter( final KDTree< T > tree )
	{
		return new MedianFilter< T >(
				new RadiusNeighborSearchOnKDTree<>( tree ),
				radius,
				outofbounds );
	}

	@Override
	public T create()
	{
		return outofbounds.createVariable();
	}
}
