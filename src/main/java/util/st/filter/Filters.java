package util.st.filter;

import net.imglib2.Cursor;
import net.imglib2.IterableRealInterval;
import net.imglib2.Iterator;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.RealPointSampleList;
import net.imglib2.view.Views;

public class Filters
{
	public static < S, T > RealPointSampleList< T > filter( final IterableRealInterval< S > data, final FilterFactory< S, T > filterFactory )
	{
		return filter( data, data.localizingCursor(), filterFactory );
	}

	public static < S, T, C extends RealLocalizable & Iterator > RealPointSampleList< T > filter(
			final IterableRealInterval< S > data,
			final C outputCursor,
			final FilterFactory< S, T > filterFactory )
	{
		final RealPointSampleList< T > filtered = new RealPointSampleList<>( data.numDimensions() );

		final Filter< T > filter = filterFactory.createFilter( data );

		while ( outputCursor.hasNext() )
		{
			outputCursor.fwd();

			final T value = filterFactory.create();

			filter.filter( outputCursor, value );

			filtered.add( new RealPoint( outputCursor ), value );
		}

		return filtered;
	}

	public static < S, T > void filter(
			final IterableRealInterval< S > data,
			final RandomAccessibleInterval< T > filtered,
			final FilterFactory< S, T > filterFactory )
	{
		final Filter< T > filter = filterFactory.createFilter( data );

		final Cursor< T > cursor = Views.iterable( filtered ).localizingCursor();

		while ( cursor.hasNext() )
		{
			cursor.fwd();
			filter.filter( cursor, cursor.get() );
		}
	}
}
