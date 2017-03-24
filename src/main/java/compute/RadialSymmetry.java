package compute;

import gui.interactive.HelperFunctions;
import ij.measure.ResultsTable;
import mpicbg.spim.io.IOFunctions;
import net.imglib2.Point;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.dog.DogDetection;
import net.imglib2.algorithm.localextrema.RefinedPeak;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import parameters.RadialSymmetryParameters;

import java.util.ArrayList;

import background.NormalizedGradient;
import background.NormalizedGradientAverage;
import background.NormalizedGradientMedian;
import background.NormalizedGradientRANSAC;
import fit.Spot;
import fit.Center.CenterMethod;
import gradient.Gradient;
import gradient.GradientPreCompute;
import gui.Radial_Symmetry;

public class RadialSymmetry // < T extends RealType< T > & NativeType<T> >
{
	public static int bsNumIterations = 100; // not a parameter, can be changed through Beanshell
	public static int numIterations = 100; // not a parameter, can be changed through Beanshell

	ArrayList<RefinedPeak<Point>> peaks;
	Gradient derivative; 

	public RadialSymmetry( final RadialSymmetryParameters params, final RandomAccessibleInterval< FloatType > img )
	{
		// TODO: make them fields (?) 
		float sigma = params.getParams().getSigmaDoG();
		float threshold = params.getParams().getThresholdDoG();
		int supportRadius = params.getParams().getSupportRadius();
		float inlierRatio = params.getParams().getInlierRatio();
		float maxError = params.getParams().getInlierRatio();

		int bsMethod = params.getParams().getBsMethod();
		float bsMaxError = params.getParams().getBsMaxError();
		float bsInlierRatio = params.getParams().getBsInlierRatio();
		
		float sigma2 = HelperFunctions.computeSigma2(sigma, Radial_Symmetry.defaultSensitivity);

		if (img.numDimensions() == 2 || img.numDimensions() == 3 )
		{	
			// IMP: in the 3D case the blobs will have lower contrast as a function of sigma(z) therefore we have to adjust the threshold;
			// to fix the problem we use an extra factor =0.5 which will decrease the threshold value; this might help in some cases but z-extrasmoothing
			// is image depended
			
			System.out.println( System.currentTimeMillis() );
			final float tFactor = img.numDimensions() == 3 ? 0.5f : 1.0f;	
			final DogDetection<FloatType> dog2 = new DogDetection<>(img, params.getCalibration(), sigma, sigma2 , DogDetection.ExtremaType.MINIMA,  tFactor*threshold / 2, false);
			peaks = dog2.getSubpixelPeaks();
			
			derivative = new GradientPreCompute( img );
			
			// if (true) return;
			
			final ArrayList<long[]> simplifiedPeaks = new ArrayList<>(1);
			int numDimensions = img.numDimensions(); 
			// copy all peaks
			for (final RefinedPeak<Point> peak : peaks){
				if (-peak.getValue() > threshold)
				{
					final long[] coordinates = new long[numDimensions];
					for (int d = 0; d < peak.numDimensions(); ++d)
						coordinates[d] = Util.round(peak.getDoublePosition(d));
					simplifiedPeaks.add(coordinates);
				}
			}

			// the size of the RANSAC area
			final long[] range = new long[numDimensions];

			for (int d = 0; d < numDimensions; ++d){
				range[d] =  2*supportRadius;
			}

			// ImageJFunctions.show(new GradientPreCompute(img).preCompute(img));
			final NormalizedGradient ng;

			// "No background subtraction", "Mean", "Median", "RANSAC on Mean", "RANSAC on Median"
			if ( bsMethod == 0 )
				ng = null;
			else if ( bsMethod == 1 )
				ng = new NormalizedGradientAverage( derivative );
			else if ( bsMethod == 2 )
				ng = new NormalizedGradientMedian( derivative );
			else if ( bsMethod == 3 )
				ng = new NormalizedGradientRANSAC( derivative, CenterMethod.MEAN, bsMaxError, bsInlierRatio );
			else if ( bsMethod == 4 )
				ng = new NormalizedGradientRANSAC( derivative, CenterMethod.MEDIAN, bsMaxError, bsInlierRatio );
			else
				throw new RuntimeException( "Unknown bsMethod: " + bsMethod );

			final ArrayList<Spot> spots = Spot.extractSpots(img, simplifiedPeaks, derivative, ng, range);
			Spot.ransac(spots, numIterations, maxError, inlierRatio);
			for (final Spot spot : spots)
				spot.computeAverageCostInliers();
			ransacResultTable(spots);
		}
		else
			// TODO: if the code is organized correctly this part should be redundant 
			System.out.println("Wrong dimensionality. Currently supported 2D/3D!");
	}
	
	// this function will show the result of RANSAC
	// proper window -> dialog view with the columns
	public void ransacResultTable(final ArrayList<Spot> spots) {
		IOFunctions.println("Running RANSAC ... ");
		IOFunctions.println("Spots found = " + spots.size());
		// real output
		ResultsTable rt = new ResultsTable();
		String[] xyz = { "x", "y", "z" };
		for (Spot spot : spots) {
			rt.incrementCounter();
			for (int d = 0; d < spot.numDimensions(); ++d) {
				// FIXME: might be the wrong output
				rt.addValue(xyz[d], String.format(java.util.Locale.US, "%.2f", spot.getFloatPosition(d)));
			}
		}
		rt.show("Results");
	}
	
}
