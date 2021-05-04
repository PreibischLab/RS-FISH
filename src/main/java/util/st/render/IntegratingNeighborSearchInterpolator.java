package util.st.render;

import net.imglib2.RealPoint;
import net.imglib2.RealRandomAccess;

public class IntegratingNeighborSearchInterpolator < T > extends RealPoint implements RealRandomAccess< T >
{
	final protected IntegratingNeighborSearch< T > search;

	public IntegratingNeighborSearchInterpolator( final IntegratingNeighborSearch< T > search )
	{
		super( search.numDimensions() );
		this.search = search;
	}

	@Override
	public T get()
	{
		search.search( this );
		return search.getSampler().get();
	}

	@Override
	public IntegratingNeighborSearchInterpolator< T > copy()
	{
		return new IntegratingNeighborSearchInterpolator< T >( search.copy() );
	}

	@Override
	public IntegratingNeighborSearchInterpolator< T > copyRealRandomAccess()
	{
		return copy();
	}
}
