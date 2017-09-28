package intensity;

import java.util.ArrayList;
import java.util.Map;

import net.imglib2.Localizable;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.algorithm.localization.EllipticGaussianOrtho;
import net.imglib2.algorithm.localization.LevenbergMarquardtSolver;
import net.imglib2.algorithm.localization.MLEllipticGaussianEstimator;
import net.imglib2.algorithm.localization.PeakFitter;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import fit.Spot;
import ij.ImagePlus;
import ij.ImageStack;
import parameters.RadialSymmetryParameters;

public class Intensity {

	// FIXME: This one is buggy!!! fix it before using
	public static void calulateIntesitiesGF(ImagePlus imp, int numDimensions, float anisotropy,
			double sigma, ArrayList<Spot> filteredSpots, ArrayList<Float> intensity){
		double [] typicalSigmas = new double[numDimensions];
		for (int d = 0; d < numDimensions; d++)
			typicalSigmas[d] = sigma;

		// adjust 3d dimension if image is 3D
		if (numDimensions == 3) typicalSigmas[numDimensions - 1] *= anisotropy;

		ImagePlus xyzImp = getXyz(imp); // grabbed the non-normalized xyz-stack  

		PeakFitter<FloatType> pf = new PeakFitter<FloatType>(ImageJFunctions.wrap(xyzImp), (ArrayList)filteredSpots,
				new LevenbergMarquardtSolver(), new EllipticGaussianOrtho(), 
				new MLEllipticGaussianEstimator(typicalSigmas)); // use a non-symmetric gauss (sigma_x, sigma_y, sigma_z or sigma_xy & sigma_z)
		pf.process();

		// TODO: make spot implement Localizable - then this is already a HashMap that maps Spot > double[]
		// this is actually a Map< Spot, double[] >
		final Map< Localizable, double[] > fits = pf.getResult();

		// FIXME: is the order consistent
		for ( final Spot spot : filteredSpots )
		{
			double[] params = fits.get( spot );
			intensity.add(new Float(params[numDimensions]));
		}
	}
	
	public static void calculateIntensitiesLinear(ImagePlus imp, ArrayList<Spot> filteredSpots, ArrayList<Float> intensity){
		//  iterate over all points and perform the linear interpolation for each of the spots
		ImagePlus xyzImp = getXyz(imp); // grabbed the non-normalized xyz-stack  
		NLinearInterpolatorFactory<FloatType> factory = new NLinearInterpolatorFactory<>();
		RealRandomAccessible<FloatType> interpolant = Views.interpolate(Views.extendZero(ImageJFunctions.wrapFloat(xyzImp)), factory);
		for (Spot fSpot : filteredSpots){
			RealRandomAccess<FloatType> rra = interpolant.realRandomAccess();
			double[] position = fSpot.getCenter();
			rra.setPosition(position);
			intensity.add(new Float(rra.get().get()));	
		}
	}
	
	// returns only xyz stack from the ImagePlus object
	public static ImagePlus getXyz(ImagePlus imp){
		final ImageStack stack = new ImageStack(imp.getWidth(), imp.getHeight() );
		int [] impDimensions = imp.getDimensions();

		for (int z = 0; z < impDimensions[3]; z++){
			int id = imp.getStackIndex(1, z + 1, 1);
			stack.addSlice(imp.getStack().getProcessor( id ));
		}

		ImagePlus xyzImp = new ImagePlus("merge", stack );
		return xyzImp;
	}
}
