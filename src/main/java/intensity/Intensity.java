/*-
 * #%L
 * A plugin for radial symmetry localization on smFISH (and other) images.
 * %%
 * Copyright (C) 2016 - 2023 Developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */
package intensity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.math3.stat.regression.SimpleRegression;

import fit.PointFunctionMatch;
import fitting.Spot;
import gui.interactive.HelperFunctions;
import net.imglib2.FinalDimensions;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.Localizable;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.converter.Converters;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import util.localization.EllipticGaussianOrtho;
import util.localization.GenericPeakFitter;
import util.localization.LevenbergMarquardtSolver;
import util.localization.LocalizationUtils;
import util.localization.MLEllipticGaussianEstimator;
import util.localization.SparseObservationGatherer;

public class Intensity {

	public static class WrappedSpot implements Localizable
	{
		final Spot spot;
		final long[] loc;

		public WrappedSpot( final Spot spot )
		{
			this.spot = spot;
			this.loc = new long[ spot.numDimensions() ];
			for ( int d = 0; d < spot.numDimensions(); ++d )
				loc[ d ] = Math.round( spot.getDoublePosition( d ) );
		}

		public Spot getSpot() { return spot; }

		@Override
		public int numDimensions() { return spot.numDimensions(); }

		@Override
		public long getLongPosition( final int d ) { return loc[ d ]; }

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + Arrays.hashCode(loc);
			result = prime * result + ((spot == null) ? 0 : spot.hashCode());
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
			WrappedSpot other = (WrappedSpot) obj;
			if (!Arrays.equals(loc, other.loc))
				return false;
			if (spot == null) {
				if (other.spot != null)
					return false;
			} else if (!spot.equals(other.spot))
				return false;
			return true;
		}
	}

	public static Interval coveredArea( final Collection<Spot> filteredSpots, final int spotRadius, final int numDimensions )
	{
		final long[] min = new long[ numDimensions ];
		final long[] max = new long[ numDimensions ];

		for ( int d = 0; d < numDimensions; ++d )
		{
			min[ d ] = Long.MAX_VALUE;
			max[ d ] = Long.MIN_VALUE;
		}

		// compute min & max for background subtraction
		for ( final Spot spot : filteredSpots )
		{
			for ( int d = 0; d < numDimensions; ++d )
			{
				final long loc = Math.round( spot.getDoublePosition( d ) );

				min[d] = Math.min( min[d], loc );
				max[d] = Math.max( max[d], loc );
			}
		}

		return Intervals.expand( new FinalInterval(min, max), spotRadius + 1 );
	}

	public static void calulateIntesitiesGF(
			final RandomAccessible<FloatType> xyz,
			final int numDimensions,
			final double anisotropy,
			final double sigma,
			final ArrayList<Spot> filteredSpots,
			final int spotRadius,
			final int ransacSelection )
	{
		double[] typicalSigmas = new double[numDimensions];
		for (int d = 0; d < numDimensions; d++)
			typicalSigmas[d] = sigma;
		// adjust 3d dimension if image is 3D
		if (numDimensions == 3)
			typicalSigmas[numDimensions - 1] *= anisotropy;

		final Interval interval = coveredArea( filteredSpots, spotRadius, numDimensions );

		HelperFunctions.log( "Temporarily removing background (gauss fit requires empty bg)..." );
		
		final RandomAccessibleInterval< FloatType > tmp = Views.translate( ArrayImgs.floats( new FinalDimensions( interval ).dimensionsAsLongArray() ), interval.minAsLongArray() );
		Gauss3.gauss( 10, xyz, tmp );

		final RandomAccessible< FloatType > bg =
				Converters.convert( xyz, Views.extendZero( tmp ), (i1,i2,o) -> { o.set( Math.max( 0, i1.get() - i2.get() ) ); }, new FloatType() );

		//ImageJFunctions.show( Views.interval( xyz, interval)).setTitle("xyz");;
		//ImageJFunctions.show( tmp).setTitle("tmp");;
		//ImageJFunctions.show( Views.interval( tmp, interval)).setTitle("tmp2");;
		//ImageJFunctions.show( Views.interval( bg, interval)).setTitle("bg");;

		final SparseObservationGatherer<FloatType> sparseObservationGatherer = new SparseObservationGatherer<>(bg, ransacSelection);

		final List< WrappedSpot > wrapped = filteredSpots.stream().map( spot -> new WrappedSpot( spot ) ).collect( Collectors.toList() );

		final GenericPeakFitter< FloatType, WrappedSpot > pf =
				new GenericPeakFitter< FloatType, WrappedSpot >(
						sparseObservationGatherer,
						wrapped,
						new LevenbergMarquardtSolver(),
						new EllipticGaussianOrtho(),
						new MLEllipticGaussianEstimator(typicalSigmas) );

		pf.setNumThreads( 1 );
		pf.process();

		final Map<WrappedSpot, double[]> fits = pf.getResult();

		// add back the background
		final RealRandomAccess<FloatType> rra = Views.interpolate( Views.extendMirrorSingle( tmp ), new NLinearInterpolatorFactory<>() ).realRandomAccess();

		for (final WrappedSpot spot : wrapped)
		{
			final double[] loc = fits.get( spot );

			for ( int d = 0; d < numDimensions; ++d )
				rra.setPosition( loc[ d ], d );

			//System.out.println( spot.getSpot().getIntensity() + " >>> " + (fits.get(spot)[numDimensions] + rra.get().get()) );
			spot.getSpot().setIntensity(fits.get(spot)[numDimensions] + rra.get().get() );
			//if ( location )
			//	spot.getSpot().localize(position);
			//System.out.println( Util.printCoordinates( loc ) );
		}
	}

	public static void calulateIntesitiesIntegrate(
			final RandomAccessible<FloatType> xyz,
			final ArrayList<Spot> filteredSpots,
			final int numDimensions )
	{
		final RealRandomAccess<FloatType> rra = Views.interpolate(xyz, new NLinearInterpolatorFactory<>()).realRandomAccess();

		for ( final Spot spot : filteredSpots )
		{
			double sum = 0;

			// can't use inliers as the number of pixels (and which pixels) always changes
			for ( final PointFunctionMatch pm : spot.getCandidates() )
			{
				rra.setPosition( pm.getP1().getL() );
				sum += rra.get().getRealDouble();
			}

			// sum = ( sum / spot.getInliers().size() ) * spot.getCandidates().size();

			// normalize by the number of pixels at least
			sum /= spot.getCandidates().size();

			spot.setIntensity( sum );
		}
	}

	public static void calculateIntensitiesLinear(
			final RandomAccessible<FloatType> xyz,
			final ArrayList<Spot> filteredSpots) {
		// iterate over all points and perform the linear interpolation for each
		// of the spots
		// FIXME: the factory should depend on the imp > floatType, ByteType,
		// etc.
		final NLinearInterpolatorFactory<FloatType> factory = new NLinearInterpolatorFactory<>();
		final RealRandomAccessible<FloatType> interpolant = Views.interpolate(xyz, factory);
		final RealRandomAccess<FloatType> rra = interpolant.realRandomAccess();
		for (final Spot fSpot : filteredSpots) {
			rra.setPosition(fSpot);
			fSpot.setIntensity(rra.get().get());
		}
	}

	// FIXME: the factory should depend on the imp > floatType, ByteType, etc.
	public static void fixIntensities(ArrayList<Spot> spots) {
		boolean includeIntercept = true;
		SimpleRegression sr = new SimpleRegression(includeIntercept);
		// perform correction in 3D only
		int numDimensions = 3;

		for (int j = 0; j < spots.size(); ++j) {
			float z = spots.get(j).getFloatPosition(numDimensions - 1);
			float I = spots.get(j).getFloatIntensity();
			sr.addData(z, I);
		}

		double slope = sr.getSlope();
		double intercept = sr.getIntercept();

		double zMin = getZMin(spots, numDimensions);

		for ( final Spot spot : spots ){
			double I = spot.getIntensity();
			float z = spot.getFloatPosition(numDimensions - 1);
			double dI = linearFunc(zMin, slope, intercept) - linearFunc(z, slope, intercept);
			spot.setIntensity(I + dI);
		}

	}

	// return the minimum z
	public static double getZMin(ArrayList<Spot> spots, int numDimensions) {
		double zMin = Double.MAX_VALUE;

		for (Spot spot : spots)
			if (zMin > spot.getDoublePosition(numDimensions - 1))
				zMin = spot.getDoublePosition(numDimensions - 1);
		return zMin;
	}

	// return y for y = kx + b
	public static double linearFunc(double x, double k, double b) {
		return k * x + b;
	}

	public static void main( String[] args )
	{
		
		// Creating a list of Integers
        List<Integer> list = Arrays.asList(3, 6, 9, 12, 15);
  
        // Using Stream map(Function mapper) and
        // displaying the corresponding new stream
        list.stream().map(number -> number/3).forEach(System.out::println);
	}
}
