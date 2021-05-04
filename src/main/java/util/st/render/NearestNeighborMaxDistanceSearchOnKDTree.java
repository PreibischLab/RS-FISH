package util.st.render;

import net.imglib2.KDTree;
import net.imglib2.RealLocalizable;
import net.imglib2.Sampler;
import net.imglib2.neighborsearch.NearestNeighborSearchOnKDTree;
import util.st.render.util.SimpleRealLocalizable;
import util.st.render.util.SimpleSampler;

public class NearestNeighborMaxDistanceSearchOnKDTree< T > extends NearestNeighborSearchOnKDTree< T >
{
	final T outofbounds;
	final SimpleSampler< T > oobsSampler;
	final SimpleRealLocalizable position;
	final double maxSqDistance, maxDistance;

	Sampler< T > value;
	RealLocalizable point;
	double newbestSquDistance;

	public NearestNeighborMaxDistanceSearchOnKDTree( final KDTree< T > tree, final T outofbounds, final double maxDistance )
	{
		super( tree );

		this.oobsSampler = new SimpleSampler< T >( outofbounds );
		this.position = new SimpleRealLocalizable( pos );
		this.maxDistance = maxDistance;
		this.maxSqDistance = maxDistance * maxDistance;
		this.outofbounds = outofbounds;
	}

	@Override
	public void search( final RealLocalizable p )
	{
		super.search( p );

		if ( bestSquDistance > maxSqDistance )
		{
			value = oobsSampler;
			point = position;
			newbestSquDistance = 0;
		}
		else
		{
			value = bestPoint;
			point = bestPoint;
			newbestSquDistance = bestSquDistance;
		}
	}

	@Override
	public Sampler< T > getSampler()
	{
		return value;
	}

	@Override
	public RealLocalizable getPosition()
	{
		return point;
	}

	@Override
	public double getSquareDistance()
	{
		return newbestSquDistance;
	}

	@Override
	public double getDistance()
	{
		return Math.sqrt( newbestSquDistance );
	}

	@Override
	public NearestNeighborMaxDistanceSearchOnKDTree< T > copy()
	{
		final NearestNeighborMaxDistanceSearchOnKDTree< T > copy = new NearestNeighborMaxDistanceSearchOnKDTree< T >( tree, outofbounds, maxDistance );
		System.arraycopy( pos, 0, copy.pos, 0, pos.length );
		copy.bestPoint = bestPoint;
		copy.bestSquDistance = bestSquDistance;
		copy.newbestSquDistance = newbestSquDistance;
		copy.point = point;
		copy.value = value;
		return copy;
	}
}
