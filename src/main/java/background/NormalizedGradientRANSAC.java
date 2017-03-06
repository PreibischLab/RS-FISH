package background;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

import fit.Center;
import fit.PointFunctionMatch;
import gradient.Gradient;
import mpicbg.models.Point;

public class NormalizedGradientRANSAC extends NormalizedGradient
{
	double maxError, minInlierRatio;

	public NormalizedGradientRANSAC( final Gradient gradient, final double maxError, final double minInlierRatio )
	{
		super( gradient );

		this.maxError = maxError;
		this.minInlierRatio = minInlierRatio;
	}

	public NormalizedGradientRANSAC( final Gradient gradient )
	{
		this( gradient, 0.05, 0.3 );
	}

	public void setMaxError( final double maxError ) { this.maxError = maxError; }
	public void setMinInlierRatio( final double minInlierRatio ) { this.minInlierRatio = minInlierRatio; }

	public double getMaxError() { return maxError; }
	public double getMinInlierRatio() { return minInlierRatio; }

	@Override
	protected void computeBackground(
			final ArrayList< LinkedList< Double > > gradientsPerDim,
			final double[] bkgrnd )
	{
		// TODO: Parameters in GUI: 0.05, 0.3
		for ( int d = 0; d < n; ++d )
			bkgrnd[ d ] = runRANSAC( gradientsPerDim.get( d ), maxError, minInlierRatio );
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
			l.fit( inliers );

			return l.getP();
		}
		catch ( Exception e ) { return 0; }
	}

}
