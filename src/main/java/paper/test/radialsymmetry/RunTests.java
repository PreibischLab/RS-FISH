package paper.test.radialsymmetry;

import java.io.File;
import java.util.ArrayList;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.multithreading.SimpleMultiThreading;
import net.imglib2.type.numeric.real.FloatType;

import compute.RadialSymmetry;
import fit.Spot;
import gui.imagej.GenericDialogGUIParams;
import gui.interactive.HelperFunctions;
import gui.interactive.InteractiveRadialSymmetry;
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

		// TODO: move the inits to the separate file
		// this parameters should come from the manual adjustment
		// RANSAC parameters
		// current value
		boolean RANSAC = true;
		float maxError = 0.30f; 
		float inlierRatio = 0.60f;
		int supportRadius = 3; // this one I know

		// Background Subtraction parameters
		// current values
		float bsMaxError = 0.05f;
		float bsInlierRatio = 0.75f;
		String bsMethod = "No background subtraction";

		// DoG parameters
		// current
		float sigma = 3.0f; 
		float threshold = 0.05f;

		// Gauss Fit over intensities
		boolean gaussFit = false;

		double[] minmax = HelperFunctions.computeMinMax(img);

		float min = (float) minmax[0];
		float max = (float) minmax[1];

		// TODO: do I need this one
		// set the parameters from the defaults
		final GUIParams params = new GUIParams();
		params.setRANSAC(RANSAC);

		// InteractiveRadialSymmetry irs = new InteractiveRadialSymmetry(imp, params, min, max);

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

		ArrayList<Spot> allSpots = new ArrayList<>(0);
		// stores number of detected spots per time point
		ArrayList<Long> timePoint = new ArrayList<>(0);
		// stores number of detected spots per channel
		ArrayList<Long> channelPoint = new ArrayList<>(0);
		// stores the intensity values
		ArrayList<Float> intensity = new ArrayList<>(0);
		
		int [] fullDim = new int[] {(int)dimensions[0], (int)dimensions[1], 1, 1, 1};

		RadialSymmetry.processSliceBySlice(img, rai, rsm, fullDim, gaussFit, params.getSigmaDoG(),
			allSpots, timePoint, channelPoint, intensity);

		Visualization.showVisualization(ImageJFunctions.wrap(img, ""), allSpots, intensity, timePoint, true, true,
			params.getSigmaDoG(), params.getAnisotropyCoefficient());
		double histThreshold = Visualization.getHistThreshold(); // used to show the overlays
		ShowResult.ransacResultTable(allSpots, timePoint, channelPoint, intensity, histThreshold);

		// 2. sort both lists for faster search 
		// 3. compare how far away are spots from the initial dots
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
		runTest();
	}
}
