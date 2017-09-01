package compute;

import gui.interactive.HelperFunctions;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
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

//			for (RefinedPeak<Point> point : peaks){
//				for (int d = 0; d < 3; d++)
//					System.out.print(point.getDoublePosition(d) + " ");
//				System.out.println();
//			}
//			System.out.println(peaks.size());
			
			
			if (debug) {
				System.out.println("Timing: DoG peaks : " + (System.currentTimeMillis() - sTime) / timingScale);
				sTime = System.currentTimeMillis();
			}

			derivative = new GradientPreCompute(img);

			if (debug) {
				System.out.println("Timing: Derivative : " + (System.currentTimeMillis() - sTime) / timingScale);
				sTime = System.currentTimeMillis();
			}

			final ArrayList<long[]> simplifiedPeaks = new ArrayList<>(1);
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

			// IJ.log( "peaks: " + simplifiedPeaks.size() );

			if (debug) {
				System.out.println("Timing: Copying peaks : " + (System.currentTimeMillis() - sTime) / timingScale);
				sTime = System.currentTimeMillis();
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

			// the size of the RANSAC area
			final long[] range = new long[numDimensions];

			for (int d = 0; d < numDimensions; ++d)
				range[d] = supportRadius;

			// this is a hack
			//range[ 2 ] *= 6;
			System.out.println( "Range: " + Util.printCoordinates(range));
			
//			double bestDist = Double.MAX_VALUE;
//			double bestScale = -1;
			
//			for (float idx = 1.0f; idx < 1.21f; idx += 0.2f){
				spots = Spot.extractSpots(img, simplifiedPeaks, derivative, ng, range);
						
				// TODO: HERE OR BEFORE? 
				// ADD SCALING HERE
//				float scaled = idx;
//				for (int j = 0; j < spots.size(); j++){
//					spots.get(j).updateScale(new float []{1, 1, scaled});
//				}
	
	
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
				
//				double[] knowLocation = new double[]{ 124.52561137015748, 129.88211102199878, 121.78135663923388 };
//				double dist = 0;
//		
//				double avgInliers = 0;
//				long total = 0;
//				for (final Spot spot : spots)
//				{
//					dist = mpicbg.models.Point.distance( new mpicbg.models.Point( knowLocation), new mpicbg.models.Point( spot.getCenter() ) );
//
//					if ( spot.inliers.size() == 0)
//						continue;
//
//					total++;
//					avgInliers += spot.inliers.size();	
//
//					// IJ.log( "removed " + spot.numRemoved );
//				}
//	
//				if ( dist < bestDist )
//				{
//					bestDist = dist;
//					bestScale = idx;
//				}
//
//				IJ.log(scaled + " " + avgInliers/total + " " + avgInliers + " " + total + " d=" + dist );
				
//			}

//			IJ.log("best:");
//			IJ.log( bestScale + " " + bestDist );

			if (debug) {
				System.out.println("Timing : RANSAC peaks : " + (System.currentTimeMillis() - sTime) / timingScale);
				sTime = System.currentTimeMillis();
			}
			
			// TODO: Moved to the main function
			//			ransacResultTable(spots);
			//			if (debug) {
			//				System.out.println("Timing : Results peaks : " + (System.currentTimeMillis() - sTime) / timingScale);
			//				sTime = System.currentTimeMillis();
			//			}

		} else
			// TODO: if the code is organized correctly this part should be removed 
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
	public static void ransacResultTable(final ArrayList<Spot> spots, final ArrayList<Long> timePoint, final ArrayList<Long> channelPoint, ArrayList<Float> intensity) {
		IOFunctions.println("Running RANSAC ... ");
		// real output
		ResultsTable rt = new ResultsTable();
		String[] xyz = { "x", "y", "z" };
		int currentTimePoint = 0; 
		int totalSpotsPerTimePoint = 0;
		
		int currentChannelPoint = 0; 
		int totalSpotsPerChannelPoint = 0;
		
		for (Spot spot : spots) {	
			// if spot was not discarded
			if (spot.inliers.size() != 0){
				rt.incrementCounter();
				double[] pos = spot.getCenter();	
				for (int d = 0; d < spot.numDimensions(); ++d) {
					rt.addValue(xyz[d], String.format(java.util.Locale.US, "%.4f", pos[d]));
				}
						
				totalSpotsPerTimePoint++;
				if (totalSpotsPerTimePoint > timePoint.get(currentTimePoint)){
					currentTimePoint++;
					totalSpotsPerTimePoint = 0;
				}
				rt.addValue("t", currentTimePoint + 1); // user-friendly, starting the counting from 1
				
				totalSpotsPerChannelPoint++;
				if (totalSpotsPerChannelPoint > channelPoint.get(currentChannelPoint)){
					currentChannelPoint++;
					totalSpotsPerChannelPoint = 0;
				}
				rt.addValue("c", currentChannelPoint + 1); // user-friendly, starting the counting from 1				
				
				// TODO: REMOVE THIS IF - show the result always
				// if (gaussFit)
				rt.addValue("intensity",  String.format(java.util.Locale.US, "%.4f", intensity.get(rt.getCounter() - 1)) );
				
			}
		}
		IOFunctions.println("Spots found = " + rt.getCounter()); 
		rt.show("Results");
	}

}
