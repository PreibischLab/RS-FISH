package fit;

import gradient.Gradient3d;

import java.util.ArrayList;
import java.util.Random;

import derivative.Derivative;

import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.Point;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.algorithm.legacy.scalespace.DifferenceOfGaussianPeak;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

public class Spot 
{
	public final SymmetryCenter3d center = new SymmetryCenter3d();
	public final ArrayList< PointFunctionMatch > candidates = new ArrayList<PointFunctionMatch>();
	public final ArrayList< PointFunctionMatch > inliers = new ArrayList<PointFunctionMatch>();
	public final float[] scale = new float[]{ 1, 1, 1 };
		
	public int numRemoved = -1;
	public double avgCost = -1, minCost = -1, maxCost = -1;
	
	@Override
	public String toString()
	{
		return "center: " + center.getXc()/scale[ 0 ] + " " + center.getYc()/scale[ 1 ] + " " + center.getZc()/scale[ 2 ] + " Removed = " + numRemoved + "/" + candidates.size() + " error = " + minCost + ";" + avgCost + ";" + maxCost;
	}
	
	public double[] getCenter() { return new double[]{ center.getXc()/scale[ 0 ], center.getYc()/scale[ 1 ], center.getZc()/scale[ 2 ] }; }
	public void getCenter( final double[] c )
	{ 
		 c[ 0 ] = center.getXc()/scale[ 0 ];
		 c[ 1 ] = center.getYc()/scale[ 1 ];
		 c[ 2 ] = center.getZc()/scale[ 2 ]; 
	}
	
	public double computeAverageCostCandidates() { return computeAverageCost( candidates ); }
	public double computeAverageCostInliers() { return computeAverageCost( inliers ); }

	public double computeAverageCost( final ArrayList< PointFunctionMatch > set )
	{
		minCost = Double.MAX_VALUE;
		maxCost = 0;
		avgCost = 0;
		
		for ( final PointFunctionMatch pm : set )
		{
			pm.apply( center );
			final double d = pm.getDistanceDouble();
			
			avgCost += d;
			minCost = Math.min( d, minCost );
			maxCost = Math.max( d, maxCost );
		}
		
		avgCost /= (double) set.size();
		
		return avgCost;
	}
	
	public void updateScale( final float[] scale )
	{
		for ( final PointFunctionMatch pm : candidates )
		{
			final OrientedPoint p = (OrientedPoint)pm.getP1();
			
			final float[] l = p.getL();
			final float[] w = p.getW();
			final float[] ol = p.getOrientationL();
			final float[] ow = p.getOrientationW();
			
			for ( int d = 0; d < l.length; ++d )
			{
				w[ d ] = l[ d ] * scale[ d ];
				ow[ d ] = ol[ d ] / scale[ d ];
			}
		}
		
		for ( int d = 0; d < scale.length; ++d )
			this.scale[ d ] = scale[ d ];
	}


	public static ArrayList< Spot > extractSpots( final Img< FloatType > image, final ArrayList< int[] > peaks, final Derivative derivative )
	{
		System.out.println( "Found " + peaks.size() + " peaks. " );
		
		final int numDimensions = image.numDimensions();
		
		// size around the detection to use
		// we detect at 0.5, 0.5, 0.5 - so we need an even size
		final int[] size = new int[]{ 10, 10, 10 };
		
		final long[] min = new long[ 3 ];
		final long[] max = new long[ 3 ];
		
		final int w = (int)image.dimension( 0 ) - 2;
		final int h = (int)image.dimension( 1 ) - 2;
		final int d = (int)image.dimension( 2 ) - 2;
		
		//final RandomAccess< FloatType > randomAccess = image.randomAccess();// new OutOfBoundsStrategyValueFactory<FloatType>() );
		
		final ArrayList< Spot > spots = new ArrayList<Spot>();		
		final RandomAccessible< FloatType > infinite = Views.extendZero( image );
		
		int i = 0;
		
		for ( final int[] peak : peaks )
		{	
			System.out.println( "peak: " + Util.printCoordinates( peak ) );
			
			final Spot spot = new Spot();
			
			for ( int e = 0; e < numDimensions; ++e )
			{
				min[ e ] = peak[ e ] - size[ e ] / 2;
				max[ e ] = min[ e ] + size[ e ] - 1;
			}

			final Cursor< FloatType > cursor = Views.iterable( Views.interval( infinite, min, max ) ).localizingCursor();
			
			while ( cursor.hasNext() )
			{
				cursor.fwd();
								
				final int x = cursor.getIntPosition( 0 );
				final int y = cursor.getIntPosition( 1 );
				final int z = cursor.getIntPosition( 2 );
				
				if ( x < 0 || y < 0 || z < 0 || x > w || y > h || z > d )
					continue;
				
				final float[] v = new float[ 3 ];
				
				derivative.gradientAt( cursor, v );
				//randomAccess.setPosition( cursor );
				//Gradient3d.computeDerivativeVector3d( randomAccess, v );
				
				//norm( v );
												
				if ( length( v ) != 0 )
				{
					final float[] p = new float[ 3 ];
					
					p[ 0 ] = x + 0.5f;
					p[ 1 ] = y + 0.5f;
					p[ 2 ] = z + 0.5f;
					
					spot.candidates.add( new PointFunctionMatch( new OrientedPoint( p, v, 1 ) ) );
				}
			}
			
			spots.add( spot );
		}
		
		return spots;
	}
	
	public static void fitCandidates( final ArrayList< Spot > spots ) throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{
		for ( final Spot spot : spots )
			spot.center.fit( spot.candidates );
	}

	public static void fitInliers( final ArrayList< Spot > spots ) throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{
		for ( final Spot spot : spots )
			spot.center.fit( spot.inliers );
	}

	public static void ransac( final ArrayList< Spot > spots, final int iterations, final double maxError, final double inlierRatio ) throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{
		for ( final Spot spot : spots )
			ransac( spot, iterations, maxError, inlierRatio );
	}
	
	public static void ransac( final Spot spot, final int iterations, final double maxError, final double inlierRatio ) throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{
		spot.center.ransac( spot.candidates, spot.inliers, iterations, maxError, inlierRatio );
		spot.numRemoved = spot.candidates.size() - spot.inliers.size();
		
		if ( spot.inliers.size() >= spot.center.minNumPoints )
			spot.center.fit( spot.inliers );
	}
	
	public static <T extends RealType<T> > void drawRANSACArea( final ArrayList< Spot > spots, final Img< T > draw )
	{
		final int numDimensions = draw.numDimensions();
		double point = 1;
		final Random random = new Random( 34563646 );
		
		for ( final Spot spot : spots )
		{
			if ( spot.inliers.size() == 0 )
				continue;
			
			final RandomAccess< T > drawRA = draw.randomAccess();
			double rnd = (random.nextDouble() - 0.5) / 2.0;
			final float[] scale = spot.scale;
			
			for ( final PointFunctionMatch pm : spot.inliers )
			{
				final Point p = pm.getP1();
				
				for ( int d = 0; d < numDimensions; ++d )
					drawRA.setPosition( Math.round( p.getW()[ d ]/scale[ d ] ), d );
				
				drawRA.get().setReal( point + rnd );
			}
			//++point;
		}
	}
	
	public static double length( final float[] f )
	{
		double l = 0;
		
		for ( final float v : f )
			l += v*v;
		
		l = Math.sqrt( l );

		return l;
	}
	
	public static void norm( final float[] f )
	{
		double l = length( f );

		if ( l == 0 )
			return;
		
		for ( int i = 0; i < f.length; ++i )
			f[ i ] = (float)( f[ i ] / l );
	}	
}
