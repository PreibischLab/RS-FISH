package compute;

import java.util.ArrayList;

import net.imglib2.Point;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.dog.DogDetection;
import net.imglib2.algorithm.localextrema.RefinedPeak;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;

import background.NormalizedGradient;
import background.NormalizedGradientAverage;
import background.NormalizedGradientMedian;
import background.NormalizedGradientRANSAC;
import fit.Center.CenterMethod;
import fit.Spot;
import gradient.Gradient;
import gradient.GradientPreCompute;
import gui.Radial_Symmetry;
import gui.interactive.HelperFunctions;
import ij.IJ;
import ij.ImagePlus;
import ij.measure.ResultsTable;
import intensity.Intensity;
import mpicbg.spim.io.IOFunctions;
import parameters.RadialSymmetryParameters;

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

	String bsMethod;
	float bsMaxError;
	float bsInlierRatio;
	
	float anisotropy;

	public RadialSymmetry(final RadialSymmetryParameters params, final RandomAccessibleInterval<FloatType> img) {

		sigma = params.getParams().getSigmaDoG();
		threshold = params.getParams().getThresholdDoG();
		supportRadius = params.getParams().getSupportRadius();
		inlierRatio = params.getParams().getInlierRatio();
		maxError = params.getParams().getMaxError();

		bsMethod = params.getParams().getBsMethod();
		bsMaxError = params.getParams().getBsMaxError();
		bsInlierRatio = params.getParams().getBsInlierRatio();
		
		anisotropy = params.getParams().getAnisotropyCoefficient();

		float sigma2 = HelperFunctions.computeSigma2(sigma, Radial_Symmetry.defaultSensitivity);
		// TODO: is this check necessary ? 
		if (img.numDimensions() == 2 || img.numDimensions() == 3) {
			// IMP: in the 3D case the blobs will have lower contrast as a
			// function of sigma(z) therefore we have to adjust the threshold;
			// to fix the problem we use an extra factor = 0.5 which will
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

			final ArrayList<long[]> simplifiedPeaks = new ArrayList<>(0);
			int numDimensions = img.numDimensions();
			// copy all peaks
			// TODO: USE FUNCTION FROM HELPERFUNCTIONS
			for (final RefinedPeak<Point> peak : peaks) {
				if (-peak.getValue() > threshold) {
					final long[] coordinates = new long[numDimensions];
					for (int d = 0; d < peak.numDimensions(); ++d)
						coordinates[d] = Util.round(peak.getDoublePosition(d));
					simplifiedPeaks.add(coordinates);
				}
			}

			// HelperFunctions.copyPeaks(peaks, simplifiedPeaks, numDimensions);		
			// IJ.log( "peaks: " + simplifiedPeaks.size() );

			if (debug) {
				System.out.println("Timing: Copying peaks : " + (System.currentTimeMillis() - sTime) / timingScale);
				sTime = System.currentTimeMillis();
			}

			final NormalizedGradient ng;

			// "No background subtraction", "Mean", "Median", "RANSAC on Mean",
			// "RANSAC on Median"
			
			if (bsMethod.equals("No background subtraction"))
				ng = null;
			else if ( bsMethod.equals("Mean"))
				ng = new NormalizedGradientAverage( derivative );
			else if ( bsMethod.equals("Median") )
				ng = new NormalizedGradientMedian( derivative );
			else if ( bsMethod.equals("RANSAC on Mean") )
				ng = new NormalizedGradientRANSAC( derivative, CenterMethod.MEAN, bsMaxError, bsInlierRatio);
			else if ( bsMethod.equals("RANSAC on Median") )
				ng = new NormalizedGradientRANSAC( derivative, CenterMethod.MEDIAN, bsMaxError, bsInlierRatio);
			else
				throw new RuntimeException("Unknown bsMethod: " + bsMethod);
			
			// the size of the RANSAC area
			final long[] range = new long[numDimensions];

			for (int d = 0; d < numDimensions; ++d)
				range[d] = supportRadius;

			if (debug) System.out.println( "Range: " + Util.printCoordinates(range));
			spots = Spot.extractSpots(img, simplifiedPeaks, derivative, ng, range);
			// scale the z-component according to the anisotropy coefficient	
			// if the image is 3D
			if (numDimensions == 3)
				for (int j = 0; j < spots.size(); j++)
					spots.get(j).updateScale(new float []{1, 1, anisotropy});

			IJ.log( "num spots: " + spots.size() );
			if (debug) {
				System.out.println("Timing: Extract peaks : " + (System.currentTimeMillis() - sTime) / timingScale);
				sTime = System.currentTimeMillis();
			}

			// TODO: IS THIS A PLACE WHERE YOU CAN SKIP RANSAC
			if (params.getParams().getRANSAC()){
				Spot.ransac(spots, numIterations, maxError, inlierRatio);
			}
			else{
				try{
					Spot.fitCandidates(spots);
					//IJ.log( "inliers: " + spots.get(0).inliers.size() );
				}
				catch(Exception e){
					System.out.println("EXCEPTION CAUGHT");
				}
			}

			if (debug) {
				System.out.println("Timing : RANSAC peaks : " + (System.currentTimeMillis() - sTime) / timingScale);
				sTime = System.currentTimeMillis();
			}

		} else
			// TODO: if the code is organized correctly this part should be removed 
			System.out.println("Wrong dimensionality. Currently supported 2D/3D!");
	}
	
	// process each 2D/3D slice of the image to search for the spots
	public static void processSliceBySlice(ImagePlus imp, RandomAccessibleInterval<FloatType> rai, RadialSymmetryParameters rsm,
			int[] impDim, boolean gaussFit, double sigma, ArrayList<Spot> allSpots,
			ArrayList<Long> timePoint, ArrayList<Long> channelPoint, ArrayList<Float> intensity) {
		RandomAccessibleInterval<FloatType> timeFrame;

		int numDimensions = rai.numDimensions();

		// impDim <- x y c z t
		for (int c = 0; c < impDim[2]; c++) {
			for (int t = 0; t < impDim[4]; t++) {
				// "-1" because of the imp offset
				timeFrame = HelperFunctions.copyImg(rai, c, t, impDim);
				
				RadialSymmetry rs = new RadialSymmetry(rsm, timeFrame);

				// TODO: if the detect spot has at least 1 inlier add it
				// FIXME: is this part necessary? 
				ArrayList<Spot> filteredSpots = HelperFunctions.filterSpots(rs.getSpots(), 1 );

				allSpots.addAll(filteredSpots);
				// set the number of points found for the current time step
				timePoint.add(new Long(filteredSpots.size()));

				// user wants to have the gauss fit here
				if (gaussFit) { // TODO: fix the problem with the computations of this one
					Intensity.calulateIntesitiesGF(imp, numDimensions, rsm.getParams().getAnisotropyCoefficient(),
							sigma, filteredSpots, intensity);
				}
				else //  iterate over all points and perform the linear interpolation for each of the spots
					Intensity.calculateIntensitiesLinear(imp, filteredSpots, intensity);
			}
			if (c != 0)
				channelPoint.add(new Long(allSpots.size() - channelPoint.get(c - 1)));
			else
				channelPoint.add(new Long(allSpots.size()));
		}

	}
	

	public ArrayList<RefinedPeak<Point>> getPeaks() {
		return peaks;
	}

	public ArrayList<Spot> getSpots() {
		return spots;
	}

}
