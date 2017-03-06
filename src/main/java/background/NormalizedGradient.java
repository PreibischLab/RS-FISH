package background;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

import fit.Center;
import fit.PointFunctionMatch;
import gradient.Gradient;
import mpicbg.models.Point;
import net.imglib2.FinalInterval;
import net.imglib2.Localizable;
import net.imglib2.iterator.IntervalIterator;
import net.imglib2.util.Util;

public class NormalizedGradient extends Gradient
{
	final Gradient gradient;
	final double[] bkgrnd;
	final int n;

	public NormalizedGradient( final Gradient gradient )
	{
		super( gradient.numDimensions() );

		this.n = numDimensions();
		this.gradient = gradient;
		this.bkgrnd = new double[ n ];
	}

	public double[] getBackground() { return bkgrnd; }

	// compute the median of the bounding area in all dimensions
	public void normalize( final FinalInterval spotInterval )
	{
		// the list of gradients on the boundary separated by dimension
		final ArrayList< LinkedList< Double > > gradientsPerDim = new ArrayList<>( n );

		for ( int d = 0; d < n; ++d )
			gradientsPerDim.add(  new LinkedList<>() );

		// define a local region to iterate around the potential detection
		final IntervalIterator cursor = new IntervalIterator( spotInterval );

		final double[] v = new double[ n ];

		while ( cursor.hasNext() )
		{
			cursor.fwd();

			// TODO: simplest boundary iterator possible, maybe interface?
			if ( isBoundaryPixel( cursor, spotInterval, n ) )
			{
				gradient.gradientAt( cursor, v );

				for ( int d = 0; d < n; ++d )
					gradientsPerDim.get( d ).add( v[ d ] );
			}
		}

		for ( int d = 0; d < n; ++d )
			bkgrnd[ d ] = runRANSAC( gradientsPerDim.get( d ), 0.05, 0.3 );
	}

	@Override
	public void gradientAt( final Localizable location, final double[] derivativeVector )
	{
		gradient.gradientAt( location, derivativeVector );

		for ( int d = 0; d < n; ++d )
			derivativeVector[ d ] -= bkgrnd[ d ];
	}

	public static double runRANSAC( final Collection< Double > values, final double maxError, final double minInlierRatio )
	{
		final ArrayList< PointFunctionMatch > candidates = new ArrayList<PointFunctionMatch>();
		final ArrayList< PointFunctionMatch > inliers = new ArrayList<PointFunctionMatch>();
		
		for ( final double d : values )
			candidates.add( new PointFunctionMatch( new Point( new double[]{ d } ) ) );
		
		final Center l = new Center();

		try
		{
			l.ransac( candidates, inliers, 500, maxError, minInlierRatio );
			
			System.out.println( inliers.size() + " / " + candidates.size()  );
			l.fit( inliers );

			return l.getP();
		}
		catch ( Exception e ) { return 0; }
	}

	public static double absPercentile( final double[] values, final double percentile )
	{
		final int length = values.length;
		final double[] temp = new double[ length ];
		final int[] indices = new int[ length ];

		for ( int i = 0; i < indices.length; ++i )
		{
			indices[ i ] = i;
			temp[ i ] = Math.abs( values[ i ] );
		}

		Util.quicksort( temp, indices, 0, length - 1  );

		final int originalIndex = indices[ Math.min( length - 1, Math.max( 0, ( int ) Math.round( ( length - 1 ) * percentile ) ) ) ];
		return values[ originalIndex ];
	}

	public static double[] collection2Array( final Collection< Double > c )
	{
		final double[] l = new double[ c.size() ];

		int i = 0;
		for ( final double v : c )
			l[ i++ ] = v;

		return l;
	}

	public static boolean isBoundaryPixel( final Localizable l, final FinalInterval spotInterval, final int n )
	{
		for ( int d = 0; d < n; ++d )
		{
			final int p = l.getIntPosition( d );
			
			if ( p == spotInterval.min( d ) || p == spotInterval.max( d ) )
				return true;
		}

		return false;
	}
}
