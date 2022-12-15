package intensity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math3.stat.regression.SimpleRegression;

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

		final long[] min = new long[ numDimensions ];
		final long[] max = new long[ numDimensions ];

		for ( int d = 0; d < numDimensions; ++d )
		{
			min[ d ] = Long.MAX_VALUE;
			max[ d ] = Long.MIN_VALUE;
		}

		// use a non-symmetric gauss (sigma_x, sigma_y, sigma_z or sigma_xy & sigma_z)
		final ArrayList< WrappedSpot > wrapped = new ArrayList<>();
		final HashMap<WrappedSpot, Spot> lookup = new HashMap<>();

		// compute min & max for background subtraction
		for ( int i = 0; i < filteredSpots.size(); ++i )
		{
			final Spot spot = filteredSpots.get( i );
			final WrappedSpot wSpot = new WrappedSpot( spot );
			wrapped.add( wSpot );
			lookup.put( wSpot, spot );

			for ( int d = 0; d < numDimensions; ++d )
			{
				min[d] = Math.min( min[d], wSpot.loc[ d ] );
				max[d] = Math.max( max[d], wSpot.loc[ d ] );
			}
		}

		final Interval interval = Intervals.expand( new FinalInterval(min, max), spotRadius + 1 );

		HelperFunctions.log( "Removing background (gauss fit required empty bg)..." );
		RandomAccessibleInterval< FloatType > tmp = Views.translate( ArrayImgs.floats( new FinalDimensions( interval ).dimensionsAsLongArray() ), interval.minAsLongArray() );
		Gauss3.gauss( 10, xyz, tmp );

		final RandomAccessible< FloatType > bg =
				Converters.convert( xyz, Views.extendZero( tmp ), (i1,i2,o) -> { o.set( Math.max( 0, i1.get() - i2.get() ) ); }, new FloatType() );

		//ImageJFunctions.show( Views.interval( xyz, interval)).setTitle("xyz");;
		//ImageJFunctions.show( tmp).setTitle("tmp");;
		//ImageJFunctions.show( Views.interval( tmp, interval)).setTitle("tmp2");;
		//ImageJFunctions.show( Views.interval( bg, interval)).setTitle("bg");;
		final SparseObservationGatherer<FloatType> sparseObservationGatherer = new SparseObservationGatherer<>(bg, ransacSelection);

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

}
