package intensity;

import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.math3.stat.regression.SimpleRegression;

import fit.Spot;
import net.imglib2.Localizable;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.algorithm.localization.EllipticGaussianOrtho;
import net.imglib2.algorithm.localization.LevenbergMarquardtSolver;
import net.imglib2.algorithm.localization.MLEllipticGaussianEstimator;
import net.imglib2.algorithm.localization.PeakFitter;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

public class Intensity {

	// FIXME: This one is buggy!!! fix it before using
	public static void calulateIntesitiesGF(RandomAccessibleInterval<FloatType> xyz, int numDimensions,
			float anisotropy, double sigma, ArrayList<Spot> filteredSpots, ArrayList<Float> intensity) {
		double[] typicalSigmas = new double[numDimensions];
		for (int d = 0; d < numDimensions; d++)
			typicalSigmas[d] = sigma;

		// adjust 3d dimension if image is 3D
		if (numDimensions == 3)
			typicalSigmas[numDimensions - 1] *= anisotropy;

		PeakFitter<FloatType> pf = new PeakFitter<FloatType>(xyz, (ArrayList) filteredSpots,
				new LevenbergMarquardtSolver(), new EllipticGaussianOrtho(),
				new MLEllipticGaussianEstimator(typicalSigmas)); // use a non-symmetric gauss (sigma_x, sigma_y, sigma_z
																	// or sigma_xy & sigma_z)
		pf.process();

		// TODO: make spot implement Localizable - then this is already a HashMap that
		// maps Spot > double[]
		// this is actually a Map< Spot, double[] >
		final Map<Localizable, double[]> fits = pf.getResult();

		// FIXME: is the order consistent
		for (final Spot spot : filteredSpots) {
			double[] params = fits.get(spot);
			intensity.add(new Float(params[numDimensions]));
		}
	}

	public static void calculateIntensitiesLinear(RandomAccessibleInterval<FloatType> xyz,
			ArrayList<Spot> filteredSpots, ArrayList<Float> intensity) {
		// iterate over all points and perform the linear interpolation for each of the
		// spots
		// FIXME: the factory should depend on the imp > floatType, ByteType, etc.
		NLinearInterpolatorFactory<FloatType> factory = new NLinearInterpolatorFactory<>();
		RealRandomAccessible<FloatType> interpolant = Views.interpolate(Views.extendZero(xyz), factory);
		for (Spot fSpot : filteredSpots) {
			RealRandomAccess<FloatType> rra = interpolant.realRandomAccess();
			double[] position = fSpot.getCenter();
			rra.setPosition(position);
			intensity.add(new Float(rra.get().get()));
		}
	}

	// FIXME: the factory should depend on the imp > floatType, ByteType, etc.
	public static void fixIntensities(ArrayList<Spot> spots, ArrayList<Float> intensity) {
		boolean includeIntercept = true;
		SimpleRegression sr = new SimpleRegression(includeIntercept);
		// perform correction in 3D only
		int numDimensions = 3;

		for (int j = 0; j < spots.size(); ++j) {
			float z = spots.get(j).getFloatPosition(numDimensions - 1);
			float I = intensity.get(j);
			sr.addData(z, I);
		}

		double slope = sr.getSlope();
		double intercept = sr.getIntercept();

		double zMin = getZMin(spots, numDimensions);

		for (int j = 0; j < spots.size(); ++j) {
			double I = intensity.get(j);
			float z = spots.get(j).getFloatPosition(numDimensions - 1);
			double dI = linearFunc(zMin, slope, intercept) - linearFunc(z, slope, intercept);	
			intensity.set(j, (float) (I + dI));
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
