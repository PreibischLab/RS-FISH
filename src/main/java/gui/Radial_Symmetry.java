package gui;

import java.util.ArrayList;
import java.util.concurrent.Future;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.multithreading.SimpleMultiThreading;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.command.CommandModule;
import org.scijava.command.CommandService;
import org.scijava.command.ContextCommand;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import compute.RadialSymmetry;
import fit.Spot;
import gauss.GaussianMaskFit;
import gui.imagej.GenericDialogGUIParams;
import gui.interactive.HelperFunctions;
import gui.interactive.InteractiveRadialSymmetry;
import gui.vizualization.Visualization;
import ij.ImagePlus;
import ij.ImageStack;
import imglib2.RealTypeNormalization;
import imglib2.TypeTransformingRandomAccessibleInterval;
import intensity.Intensity;
import parameters.GUIParams;
import parameters.RadialSymmetryParameters;
import result.output.ShowResult;
import test.TestGauss3d;

@Plugin(type = Command.class, menuPath = "Plugins>Radial Symmetry Localization>Radial Symmetry")
public class Radial_Symmetry extends ContextCommand {
	// used to save previous values of the fields
	public static String[] paramChoice = new String[] { "Manual", "Interactive" };
	public static int defaultImg = 0;
	public static int defaultParam = 0;
	public static boolean defaultGauss = false; 
	public static boolean defaultRANSAC = true;
	public static float defaultAnisotropy = 1.0f; 

	public static boolean defaultDetections = true;
	public static boolean defaultInliers = false; 

	// steps per octave
	public static int defaultSensitivity = 4;

	@Parameter(autoFill=false, label="Image")
	ImagePlus imp;

	@Parameter(choices={ "Manual", "Interactive" }, label="Parameter's mode")
	String parameterType = paramChoice[defaultParam];

	@Parameter(label="Anisotropy coefficient")
	float anisotropy = defaultAnisotropy;

	@Parameter(label=" ", visibility=ItemVisibility.MESSAGE, persist = false)
	String anisotropyLabel = "<html>*Use the \"Anisotropy Coeffcient Plugin\"<br/>to calculate the anisotropy coefficient<br/> or leave 1.00 for a reasonable result.";

	@Parameter(label="<html><b>Computation:</h>", visibility=ItemVisibility.MESSAGE)
	String computationLabel = "";
	@Parameter(label="RANSAC")
	boolean RANSAC = defaultRANSAC;
	// use gauss fit 
	@Parameter(label="Gaussian fitting")
	boolean gaussFit = defaultGauss; // defines if we perform the gauss fit or linear interpolation for peak intensities

	@Parameter(label="<html><b>Visualization:</b>", visibility=ItemVisibility.MESSAGE)
	String visualizationLabel = "";
	@Parameter(label="Detections overlay")
	boolean showDetections = defaultDetections;
	@Parameter(label="RANSAC regions")
	boolean showInliers = defaultInliers;	

	// defines the resolution in x y z dimensions
	double[] calibration;

	// logging + error message; used instead of the IO.log
	@Parameter
	LogService logService;

	@Parameter
	CommandService commandService;

	@Override
	public void run() {
		if (this.isCanceled()) return;
		if (imp.getNChannels() > 1)
			logService.info("Multichannel image detected. We recommend to adjust the parameters for each channel separately.");

		// make some dirty code as it is not defined at compile time, but
		// for all subsequent code it is
		double[] minmax = HelperFunctions.computeMinMax((Img) ImageJFunctions.wrapReal(imp));

		float min = (float) minmax[0];
		float max = (float) minmax[1];

		// set all defaults + set RANSAC
		final GUIParams params = new GUIParams(RANSAC);

		params.setAnisotropyCoefficient(anisotropy);
		if (parameterType.equals(paramChoice[0])) // manual
		{
			// set the parameters in the manual mode
			try {
				boolean isCanceled = commandService.run(GenericDialogGUIParams.class, true, "guiParams", params).get().isCanceled();
				if (isCanceled) return;
			} catch (Exception e){
				logService.info("Internal exception caught");
			}
			// calculations are performed further
		} else // interactive
		{
			InteractiveRadialSymmetry irs = new InteractiveRadialSymmetry(imp, params, min, max);
			do {
				// TODO: change to something that is not deprecated
				SimpleMultiThreading.threadWait(100);
			} while (!irs.isFinished());

			if (irs.wasCanceled()) return;
		}

		// back up the parameter values to the default variables
		params.setDefaultValues();
		calibration = HelperFunctions.initCalibration(imp, imp.getNDimensions()); 

		RadialSymmetryParameters rsm = new RadialSymmetryParameters(params, calibration);

		// normalize the whole image if it is possible
		RandomAccessibleInterval<FloatType> rai;
		if (!Double.isNaN(min) && !Double.isNaN(max)) // if normalizable
			rai = new TypeTransformingRandomAccessibleInterval<>(ImageJFunctions.wrap(imp),
					new RealTypeNormalization<>(min, max - min), new FloatType());
		else // otherwise use
			rai = ImageJFunctions.wrap(imp);

		int[] impDim = imp.getDimensions(); // x y c z t
		long[] dim = HelperFunctions.getDimensions(impDim); // x y z 

		ArrayList<Spot> allSpots = new ArrayList<>(0);
		// stores number of detected spots per time point
		ArrayList<Long> timePoint = new ArrayList<>(0);
		// stores number of detected spots per channel 
		ArrayList<Long> channelPoint = new ArrayList<>(0);
		// stores the intensity values for gauss fitting
		ArrayList<Float> intensity = new ArrayList<>(0);

		RadialSymmetry.processSliceBySlice(imp, rai, rsm, impDim, dim, gaussFit, params.getSigmaDoG(), allSpots, timePoint,
				channelPoint, intensity);

		if (parameterType.equals("Interactive")){
			ShowResult.ransacResultTable(allSpots, timePoint, channelPoint, intensity);
			Visualization.showVisualization(imp, allSpots, intensity, showInliers, showDetections);
		}
		else{ // manual 
			//write the result to the csv file
		}
	}

	public static void main(String[] args) {
		// for the historical reasons
		System.out.println("DOGE!");
	}
}
