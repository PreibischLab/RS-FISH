package fitting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import background.NormalizedGradient;
import gradient.Gradient;
import ij.IJ;
import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.Model;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.Point;
import mpicbg.models.PointMatch;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.Localizable;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.iterator.IntervalIterator;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

/**
 * Radial Symmetry Package
 *
 * This software is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software.  If not, see http://www.gnu.org/licenses/.
 * 
 * @author Stephan Preibisch (stephan.preibisch@gmx.de) and Timothee Lionnet
 */
public class Spot implements RealLocalizable//, Localizable
{
	public final SymmetryCenter< ? extends SymmetryCenter< ? > > center;// = new SymmetryCenter3d();

	public final ArrayList< PointFunctionMatch > candidates = new ArrayList<>();
	public ArrayList< PointFunctionMatch > inliers = new ArrayList<>();
	public ArrayList<ArrayList< PointFunctionMatch >> multiConsensusMatches = new ArrayList<>();
	public final float[] scale = new float[]{ 1, 1, 1 };

	public int numRemoved = -1;
	public double avgCost = -1, minCost = -1, maxCost = -1;

	private double intensity = -1;
	// from where it was ran
	public final long[] loc;

	public final int n;

	public Spot( final long[] loc )
	{
		this.n = loc.length;
		this.loc = loc;

		if ( n == 2 )
			center = new SymmetryCenter2d();
		else if ( n == 3 )
			center = new SymmetryCenter3d();
		else
			throw new RuntimeException( "only 2d and 3d is allowed." );
	}

	public float[] getScale() { return scale; }
	public double getIntensity() { return intensity; }
	public float getFloatIntensity() { return (float)intensity; }
	public void setIntensity( final double intensity ) { this.intensity = intensity; }
	public long[] getOriginalLocation() { return loc; }

	@Override
	public String toString()
	{
		String result = "center: ";

		for ( int d = 0; d < n; ++d )
			result += center.getSymmetryCenter( d )/scale[ d ] + " ";

		result += " Removed = " + numRemoved + "/" + candidates.size() + " error = " + minCost + ";" + avgCost + ";" + maxCost;

		return result;
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
			final double d = pm.getDistance();

			avgCost += d;
			minCost = Math.min( d, minCost );
			maxCost = Math.max( d, maxCost );
		}

		avgCost /= set.size();

		return avgCost;
	}

	public void updateScale( final float[] scale )
	{
		for ( final PointFunctionMatch pm : candidates )
		{
			final OrientedPoint p = (OrientedPoint)pm.getP1();

			final double[] l = p.getL();
			final double[] w = p.getW();
			final double[] ol = p.getOrientationL();
			final double[] ow = p.getOrientationW();

			for ( int d = 0; d < l.length; ++d )
			{
				// is this is only needed to have the point also in isotropic space?
				// Answer: no, it is not, but we need to re-scale the center location as well
				w[ d ] = l[ d ] * scale[ d ];
				ow[ d ] = ol[ d ] / scale[ d ];
			}
		}

		for ( int d = 0; d < scale.length; ++d )
			this.scale[ d ] = scale[ d ];
	}

	public static <T extends RealType<T> > ArrayList< Spot > extractSpots(
			final Interval image,
			final ArrayList< long[] > peaks,
			final Gradient derivative,
			final long[] size )
	{
		return extractSpots( image, peaks, derivative, null, size );
	}

	// searches for spots in the image which is the part of the fullImage
	/**
	 * 
	 * @param interval - where to extract the spots from (must be the interval from which the gradient was computed)
	 * @param peaks - the peaks for which to extract a spot (that includes the gradients)
	 * @param derivative - the derivative image
	 * @param normalizer - potential per-spot normalization of the derivative image (can be null)
	 * @param spotSize - the support region for each spot
	 * @param <T> - type
	 * 
	 * @return list of spots
	 */
	public static <T extends RealType<T> > ArrayList< Spot > extractSpots(
			final Interval interval,
			final ArrayList< long[] > peaks,
			final Gradient derivative,
			final NormalizedGradient normalizer,
			final long[] spotSize )
	{
		final ArrayList< Spot > spots = new ArrayList<>();

		for ( final long[] peak : peaks )
			spots.add( extractSpot(interval, peak, derivative, normalizer, spotSize ) );

		return spots;
	}

	public static <T extends RealType<T> > Spot extractSpot(
			final Interval interval,
			final long[] peak,
			final Gradient derivative,
			final NormalizedGradient normalizer,
			final long[] spotSize )
	{
		// size around the detection to use (spotSize)
		// we detect at 0.5, 0.5, 0.5 - so we need an even size (as 2 pixels have been condensed into 1 when computing the gradient)
		// no gradient: 0-1-2 (to compute something for 1 we need three pixels)
		// gradient: 0,5-1,5 (to compute something for 1 we need two pixels, because pixel 0 is actually at position 0,5 and pixel 1 at position 1,5 --- 0,5-pixel-shift)

		final int numDimensions = derivative.numDimensions();
		final long[] min = new long[ numDimensions ];
		final long[] max = new long[ numDimensions ];

		final Gradient gradient;

		if ( normalizer == null )
			gradient = derivative;
		else
			gradient = normalizer;

		final Spot spot = new Spot( peak );

		// this part defines the possible values		
		for ( int e = 0; e < numDimensions; ++e )
		{
			min[ e ] = peak[ e ] - spotSize[ e ]/2;
			max[ e ] = min[ e ] + spotSize[ e ] - 1;

			// check that it does not exceed bounds of the underlying image
			min[ e ] = Math.max( min[ e ], interval.min( e ) );
			
			// we always compute the location at 0.5, 0.5, 0.5 - so we cannot compute it at the last entry of each dimension
			// 0 would be the image, -1 is the gradient image as we loose one value computing the gradient
			max[ e ] = Math.min( max[ e ], interval.max( e ) - 1 ); 
		}

		final FinalInterval spotInterval = new FinalInterval( min, max );

		if ( normalizer != null )
			((NormalizedGradient)gradient).normalize( spotInterval );

		// define a local region to iterate around the potential detection
		final IntervalIterator cursor = new IntervalIterator( spotInterval );

		while ( cursor.hasNext() )
		{
			cursor.fwd();

			final double[] v = new double[ numDimensions ];
			gradient.gradientAt( cursor, v );
			
			if ( length( v ) != 0 )
			{
				final double[] p = new double[ numDimensions ];

				for ( int e = 0; e < numDimensions; ++e )
					p[ e ] = cursor.getIntPosition( e ) + 0.5; // we add 0,5 to correct for the half-pixel-shift of the gradient image (because pixel 0 is actually at position 0,5 and pixel 1 at position 1,5)
				
				spot.candidates.add( new PointFunctionMatch( new OrientedPoint( p, v, 1 ) ) );
			}
		}

		return spot;
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
	
	/* this functions are working on one separate spot */
	public static void fitCandidates( final Spot spot) throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{
		spot.center.fit( spot.candidates );
	}

	public static void fitInliers( final Spot spot ) throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{
		spot.center.fit( spot.inliers );
	}

	public static ArrayList< Spot > ransac(
			final ArrayList< Spot > spots,
			final int iterations,
			final double maxError,
			final double inlierRatio,
			int minNumInliers,
			final boolean multiConsenus, // all variables below are to instantiate new spots
			final double nTimesStDev1,
			final double nTimesStDev2,
			final Interval interval,
			final Gradient derivative,
			final NormalizedGradient normalizer,
			final long[] spotSize )
	{
		return ransac(spots, iterations, maxError, inlierRatio, minNumInliers, multiConsenus, nTimesStDev1, nTimesStDev2, interval, derivative, normalizer, spotSize, false );
	}

	public static ArrayList< Spot > ransac(
			final ArrayList< Spot > spots,
			final int iterations,
			final double maxError,
			final double inlierRatio,
			int minNumInliers,
			final boolean multiConsenus, // all variables below are to instantiate new spots
			final double nTimesStDev1,
			final double nTimesStDev2,
			final Interval interval,
			final Gradient derivative,
			final NormalizedGradient normalizer,
			final long[] spotSize,
			final boolean silent )
	{
		// TODO: This is only to make it reproducible
		//AbstractModel.resetRandom();

		ArrayList< Spot > additionalSpots = new ArrayList<>();

		long min = Long.MAX_VALUE;
		long max = Long.MIN_VALUE;
		long sum = 0;
		ArrayList< Integer > inlierCount = new ArrayList<>();

		for ( final Spot spot : spots )
		{
			try 
			{
				ransac( spot, iterations, maxError, inlierRatio, multiConsenus, minNumInliers );
			} 
			catch (NotEnoughDataPointsException e) 
			{
				spot.inliers.clear();
				spot.numRemoved = spot.candidates.size();
				System.out.println( "Spot e1 " + spot.candidates.size() + ": " + e);
			}
			catch (IllDefinedDataPointsException e)
			{
				spot.inliers.clear();
				spot.numRemoved = spot.candidates.size();
				System.out.println( "Spot e2: " + e );
			}

			if ( spot.inliers.size() > 0 )
			{
				min = Math.min( min, spot.inliers.size() );
				max = Math.max( max, spot.inliers.size() );
				sum += spot.inliers.size();
				inlierCount.add( spot.inliers.size() );
			}
		}

		if ( inlierCount.size() > 0 )
		{
			double avg = sum/(double)inlierCount.size();	
			double stdev = 0;

			for ( final int v : inlierCount )
				stdev += ( v - avg )*( v - avg );

			stdev = Math.sqrt( stdev / (double)( inlierCount.size() - 1 ) );

			if ( !silent )
			{
				IJ.log( "min #inliers=" + min );
				IJ.log( "max #inliers=" + max );
				IJ.log( "average #inliers=" + avg );
				IJ.log( "stdev #inliers=" + stdev );
			}

			// select extra consensus areas that might be spots
			if ( multiConsenus )
			{
				final long thr1 = Math.round( avg - nTimesStDev1 * stdev );
				final long thr2 = Math.round( avg - nTimesStDev2 * stdev );

				if ( !silent )
				{
					IJ.log( "Finding additional spots ... " );
					IJ.log( "MultiConsensus initial threshold #inliers=" + thr1 );
					IJ.log( "MultiConsensus final threshold #inliers=" + thr2 );
				}

				final ArrayList< Spot > newSpots = new ArrayList<>();
	
				for ( final Spot spot : spots )
				{
					for ( final ArrayList< PointFunctionMatch > list : spot.multiConsensusMatches )
					{
						if ( list.size() >= thr1 )
						{
							long[] pos = new long[ spot.getOriginalLocation().length ];
	
							try
							{
								spot.center.fit( list );
	
								for ( int d = 0; d < pos.length; ++d )
									pos[ d ] = Math.round( spot.getDoublePosition( d ) );
	
								if ( !pointExists(pos, newSpots))
								{
									Spot newSpot = extractSpot( interval, pos, derivative, normalizer, spotSize );
	
									minNumInliers = (int)Math.max( minNumInliers, Math.round( newSpot.candidates.size() * inlierRatio ) );
	
									long [] pN = new long[ pos.length ];
	
									// remove any candiadates that were already used as inliers
									for ( int i = newSpot.candidates.size() - 1; i >= 0; --i )
									{
										PointFunctionMatch pfN = newSpot.candidates.get( i );
										for ( int d = 0; d < pos.length; ++d )
											pN[ d ] = Math.round( pfN.getP1().getL()[ d ] - 0.5 );
	
										boolean foundIdentical = false;
	
										for ( final Spot s : spots )
										{
											for ( final PointFunctionMatch p : s.inliers )
											{
												boolean same = true;
	
												for ( int d = 0; d < pos.length && same; ++d )
													if ( pN[ d ] != Math.round( p.getP1().getL()[ d ] - 0.5 ) )
														same = false;
	
												if ( same == true )
												{
													foundIdentical = true;
													break;
												}
											}
	
											if ( foundIdentical )
												break;
										}
	
										if ( foundIdentical )
											newSpot.candidates.remove( i );
									}
	
									newSpot.updateScale( spot.getScale() );
	
									System.out.println( "new candidate for: " + Util.printCoordinates( spot.getOriginalLocation() ) + "@" + Util.printCoordinates( pos ) + ": " + list.size() );
	
									ransac( newSpot, iterations, maxError, inlierRatio, false, minNumInliers );
									System.out.println( newSpot.inliers.size() + " from " + list.size() );
	
									// Ad-hoc criteria that the inliers either increase by 33% or that they reach the range of the normal spots
									if ( newSpot.inliers.size() >= minNumInliers && newSpot.inliers.size() >= thr2 )
									{
										newSpot.center.fit(newSpot.inliers);
										additionalSpots.add( newSpot );
										System.out.println( newSpot.inliers.size() + " --" + Util.printCoordinates( newSpot.localize() ) );
									}
								}
							}
							catch (NotEnoughDataPointsException | IllDefinedDataPointsException e)
							{
							}
	
							try
							{
								//re-fit as we might have destroyed locations
								if ( spot.inliers.size() >= spot.center.getMinNumPoints() )
									spot.center.fit( spot.inliers );
							}
							catch (NotEnoughDataPointsException | IllDefinedDataPointsException e)
							{
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
				}
			}
		}
		else
		{
			if (!silent )
				IJ.log( "No spots remaining after RANSAC." );
		}

		return additionalSpots;
	}

	protected static boolean pointExists( final long[] loc, final Collection< Spot > spots )
	{
		for ( final Spot spot : spots )
		{
			boolean equal = true;

			for ( int d = 0; d < spot.getOriginalLocation().length && equal; ++d )
				if ( loc[ d ] != spot.getOriginalLocation()[ d ] )
					equal = false;

			if ( equal == true )
				return true;
		}

		return false;
	}

	public static void ransac( final Spot spot, final int iterations, final double maxError, final double inlierRatio, final boolean multiConsenus, final int minNumInliers ) throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{
		if ( multiConsenus )
		{
			//System.out.println( Util.printCoordinates( spot.getOriginalLocation() ));

			spot.numRemoved = spot.candidates.size();

			MultiConsensusFilter filter = new MultiConsensusFilter( spot, iterations, maxError, inlierRatio, minNumInliers, false );
			ArrayList<ArrayList<PointFunctionMatch>> allMatches = filter.filter( spot.candidates );

			if ( allMatches == null || allMatches.size() == 0 )
			{
				spot.inliers = new ArrayList<>();
			}
			else
			{
				spot.inliers = allMatches.get( 0 );
				for ( int i = 1; i < allMatches.size(); ++i )
					spot.multiConsensusMatches.add( allMatches.get( i ) );
			}

			spot.numRemoved -= spot.inliers.size();
		}
		else
		{
			spot.center.ransac( spot.candidates, spot.inliers, iterations, maxError, inlierRatio,  minNumInliers );
			spot.numRemoved = spot.candidates.size() - spot.inliers.size();

			//System.out.println( Util.printCoordinates( spot.getOriginalLocation() ));
			//spot.center.fit(spot.inliers);
			//System.out.println( spot.inliers.size() + " --" + Util.printCoordinates( spot.getCenter() ) );
		}

		if ( spot.inliers.size() >= spot.center.getMinNumPoints() )
			spot.center.fit( spot.inliers );
	}

	public static class MultiConsensusFilter
	{
		final private Spot spot;
		final private int numIterations;
		final private double maxEpsilon;
		final private double minInlierRatio;
		final private int minNumInliers;
		final private boolean silent;

		public MultiConsensusFilter(
				final Spot spot,
				final int numIterations,
				final double maxEpsilon,
				final double minInlierRatio,
				final int minNumInliers,
				final boolean silent ) {

			this.spot = spot;
			this.numIterations = numIterations;
			this.maxEpsilon = maxEpsilon;
			this.minInlierRatio = minInlierRatio;
			this.minNumInliers = minNumInliers;
			this.silent = silent;
		}

		public  < P extends PointMatch > ArrayList<ArrayList<P>> filterMultiConsensusSets(final List<P> candidates) {

			final ArrayList<ArrayList<P>> inliers = new ArrayList<>();

			boolean modelFound = true;
			do {
				final ArrayList<P> modelInliers = new ArrayList<>();
				try {
					modelFound = spot.center.filterRansac(
							candidates,
							modelInliers,
							numIterations,
							maxEpsilon,
							minInlierRatio,
							minNumInliers,
							4f);
				}
				catch (final NotEnoughDataPointsException e) {
					modelFound = false;
				}

				if (modelFound) {
					inliers.add(modelInliers);
					candidates.removeAll(modelInliers);
				}
			} while (modelFound);

			return inliers;
		}

		public < P extends PointMatch > ArrayList<ArrayList<P>> filter(final List<P> candidates) {

			//final ArrayList<P> inliers = new ArrayList<>();
			final ArrayList<ArrayList<P>> multiConsensusSets = filterMultiConsensusSets(candidates);

			/*
			for ( int i = 0; i < multiConsensusSets.size(); ++i )
			{
				ArrayList<P> consensusSet = multiConsensusSets.get( i );
				inliers.addAll(consensusSet);
				try
				{
					spot.center.fit(consensusSet);

					final double[] original = new double[ spot.numDimensions() ];
					for ( int d = 0; d < spot.numDimensions(); ++d )
						original[ d ] = spot.getOriginalLocation()[ d ];

					double distance = Point.distance( new Point( spot.localize() ), new Point( original ) );
					System.out.println( consensusSet.size() + " --" + Util.printCoordinates( spot.localize() ) + ", " + distance );
				}
				catch (NotEnoughDataPointsException | IllDefinedDataPointsException e) {}
			}

			if ( !silent )
			{
				System.out.printf("Found %d consensus sets with %d inliers", multiConsensusSets.size(), inliers.size());
				System.out.println();
			}
			*/

			return multiConsensusSets;//.get( 0 );
			//return inliers;
		}
	}

	public static <T extends RealType<T> > void drawRANSACArea(
			final List< Spot > spots,
			final RandomAccessibleInterval< T > draw,
			final boolean encodeError )
	{
		final int numDimensions = draw.numDimensions();
		final RandomAccessible<T> drawOobs = Views.extendZero( draw );

		final long[] min = new long[ numDimensions ];
		final long[] max = new long[ numDimensions ];

		// sum of contributing pixels for one gradient == 1
		final double valuePerPixel = 1.0 / Math.pow( 2, numDimensions );

		for ( final Spot spot : spots )
		{
			if ( spot.inliers.size() == 0 )
				continue;
			// Using extension because sometimes spots are not fully inside of the image
			// final RandomAccess< T > drawRA =  Views.extendZero(draw).randomAccess();
			// final double[] scale = spot.scale;

			for ( final PointFunctionMatch pm : spot.inliers )
			{
				final Point p = pm.getP1();
				for ( int d = 0; d < numDimensions; ++d )
				{
					// the gradient sits at the 0.5, 0.5 ... location using its 8-neigborhood
					// to be computed. So we set the whole 8-neighborhood to be part of the 
					// computation
					//
					// we use getL because getW might be scaled because of anisotropy
					min[ d ] = Math.round( Math.floor( p.getL()[ d ] ) );
					max[ d ] = Math.round( Math.ceil( p.getL()[ d ] ) );
				}

				// set the intensity 
				for ( final T type : Views.iterable( Views.interval( drawOobs, min, max ) ) )
				{
					// set color to error value
					if ( encodeError )
						type.setReal( type.getRealDouble() + valuePerPixel * pm.getDistance() );
					else
						type.setReal( type.getRealDouble() + valuePerPixel );
				}
			}
		}
	}
	
	// plot inliers for the given spot 
	public static <T extends RealType<T> > void showInliers(final ArrayList< Spot > spots, final RandomAccessibleInterval< T > draw, double threshold){
		final int numDimensions = draw.numDimensions();

		for ( final Spot spot : spots )
		{
			if ( spot.inliers.size() == 0 )
				continue;
			// Using extension because sometimes spots are not fully inside of the image
			final RandomAccess< T > drawRA =  Views.extendMirrorSingle(draw).randomAccess();
			final float[] scale = spot.getScale();

			for ( final PointFunctionMatch pm : spot.candidates )
			{
				final Point p = pm.getP1();
				for ( int d = 0; d < numDimensions; ++d )
					drawRA.setPosition( Math.round( p.getW()[ d ]/scale[ d ] ), d );
				// set color to error value
				drawRA.get().setReal(pm.getDistance() > threshold ? 1 : 0 );
			}
		}
		
	}

	public static double length( final double[] f )
	{
		double l = 0;

		for ( final double v : f )
			l += v*v;

		l = Math.sqrt( l );

		return l;
	}

	public static void norm( final double[] f )
	{
		double l = length( f );

		if ( l == 0 )
			return;

		for ( int i = 0; i < f.length; ++i )
			f[ i ] = ( f[ i ] / l );
	}
	
	public ArrayList<PointFunctionMatch> getInliers(){ return inliers; }

	public ArrayList<PointFunctionMatch> getCandidates(){ return candidates; }
	
	@Override
	public int numDimensions() { return n; }

	public double[] localize()
	{
		final double[] center = new double[ n ];
		localize( center );
		return center;
	}

	@Override
	public void localize( final float[] position )
	{
		for ( int d = 0; d < n; ++d )
			position[ d ] = (float)getDoublePosition( d );
	}

	@Override
	public void localize( final double[] position )
	{
		for ( int d = 0; d < n; ++d )
			position[ d ] = getDoublePosition( d );
	}

	@Override
	public float getFloatPosition( final int d ) { return (float)getDoublePosition( d ); }

	@Override
	public double getDoublePosition( final int d ) { return center.getSymmetryCenter( d ) / scale[ d ]; }

	/*
	@Override
	public void localize(final int[] position)
	{
		for ( int d = 0; d < n; ++d )
			position[ d ] = (int)loc[ d ];
	}

	@Override
	public void localize(long[] position)
	{
		for ( int d = 0; d < n; ++d )
			position[ d ] = loc[ d ];
	}

	@Override
	public int getIntPosition( final int d ) { return (int)loc[ d ]; }

	@Override
	public long getLongPosition( final int d ) { return loc[ d ]; }
	*/

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(loc);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Spot other = (Spot) obj;
		if (!Arrays.equals(loc, other.loc))
			return false;
		return true;
	}
}
