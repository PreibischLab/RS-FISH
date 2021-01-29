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
import fitting.Spot;
import gui.interactive.HelperFunctions;
import gui.vizualization.Visualization;
import imglib2.RealTypeNormalization;
import imglib2.TypeTransformingRandomAccessibleInterval;
import parameters.GUIParams;
import parameters.RadialSymmetryParameters;
import result.output.ShowResult;
import util.ImgLib2Util;

public class RunTestsGenerated {
	// here we actually run the tests 

	public static void test(Img<FloatType> img, ArrayList <double[] > positions, RadialSymmetryParameters rsm, int numDimensions, long [] dimensions) {
		// 1. find the spots using RS 
		int numRealSpots = positions.size();

		double[] minmax = HelperFunctions.computeMinMax(img);

		float min = (float) minmax[0];
		float max = (float) minmax[1];

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
		if (numDimensions == 3)
			fullDim[3] = (int)dimensions[2];
		
		RadialSymmetry.processSliceBySlice(img, rai, rsm, fullDim,
			spots, timePoint, channelPoint, intensity);

		if (false) {
			// some feedback
			Visualization.showVisualization(ImageJFunctions.wrap(img, ""), spots, intensity, timePoint, true, true,
				rsm.getParams().getSigmaDoG(), rsm.getParams().getAnisotropyCoefficient());
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
		int [] pos2spot = new int [numRealSpots];
		for (int j = 0; j < numRealSpots; j++)
			pos2spot[j] = -1;
		
		// defines how far are the spots from the correct spots
		double eps = rsm.getParams().getMaxError(); 
		long numRealDetections = 0;
		for (int j = 0; j < spots.size(); j++) {
			Spot spot = spots.get(j);
			int idx = Collections.binarySearch(positions, spot.localize(), comparators.new doubleComparator());

			idx += 1;
			idx *= -1;
			
			final double[] location = new double [numDimensions];
			spot.localize(location);
			
			if (Utils.dist(location, positions.get(idx - 1)) < eps && pos2spot[idx - 1] == -1) {
				pos2spot[idx - 1] = j;
				numRealDetections++;
			} else if (Utils.dist(location, positions.get(idx)) < eps && pos2spot[idx] == -1) {
				pos2spot[idx] = j;
				numRealDetections++;
			}
		}
		System.out.println("total detections: " + numRealDetections);
		double rmse = Utils.rmse(positions, spots, pos2spot);
		System.out.println("rmse: " + rmse);
	}
	
	public static void runTest2D() {
		// initialize params for this specific test case
		InputParamsGenerated ipg = new InputParamsGenerated("", 0);
		// InputParamsGenerated.setParameters2D();
		// initialize the parameters
		String path = ipg.path;
		String imgName = ipg.imgName;
		String posName = ipg.posName;
		long [] dims = ipg.dims;
		double [] sigma = ipg.sigma; 
		long numSpots = ipg.numSpots;
		int seed = ipg.seed;
		boolean padding = ipg.padding;

		int numDimensions = ipg.numDimensions;

		// paths to the data
		String fullImgPath = path + imgName + "-" + numSpots + "-" + sigma[0] + "-" + sigma[1] + "-" + seed + ".tif";
		String fullPosPath = path + posName + "-" + numSpots + "-" + sigma[0] + "-" + sigma[1] + "-" + seed + ".csv";

		Img<FloatType> img = ImgLib2Util.openAs32Bit(new File(fullImgPath));
		ArrayList<double[]> positions = new ArrayList<>();

		IOFunctions.readCSV(positions, fullPosPath, numDimensions);

		ImageJFunctions.show(img);
		RadialSymmetryParameters rsm = ipg.rsm;
		test(img, positions, rsm, numDimensions, dims);
	}
	
	public static void runTest3D(){
		// initialize params for this specific test case
		InputParamsGenerated ipg = new InputParamsGenerated("", 1);
		// InputParamsGenerated.setParameters2D();
		// initialize the parameters
		String path = ipg.path;
		String imgName = ipg.imgName;
		String posName = ipg.posName;
		long [] dims = ipg.dims;
		double [] sigma = ipg.sigma; 
		long numSpots = ipg.numSpots;
		int seed = ipg.seed;
		boolean padding = ipg.padding;

		int numDimensions = ipg.numDimensions;

		// paths to the data
		String fullImgPath = path + imgName + "-" + numSpots + "-" + sigma[0] + "-" + sigma[1] + "-" + seed + ".tif";
		String fullPosPath = path + posName + "-" + numSpots + "-" + sigma[0] + "-" + sigma[1] + "-" + seed + ".csv";

		Img<FloatType> img = ImgLib2Util.openAs32Bit(new File(fullImgPath));
		ArrayList<double[]> positions = new ArrayList<>();

		IOFunctions.readCSV(positions, fullPosPath, numDimensions);

		ImageJFunctions.show(img);
		RadialSymmetryParameters rsm = ipg.rsm;
		test(img, positions, rsm, numDimensions, dims);
	}

	public static void main(String [] args) {
		new ImageJ();
		runTest2D();
	}
}
