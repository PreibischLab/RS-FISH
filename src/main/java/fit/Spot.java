package fit;

import gradient.Gradient;

import java.util.ArrayList;
import java.util.Random;

import mpicbg.models.AbstractModel;
import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.Point;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RealLocalizable;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

public class Spot implements RealLocalizable
{
	public final SymmetryCenter< ? extends SymmetryCenter< ? > > center;// = new SymmetryCenter3d();
	public final ArrayList< PointFunctionMatch > candidates = new ArrayList<PointFunctionMatch>();
	public final ArrayList< PointFunctionMatch > inliers = new ArrayList<PointFunctionMatch>();
	public final float[] scale = new float[]{ 1, 1, 1 };
		
	public int numRemoved = -1;
	public double avgCost = -1, minCost = -1, maxCost = -1;

	// from where it was ran
	public int[] loc = null;

	public final int n;
	
	public Spot( final int n )
	{
		this.n = n;
		
		if ( n == 2 )
			center = new SymmetryCenter2d();
		else if ( n == 3 )
			center = new SymmetryCenter3d();
		else
			throw new RuntimeException( "only 2d and 3d is allowed." );
	}

	public void setOriginalLocation(int[] loc) { this.loc = loc; }
	public int[] getOriginalLocation() { return loc; }
	
	@Override
	public String toString()
	{
		String result = "center: ";
		
		for ( int d = 0; d < n; ++d )
			result += center.getSymmetryCenter( d )/scale[ d ] + " ";
		
		result += " Removed = " + numRemoved + "/" + candidates.size() + " error = " + minCost + ";" + avgCost + ";" + maxCost;
		
		return result;
	}
	
	public double[] getCenter()
	{
		final double[] c = new double[ n ];
		
		getCenter( c );
		
		return c; 
	}
	
	public void getCenter( final double[] c )
	{ 
		for ( int d = 0; d < n; ++d )
			c[ d ] = center.getSymmetryCenter( d ) / scale[ d ];
	}
	
	public double computeAverageCostCandidates() { return computeAverageCost( candidates ); }
	public double computeAverageCostInliers() { return computeAverageCost( inliers ); }

	public double computeAverageCost( final ArrayList< PointFunctionMatch > set )
	{
		if ( set.size() == 0 )
		{
			minCost = Double.MAX_VALUE;
			maxCost = Double.MAX_VALUE;
			avgCost = Double.MAX_VALUE;
			
			return avgCost;
		}
		
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


	public static ArrayList< Spot > extractSpots( final Img< FloatType > image, final ArrayList< int[] > peaks, final Gradient derivative, final int[] size )
	{
		//System.out.println( "Found " + peaks.size() + " peaks. " );
		
		final int numDimensions = image.numDimensions();
		
		// size around the detection to use
		// we detect at 0.5, 0.5, 0.5 - so we need an even size
		// final int[] size = new int[]{ 10, 10, 10 };
		
		final long[] min = new long[ numDimensions ];
		final long[] max = new long[ numDimensions ];

		// we always compute the location at 0.5, 0.5, 0.5 - so we cannot compute it at the last entry of each dimension
		
		final int[] maxDim = new int[ numDimensions ];

		for ( int d = 0; d < numDimensions; ++d )
			maxDim[ d ] = (int)image.dimension( d ) - 2;

		//final int w = (int)image.dimension( 0 ) - 2;
		//final int h = (int)image.dimension( 1 ) - 2;
		//final int d = (int)image.dimension( 2 ) - 2;
		
		final ArrayList< Spot > spots = new ArrayList<Spot>();		
		final RandomAccessible< FloatType > infinite = Views.extendZero( image );
		
		int i = 0;
		
		for ( final int[] peak : peaks )
		{	
			//if ( i++ % 1000 == 0 )
			//	System.out.println( "peak " + i + ": " + Util.printCoordinates( peak ) );
			
			final Spot spot = new Spot( numDimensions );
			spot.setOriginalLocation( peak );
			
			for ( int e = 0; e < numDimensions; ++e )
			{
				min[ e ] = peak[ e ] - size[ e ] / 2;
				max[ e ] = min[ e ] + size[ e ] - 1;
				
				// check that it does not exceed bounds of the underlying image
				min[ e ] = Math.max( min[ e ], 0 );
				max[ e ] = Math.min( max[ e ], maxDim[ e ] );
			}

			// define a local region to iterate around the potential detection
			final Cursor< FloatType > cursor = Views.iterable( Views.interval( infinite, min, max ) ).localizingCursor();
			
			while ( cursor.hasNext() )
			{
				cursor.fwd();
				
				/*
				final int x = cursor.getIntPosition( 0 );
				final int y = cursor.getIntPosition( 1 );
				final int z = cursor.getIntPosition( 2 );
				
				if ( x < 0 || y < 0 || z < 0 || x > w || y > h || z > d )
					continue;
				*/
				
				final float[] v = new float[ numDimensions ];
				
				derivative.gradientAt( cursor, v );
				
				//norm( v );
												
				if ( length( v ) != 0 )
				{
					final float[] p = new float[ numDimensions ];
					
					for ( int e = 0; e < numDimensions; ++e )
						p[ e ] = cursor.getIntPosition( e ) + 0.5f;
					
					/*
					p[ 0 ] = x + 0.5f;
					p[ 1 ] = y + 0.5f;
					p[ 2 ] = z + 0.5f;
					*/
					
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

	public static void ransac( final ArrayList< Spot > spots, final int iterations, final double maxError, final double inlierRatio )
	{
		// TODO: This is only to make it reproducible
		AbstractModel.resetRandom();

		for ( final Spot spot : spots )
		{
			try 
			{
				ransac( spot, iterations, maxError, inlierRatio );
			} 
			catch (NotEnoughDataPointsException e) 
			{
				spot.inliers.clear();
				spot.numRemoved = spot.candidates.size();
				System.out.println( "e1" );
			}
			catch (IllDefinedDataPointsException e)
			{
				spot.inliers.clear();
				spot.numRemoved = spot.candidates.size();
				System.out.println( "e2" );
			}
		}
	}
	
	public static void ransac( final Spot spot, final int iterations, final double maxError, final double inlierRatio ) throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{
		spot.center.ransac( spot.candidates, spot.inliers, iterations, maxError, inlierRatio );
		spot.numRemoved = spot.candidates.size() - spot.inliers.size();
		
		if ( spot.inliers.size() >= spot.center.getMinNumPoints() )
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

	@Override
	public int numDimensions() { return n; }

	@Override
	public void localize( final float[] position ) { center.getSymmetryCenter( position ); }

	@Override
	public void localize( final double[] position ) { center.getSymmetryCenter( position ); }

	@Override
	public float getFloatPosition( final int d ) { return (float)center.getSymmetryCenter( d ); }

	@Override
	public double getDoublePosition( final int d ) { return center.getSymmetryCenter( d ); }	
}
