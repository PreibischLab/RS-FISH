package compute;

import java.awt.Rectangle;
import java.util.ArrayList;

import net.imglib2.Point;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.dog.DogDetection;
import net.imglib2.algorithm.localextrema.RefinedPeak;
import net.imglib2.type.numeric.real.FloatType;

import background.NormalizedGradient;
import background.NormalizedGradientAverage;
import background.NormalizedGradientMedian;
import background.NormalizedGradientRANSAC;
import fit.Center.CenterMethod;
import fit.Spot;
import gradient.Gradient;
import gradient.GradientPreCompute;
import gui.interactive.HelperFunctions;
import ij.IJ;
import ij.ImagePlus;
import intensity.Intensity;
import parameters.RadialSymmetryParameters;

public class RadialSymmetry {
	public static int bsNumIterations = 100; // not a parameter, can be changed
												// through Beanshell
	public static int numIterations = 100; // not a parameter, can be changed
											// through Beanshell
	// steps per octave for DoG
	private static int defaultSensitivity = 4;

	ArrayList<RefinedPeak<Point>> peaks;
	ArrayList<Spot> spots;
	Gradient derivative;
	NormalizedGradient ng;

	// private static final boolean debug = true;
	// private static final long timingScale = 1000; // will result in seconds
	// DOG:
	float sigma;
	float threshold;
	// RANSAC:
	boolean ransac;
	int supportRadius;
	float inlierRatio;
	float maxError;
	// Background subtraction:
	String bsMethod;
	float bsMaxError;
	float bsInlierRatio;
	// General image params:
	RandomAccessibleInterval<FloatType> img;
	float anisotropy;
	double[] calibration;

	// set all parameters in the constructor
	public RadialSymmetry(final RandomAccessibleInterval<FloatType> img, final RadialSymmetryParameters params) {
		this.img = img;
		sigma = params.getParams().getSigmaDoG();
		threshold = params.getParams().getThresholdDoG();
		supportRadius = params.getParams().getSupportRadius();
		inlierRatio = params.getParams().getInlierRatio();
		maxError = params.getParams().getMaxError();
		bsMethod = params.getParams().getBsMethod();
		bsMaxError = params.getParams().getBsMaxError();
		bsInlierRatio = params.getParams().getBsInlierRatio();
		anisotropy = params.getParams().getAnisotropyCoefficient();
		ransac = params.getParams().getRANSAC();
		calibration = params.getCalibration();
	}

	public void compute() {
		compute(img, sigma, threshold, supportRadius, inlierRatio, maxError, bsMethod, bsMaxError, bsInlierRatio,
				anisotropy, ransac);
	}

	public void compute(final RandomAccessibleInterval<FloatType> pImg, float pSigma, float pThreshold,
			int pSupportRadius, float pInlierRatio, float pMaxError, String pBsMethod, float pBsMaxError,
			float pBsInlierRatio, float pAnisotropy, boolean pRansac) {
		// perform DOG
		peaks = computeDog(pImg, pSigma, pThreshold);
		// calculate (normalized) derivatives
		derivative = new GradientPreCompute(pImg);
		ng = calculateNormalizedGradient(derivative, pBsMethod, pBsMaxError, pBsInlierRatio);
		// use light weighted structure for the radial symmetry computations
		final ArrayList<long[]> simplifiedPeaks = new ArrayList<>(0);
		int numDimensions = pImg.numDimensions();
		Rectangle rectangle = new Rectangle(0, 0, (int) pImg.dimension(0), (int) pImg.dimension(1));
		HelperFunctions.copyPeaks(peaks, simplifiedPeaks, numDimensions, rectangle, pThreshold);
		// trigger radial symmetry
		spots = computeRadialSymmetry(pImg, ng, derivative, simplifiedPeaks, pSupportRadius, pInlierRatio, pMaxError,
				pAnisotropy, pRansac);
	}

	public ArrayList<RefinedPeak<Point>> computeDog(final RandomAccessibleInterval<FloatType> pImg, float pSigma,
			float pThreshold) {
		float pSigma2 = HelperFunctions.computeSigma2(pSigma, defaultSensitivity);
		final float tFactor = pImg.numDimensions() == 3 ? 0.5f : 1.0f;
		final DogDetection<FloatType> dog2 = new DogDetection<>(pImg, calibration, pSigma, pSigma2,
				DogDetection.ExtremaType.MINIMA, tFactor * pThreshold / 2, false);
		ArrayList<RefinedPeak<Point>> pPeaks = dog2.getSubpixelPeaks();
		return pPeaks;
	}

	public ArrayList<Spot> computeRadialSymmetry(final RandomAccessibleInterval<FloatType> pImg, NormalizedGradient pNg,
			Gradient pDerivative, ArrayList<long[]> simplifiedPeaks, int pSupportRadius, float pInlierRatio,
			float pMaxError, float pAnisotropy, boolean useRansac) {
		int numDimensions = pImg.numDimensions();

		// the size of the RANSAC area
		final long[] range = new long[numDimensions];
		for (int d = 0; d < numDimensions; ++d)
			range[d] = pSupportRadius;

		ArrayList<Spot> pSpots = Spot.extractSpots(pImg, simplifiedPeaks, pDerivative, pNg, range);
		// scale the z-component according to the anisotropy coefficient
		if (numDimensions == 3)
			fixAnisotropy(pSpots, pAnisotropy);

		IJ.log("DoG pre-detected spots: " + pSpots.size());

		if (useRansac)
			Spot.ransac(pSpots, numIterations, pMaxError, pInlierRatio);
		else
			try {
				Spot.fitCandidates(pSpots);
			} catch (Exception e) {
				System.out.println("Something went wrong, please report the bug.");
			}
		return pSpots;
	}

	public static void fixAnisotropy(ArrayList<Spot> spots, float pAnisotropy) {
		for (int j = 0; j < spots.size(); j++)
			spots.get(j).updateScale(new float[] { 1, 1, pAnisotropy });
	}

	public static NormalizedGradient calculateNormalizedGradient(Gradient pDerivative, String pBsMethod,
			float pBsMaxError, float pBsInlierRatio) {
		final NormalizedGradient ng;
		// "No background subtraction", "Mean", "Median", "RANSAC on Mean",
		// "RANSAC on Median"
		if (pBsMethod.equals("No background subtraction"))
			ng = null;
		else if (pBsMethod.equals("Mean"))
			ng = new NormalizedGradientAverage(pDerivative);
		else if (pBsMethod.equals("Median"))
			ng = new NormalizedGradientMedian(pDerivative);
		else if (pBsMethod.equals("RANSAC on Mean"))
			ng = new NormalizedGradientRANSAC(pDerivative, CenterMethod.MEAN, pBsMaxError, pBsInlierRatio);
		else if (pBsMethod.equals("RANSAC on Median"))
			ng = new NormalizedGradientRANSAC(pDerivative, CenterMethod.MEDIAN, pBsMaxError, pBsInlierRatio);
		else
			throw new RuntimeException("Unknown bsMethod: " + pBsMethod);
		return ng;
	}

	// process each 2D/3D slice of the image to search for the spots
	public static void processSliceBySlice(RandomAccessibleInterval<FloatType> img, RandomAccessibleInterval<FloatType> rai,
			RadialSymmetryParameters rsm, int[] impDim, boolean gaussFit, double sigma, ArrayList<Spot> allSpots,
			ArrayList<Long> timePoint, ArrayList<Long> channelPoint, ArrayList<Float> intensity) {
		RandomAccessibleInterval<FloatType> timeFrameNormalized;
		RandomAccessibleInterval<FloatType> timeFrame;

		// impDim <- x y c z t
		for (int c = 0; c < impDim[2]; c++) {
			for (int t = 0; t < impDim[4]; t++) {
				// grab xy(z) part of the image
				timeFrameNormalized = HelperFunctions.copyImg(rai, c, t, impDim);
				timeFrame 			= HelperFunctions.copyImg(img, c, t, impDim);
				
				RadialSymmetry rs = new RadialSymmetry(timeFrameNormalized, rsm);
				rs.compute();

				int minNumInliers = 1;
				ArrayList<Spot> filteredSpots = HelperFunctions.filterSpots(rs.getSpots(), minNumInliers);
				allSpots.addAll(filteredSpots);
				timePoint.add(new Long(filteredSpots.size()));

				if (gaussFit) {
					// FIXME: fix the problem with the computations of this one
					Intensity.calulateIntesitiesGF(timeFrame, timeFrame.numDimensions(), rsm.getParams().getAnisotropyCoefficient(),
							sigma, filteredSpots, intensity);
				} else // iterate over all points and perform the linear
						// interpolation for each of the spots
					Intensity.calculateIntensitiesLinear(timeFrame, filteredSpots, intensity);
				
				// FIXME: make this a parameter 
				if (true)
					Intensity.fixIntensities(filteredSpots, intensity);
				
			}
			if (c != 0) // FIXME: formula is wrong
				channelPoint.add(new Long(allSpots.size() - channelPoint.get(c - 1)));
			else
				channelPoint.add(new Long(allSpots.size()));
		}

	}

	// getters
	public ArrayList<RefinedPeak<Point>> getPeaks() {
		return peaks;
	}

	public ArrayList<Spot> getSpots() {
		return spots;
	}

	public Gradient getDerivative() {
		return derivative;
	}

	public NormalizedGradient getNormalizedGradient() {
		return ng;
	}
}
