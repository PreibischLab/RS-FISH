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
	public static int bsNumIterations = 100; // not a parameter, can be changed
												// through Beanshell
	public static int numIterations = 100; // not a parameter, can be changed
											// through Beanshell
	ArrayList<RefinedPeak<Point>> peaks;
	Gradient derivative;

	ArrayList<Spot> spots;

	private static final boolean debug = true;
	private static final long timingScale = 1000; // will result in seconds

	float sigma;
	float threshold;
	int supportRadius;
	float inlierRatio;
	float maxError;

	int bsMethod;
	float bsMaxError;
	float bsInlierRatio;

	public RadialSymmetry(final RadialSymmetryParameters params, final RandomAccessibleInterval<FloatType> img) {
		// TODO: make them fields (?)
		// instead you can use only one RadialSymmetryParameters variable but
		// the calls will be too long
		sigma = params.getParams().getSigmaDoG();
		threshold = params.getParams().getThresholdDoG();
		supportRadius = params.getParams().getSupportRadius();
		inlierRatio = params.getParams().getInlierRatio();
		maxError = params.getParams().getMaxError();

		bsMethod = params.getParams().getBsMethod();
		bsMaxError = params.getParams().getBsMaxError();
		bsInlierRatio = params.getParams().getBsInlierRatio();

		float sigma2 = HelperFunctions.computeSigma2(sigma, Radial_Symmetry.defaultSensitivity);
		
		// ImageJFunctions.show(img).setTitle("is this one a 3D");
		

		if (img.numDimensions() == 2 || img.numDimensions() == 3) {
			// IMP: in the 3D case the blobs will have lower contrast as a
			// function of sigma(z) therefore we have to adjust the threshold;
			// to fix the problem we use an extra factor =0.5 which will
			// decrease the threshold value; this might help in some cases but
			// z-extra smoothing is image depended

			long sTime = System.currentTimeMillis();
			final float tFactor = img.numDimensions() == 3 ? 0.5f : 1.0f;
			final DogDetection<FloatType> dog2 = new DogDetection<>(img, params.getCalibration(), sigma, sigma2,
					DogDetection.ExtremaType.MINIMA, tFactor * threshold / 2, false);
			peaks = dog2.getSubpixelPeaks();

			if (debug) {
				System.out.println("Timing: DoG peaks : " + (System.currentTimeMillis() - sTime) / timingScale);
				sTime = System.currentTimeMillis();
			}

			derivative = new GradientPreCompute(img);

			if (debug) {
				System.out.println("Timing: Derivative : " + (System.currentTimeMillis() - sTime) / timingScale);
				sTime = System.currentTimeMillis();
			}
			// if (true) return;

			final ArrayList<long[]> simplifiedPeaks = new ArrayList<>(1);
			int numDimensions = img.numDimensions();
			// copy all peaks
			for (final RefinedPeak<Point> peak : peaks) {
				if (-peak.getValue() > threshold) {
					final long[] coordinates = new long[numDimensions];
					for (int d = 0; d < peak.numDimensions(); ++d)
						coordinates[d] = Util.round(peak.getDoublePosition(d));
					simplifiedPeaks.add(coordinates);
				}
			}

			if (debug) {
				System.out.println("Timing: Copying peaks : " + (System.currentTimeMillis() - sTime) / timingScale);
				sTime = System.currentTimeMillis();
			}
			// the size of the RANSAC area
			final long[] range = new long[numDimensions];

			for (int d = 0; d < numDimensions; ++d) {
				range[d] = 2 * supportRadius;
			}

			final NormalizedGradient ng;

			// "No background subtraction", "Mean", "Median", "RANSAC on Mean",
			// "RANSAC on Median"
			if (bsMethod == 0)
				ng = null;
			else if (bsMethod == 1)
				ng = new NormalizedGradientAverage(derivative);
			else if (bsMethod == 2)
				ng = new NormalizedGradientMedian(derivative);
			else if (bsMethod == 3)
				ng = new NormalizedGradientRANSAC(derivative, CenterMethod.MEAN, bsMaxError, bsInlierRatio);
			else if (bsMethod == 4)
				ng = new NormalizedGradientRANSAC(derivative, CenterMethod.MEDIAN, bsMaxError, bsInlierRatio);
			else
				throw new RuntimeException("Unknown bsMethod: " + bsMethod);

			spots = Spot.extractSpots(img, simplifiedPeaks, derivative, ng, range);
			if (debug) {
				System.out.println("Timing: Extract peaks : " + (System.currentTimeMillis() - sTime) / timingScale);
				sTime = System.currentTimeMillis();
			}

			Spot.ransac(spots, numIterations, maxError, inlierRatio);
			
			for (final Spot spot : spots)
				spot.computeAverageCostInliers();

			if (debug) {
				System.out.println("Timing : RANSAC peaks : " + (System.currentTimeMillis() - sTime) / timingScale);
				sTime = System.currentTimeMillis();
			}
			ransacResultTable(spots);
			if (debug) {
				System.out.println("Timing : Results peaks : " + (System.currentTimeMillis() - sTime) / timingScale);
				sTime = System.currentTimeMillis();
			}

		} else
			// TODO: if the code is organized correctly this part should be
			// redundant
			System.out.println("Wrong dimensionality. Currently supported 2D/3D!");
	}

	public ArrayList<RefinedPeak<Point>> getPeaks() {
		return peaks;
	}

	public ArrayList<Spot> getSpots() {
		return spots;
	}

	// this function will show the result of RANSAC
	// proper window -> dialog view with the columns
	public void ransacResultTable(final ArrayList<Spot> spots) {
		IOFunctions.println("Running RANSAC ... ");
		// real output
		ResultsTable rt = new ResultsTable();
		String[] xyz = { "x", "y", "z" };
		for (Spot spot : spots) {	
			// if spot was not discarded
			if (spot.numRemoved != spot.candidates.size()){
				rt.incrementCounter();
				for (int d = 0; d < spot.numDimensions(); ++d) {
					// FIXME: might be the wrong output
					rt.addValue(xyz[d], String.format(java.util.Locale.US, "%.2f", spot.getFloatPosition(d)));
				}
			}
		}
		IOFunctions.println("Spots found = " + rt.getCounter()); 
		rt.show("Results");
	}

}
