package paper.test.radialsymmetry;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import net.imagej.ImageJ;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;

import compute.RadialSymmetry;
import fit.Spot;
import gui.interactive.HelperFunctions;
import gui.vizualization.Visualization;
import imglib2.RealTypeNormalization;
import imglib2.TypeTransformingRandomAccessibleInterval;
import parameters.GUIParams;
import parameters.RadialSymmetryParameters;
import result.output.ShowResult;
import util.ImgLib2Util;

public class RunTests {
	// here we actually run the tests 

	public static void test(Img<FloatType> img, ArrayList <double[] > positions, int numDimensions, long [] dimensions) {
		// 1. find the spots using RS 
		int numRealSpots = positions.size();

		// TODO: move the inits to the separate file
		// this parameters should come from the manual adjustment
		// DoG parameters
		// current
		float sigmaDog = 1.5f; 
		float threshold = 0.015f;
		
		// RANSAC parameters
		// current value
		boolean RANSAC = true;
		int supportRadius = 3; // this one I know
		float maxError = 0.3f; 
		float inlierRatio = 0.60f;

		// Background Subtraction parameters
		// current values
		float bsMaxError = 0.05f;
		float bsInlierRatio = 0.75f;
		String bsMethod = "No background subtraction";

		// Gauss Fit over intensities
		boolean gaussFit = false;
		double[] minmax = HelperFunctions.computeMinMax(img);

		float min = (float) minmax[0];
		float max = (float) minmax[1];
		// set the parameters from the defaults
		final GUIParams params = new GUIParams();
		
		params.setRANSAC(RANSAC);
		params.setMaxError(maxError);
		params.setInlierRatio(inlierRatio);
		params.setSupportRadius(supportRadius);
		params.setBsMaxError(bsMaxError);
		params.setBsInlierRatio(bsInlierRatio);
		params.setBsMethod(bsMethod);
		params.setSigmaDog(sigmaDog);
		params.setThresholdDog(threshold);
		
		// TODO: 
		// back up the parameter values to the default variables
		params.setDefaultValues();

		double [] calibration  = new double[numDimensions];
		for (int d = 0; d < numDimensions; d++)
			calibration[d] = 1;
		RadialSymmetryParameters rsm = new RadialSymmetryParameters(params, calibration);

		// normalize the whole image if it is possible
		RandomAccessibleInterval<FloatType> rai;
		if (!Double.isNaN(min) && !Double.isNaN(max)) // if normalizable
			rai = new TypeTransformingRandomAccessibleInterval<>(img, new RealTypeNormalization<>(min, max - min), new FloatType());
		else // otherwise use
			rai = img;

		ArrayList<Spot> spots = new ArrayList<>(0);
		// stores number of detected spots per time point
		ArrayList<Long> timePoint = new ArrayList<>(0);
		// stores number of detected spots per channel
		ArrayList<Long> channelPoint = new ArrayList<>(0);
		// stores the intensity values
		ArrayList<Float> intensity = new ArrayList<>(0);

		int [] fullDim = new int[] {(int)dimensions[0], (int)dimensions[1], 1, 1, 1};

		RadialSymmetry.processSliceBySlice(img, rai, rsm, fullDim, gaussFit, params.getSigmaDoG(),
			spots, timePoint, channelPoint, intensity);

		if (false) {
			// some feedback
			Visualization.showVisualization(ImageJFunctions.wrap(img, ""), spots, intensity, timePoint, true, true,
				params.getSigmaDoG(), params.getAnisotropyCoefficient());
			double histThreshold = Visualization.getHistThreshold(); // used to show the overlays
			ShowResult.ransacResultTable(spots, timePoint, channelPoint, intensity, histThreshold);
		}
		// 2. sort both lists for faster search 
		UtilComparators comparators = new UtilComparators();
		positions.sort(comparators.new doubleComparator());
		spots.sort(comparators.new spotComparator());

		if (false) {
			// some feedback
			for (double [] dd : positions)
				System.out.println(dd[0] + " " + dd[1]);
			for (Spot dd : spots)
				System.out.println(dd.getDoublePosition(0) + " " + dd.getDoublePosition(1));
		}

		// 3. compare how far away are spots from the initial dots
		// 
		int [] pos2spot = new int [numRealSpots];
		for (int j = 0; j < numRealSpots; j++)
			pos2spot[j] = -1;
		
		// defines how far are the spots from the correct spots
		double eps = maxError; 
		long numRealDetections = 0;
		for (int j = 0; j < spots.size(); j++) {
			Spot spot = spots.get(j);
			int idx = Collections.binarySearch(positions, spot.getCenter(), comparators.new doubleComparator());

			idx += 1;
			idx *= -1;
			
			final double[] location = new double [numDimensions];
			spot.localize(location);
			
			if (UtilComparators.dist(location, positions.get(idx - 1)) < eps && pos2spot[idx - 1] == -1) {
				pos2spot[idx - 1] = j;
				// System.out.println(spot.getDoublePosition(0) + " " + spot.getDoublePosition(1));
				numRealDetections++;
			} else if (UtilComparators.dist(location, positions.get(idx)) < eps && pos2spot[idx] == -1) {
				pos2spot[idx] = j;
				// System.out.println(spot.getDoublePosition(0) + " " + spot.getDoublePosition(1));
				numRealDetections++;
			}
		}
		System.out.println("Total detections:" + numRealDetections);
		
	}

	public static void runTest() {

		// initialize the parameters
		String path = InputParams.path;
		String imgName = InputParams.imgName;
		String posName = InputParams.posName;
		long [] dims = InputParams.dims;
		double [] sigma = InputParams.sigma; 
		long numSpots = InputParams.numSpots;
		int seed = InputParams.seed;
		boolean padding = InputParams.padding;

		int numDimensions = InputParams.numDimensions;

		// paths to the data
		String fullImgPath = path + imgName + "-" + numSpots + "-" + sigma[0] + "-" + sigma[1] + "-" + seed + ".tif";
		String fullPosPath = path + posName + "-" + numSpots + "-" + sigma[0] + "-" + sigma[1] + "-" + seed + ".csv";

		Img<FloatType> img = ImgLib2Util.openAs32Bit(new File(fullImgPath));
		ArrayList<double[]> positions = new ArrayList<>();

		IOFunctions.readCSV(positions, fullPosPath, numDimensions);


		ImageJFunctions.show(img);
		test(img, positions, numDimensions, dims);
	}

	public static void main(String [] args) {
		new ImageJ();
		runTest();
	}
}
