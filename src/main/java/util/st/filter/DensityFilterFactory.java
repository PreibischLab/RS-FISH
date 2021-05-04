package util.st.filter;

import net.imglib2.KDTree;
import net.imglib2.neighborsearch.RadiusNeighborSearchOnKDTree;
import net.imglib2.type.numeric.RealType;

public class DensityFilterFactory< T extends RealType< T > > extends RadiusSearchFilterFactory< T, T >
{
	final double radius;
	final T type;

	public DensityFilterFactory( final T type, final double radius )
	{
		this.radius = radius;
		this.type = type;
	}

	@Override
	public Filter< T > createFilter( final KDTree< T > tree )
	{
		return new DensityFilter< T >(
				new RadiusNeighborSearchOnKDTree<>( tree ),
				radius );
	}

	@Override
	public T create()
	{
		return type.createVariable();
	}
}
