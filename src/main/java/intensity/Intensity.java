package intensity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import org.apache.commons.math3.stat.regression.SimpleRegression;

import fitting.Spot;
import milkyklim.algorithm.localization.EllipticGaussianOrtho;
import milkyklim.algorithm.localization.GenericPeakFitter;
import milkyklim.algorithm.localization.LevenbergMarquardtSolver;
import milkyklim.algorithm.localization.MLEllipticGaussianEstimator;
import milkyklim.algorithm.localization.SparseObservationGatherer;
import net.imglib2.Localizable;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

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
			return result;
		}
	}

	public static void calulateIntesitiesGF(RandomAccessible<FloatType> xyz, int numDimensions,
			double anisotropy, double sigma, ArrayList<Spot> filteredSpots) {
		double[] typicalSigmas = new double[numDimensions];
		for (int d = 0; d < numDimensions; d++)
			typicalSigmas[d] = sigma;
		// adjust 3d dimension if image is 3D
		if (numDimensions == 3)
			typicalSigmas[numDimensions - 1] *= anisotropy;

		SparseObservationGatherer<FloatType> sparseObservationGatherer = new SparseObservationGatherer<>(xyz);
		// use a non-symmetric gauss (sigma_x, sigma_y, sigma_z or sigma_xy &
		// sigma_z)
		final ArrayList< WrappedSpot > wrapped = new ArrayList<>();
		filteredSpots.forEach( spot -> wrapped.add( new WrappedSpot( spot ) ) );

		GenericPeakFitter< FloatType, WrappedSpot > pf =
				new GenericPeakFitter< FloatType, WrappedSpot >(
						sparseObservationGatherer,
						wrapped,
						new LevenbergMarquardtSolver(),
						new EllipticGaussianOrtho(),
						new MLEllipticGaussianEstimator(typicalSigmas) );

		pf.process();

		final Map<WrappedSpot, double[]> fits = pf.getResult();

		// FIXME: is the order consistent
		for (final WrappedSpot spot : wrapped)
			spot.getSpot().setIntensity(fits.get(spot)[numDimensions]);
	}

	public static void calculateIntensitiesLinear(
			RandomAccessible<FloatType> xyz,
			ArrayList<Spot> filteredSpots) {
		// iterate over all points and perform the linear interpolation for each
		// of the spots
		// FIXME: the factory should depend on the imp > floatType, ByteType,
		// etc.
		NLinearInterpolatorFactory<FloatType> factory = new NLinearInterpolatorFactory<>();
		RealRandomAccessible<FloatType> interpolant = Views.interpolate(xyz, factory);
		RealRandomAccess<FloatType> rra = interpolant.realRandomAccess();
		for (Spot fSpot : filteredSpots) {
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
