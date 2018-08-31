package test.paper.radial.symmetry;

import java.io.File;
import java.util.ArrayList;

import net.imagej.ImageJ;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.real.FloatType;

import fitting.Spot;
import gui.radial.symmetry.interactive.HelperFunctions;
import gui.radial.symmetry.result.ShowResult;
import gui.radial.symmetry.vizualization.Visualization;
import ij.ImagePlus;
import imglib2.type.numeric.real.normalized.RealTypeNormalization;
import imglib2.type.numeric.real.normalized.TypeTransformingRandomAccessibleInterval;
import radial.symmetry.computation.RadialSymmetry;
import radial.symmetry.parameters.RadialSymmetryParameters;
import util.ImgLib2Util;

public class RunTestReal {
	// TODO: create the ground truth for this test case
	
	public static void test(Img<FloatType> img, RadialSymmetryParameters rsm, int numDimensions, long [] dimensions) {
		// 1. find the spots using RS 
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

		int [] fullDim = new int[] {1, 1, 1, 1, 1};

		for (int d = 0; d < numDimensions; d++)
		{
			// if (d == 2) d++;
			fullDim[d == 2 ? d + 1 : d] = (int) dimensions[d];
		}
		
		RadialSymmetry.processSliceBySlice(img, rai, rsm, fullDim,
				spots, timePoint, channelPoint, intensity);

		if (true) {
			// some feedback
			ImagePlus imp = ImageJFunctions.wrap(img, "");
			imp.setDimensions(1, imp.getStackSize(), 1);
			Visualization.showVisualization(imp, spots, intensity, timePoint, true, true,
					rsm.getParams().getSigmaDoG(), rsm.getParams().getAnisotropyCoefficient());
			double histThreshold = Visualization.getHistThreshold(); // used to show the overlays
			ShowResult.ransacResultTable(spots, timePoint, channelPoint, intensity, histThreshold);
		}
	}
	
	public static void runTest3D() {
		// initialize params for this specific test case
		InputParamsReal ipg = new InputParamsReal("/media/milkyklim/1TB/2018-02-21-paper-radial-symmetry-test/filtered-images/", "Result of C3-N2_dpy-23_ex_int_ama-1_014.nd2 - N2_dpy-23_ex_int_ama-1_014.nd2 (series 01)-1.tif", 1);
		// initialize the parameters
		String path = ipg.path;
		String imgName = ipg.imgName;
		int numDimensions = ipg.numDimensions;

		// paths to the data
		String fullImgPath = path + imgName;

		Img<FloatType> img = ImgLib2Util.openAs32Bit(new File(fullImgPath));

		long [] dims = new long [numDimensions];
		img.dimensions(dims);

		ImageJFunctions.show(img);
		RadialSymmetryParameters rsm = ipg.rsm;
		test(img, rsm, numDimensions, dims);
	}

	public static void runTestMax2D() {

		// initialize params for this specific test case
		InputParamsReal ipg = new InputParamsReal("/media/milkyklim/1TB/2018-02-21-paper-radial-symmetry-test/max-project-images/", "MAX_Result of C3-N2_dpy-23_ex_int_ama-1_014.nd2 - N2_dpy-23_ex_int_ama-1_014.nd2 (series 01).tif", 0);
		// initialize the parameters
		String path = ipg.path;
		String imgName = ipg.imgName;
		int numDimensions = ipg.numDimensions;

		// paths to the data
		String fullImgPath = path + imgName;

		Img<FloatType> img = ImgLib2Util.openAs32Bit(new File(fullImgPath));

		long [] dims = new long [numDimensions];
		img.dimensions(dims);

		ImageJFunctions.show(img);
		RadialSymmetryParameters rsm = ipg.rsm;
		test(img, rsm, numDimensions, dims);
	}

	public static void main(String [] args) {
		new ImageJ();
		runTest3D();
	}
}
