package gui.radial.symmetry.plugin;

import java.util.ArrayList;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.multithreading.SimpleMultiThreading;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;

import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.command.ContextCommand;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import compute.RadialSymmetry;
import fitting.Spot;
import gui.imagej.GenericDialogGUIParams;
import gui.interactive.HelperFunctions;
import gui.interactive.InteractiveRadialSymmetry;
import gui.vizualization.Visualization;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.Roi;
import imglib2.RealTypeNormalization;
import imglib2.TypeTransformingRandomAccessibleInterval;
import parameters.GUIParams;
import parameters.RadialSymmetryParameters;
import result.output.ShowResult;

@Plugin(type = Command.class, menuPath = "Plugins>Radial Symmetry Localization>Radial Symmetry")
public class Radial_Symmetry extends ContextCommand {
	// Defaults: used to save previous values of the plugin fields
	public static String[] paramChoice = new String[] { "Manual", "Interactive" };
	// public static int defaultImg = 0;
	public static int defaultParam = 0;
	public static boolean defaultGauss = false;
	public static boolean defaultRANSAC = true;
	public static float defaultAnisotropy = 1.0f;

	public static boolean defaultDetections = true;
	public static boolean defaultInliers = false;

	// steps per octave for DoG
	public static int defaultSensitivity = 4;

	// TODO:
	// https://github.com/scijava/scijava-common/issues/42#issuecomment-332724692
	// that the answer to the question how to hide some of the elements of the
	// gui

	@Parameter(autoFill = false, label = "Image")
	ImagePlus imp;

	@Parameter(choices = { "Manual", "Interactive" }, label = "Parameter's mode")
	String parameterType = paramChoice[defaultParam];

	@Parameter(label = "Anisotropy coefficient")
	float anisotropy = defaultAnisotropy;

	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	String anisotropyLabel = "<html>*Use the \"Anisotropy Coeffcient Plugin\"<br/>to calculate the anisotropy coefficient<br/> or leave 1.00 for a reasonable result.";

	@Parameter(label = "<html><b>Computation:</h>", visibility = ItemVisibility.MESSAGE)
	String computationLabel = "";
	@Parameter(label = "RANSAC")
	boolean RANSAC = defaultRANSAC;
	// use gauss fit
	// defines if we perform the gauss fit or
	// linear interpolation for peak intensities
	// @Parameter(label = "Gaussian fitting")
	boolean gaussFit = defaultGauss;

	@Parameter(label = "<html><b>Visualization:</b>", visibility = org.scijava.ItemVisibility.MESSAGE)
	String visualizationLabel = "";
	@Parameter(label = "Detections overlay")
	boolean showDetections = defaultDetections;
	@Parameter(label = "RANSAC regions")
	boolean showInliers = defaultInliers;

	// defines the resolution in x y z dimensions
	double[] calibration;

	// logging + error message; used instead of the IO.log
	@Parameter(visibility = org.scijava.ItemVisibility.INVISIBLE)
	LogService logService;

	@Parameter(visibility = ItemVisibility.INVISIBLE)
	CommandService commandService;

	@Override
	public void run() {
		if (this.isCanceled())
			return;
		if (imp.getNChannels() > 1)
			logService.info(
					"Multichannel image detected. We recommend to adjust the parameters for each channel separately.");

		// dirty cast that can't be avoided :(
		double[] minmax = HelperFunctions.computeMinMax((Img) ImageJFunctions.wrapReal(imp));

		float min = (float) minmax[0];
		float max = (float) minmax[1];

		// set the parameters from the defaults
		final GUIParams params = new GUIParams();
		// the 2 below we adjust here because they are defined in the gui
		params.setAnisotropyCoefficient(anisotropy);
		params.setRANSAC(RANSAC);
		params.setGaussFit(gaussFit);

		
		// DEBUG: 
		// System.out.println(gaussFit + " " + RANSAC);
		
		
		if (parameterType.equals(paramChoice[0])) // manual
		{
			// set the parameters in the manual mode
			try {
				boolean isCanceled = commandService.run(GenericDialogGUIParams.class, true, "guiParams", params).get()
						.isCanceled();
				if (isCanceled)
					return;
			} catch (Exception e) {
				logService.info("Internal exception caught");
				e.printStackTrace();
			}
			// calculations are performed further
		} else // interactive
		{
			InteractiveRadialSymmetry irs = new InteractiveRadialSymmetry(imp, params, min, max);
			do {
				// TODO: change to something that is not deprecated
				SimpleMultiThreading.threadWait(100);
			} while (!irs.isFinished());

			if (irs.wasCanceled())
				return;
		}

		// back up the parameter values to the default variables
		// params.setDefaultValues();
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

		ArrayList<Spot> allSpots = new ArrayList<>(0);
		// stores number of detected spots per time point
		ArrayList<Long> timePoint = new ArrayList<>(0);
		// stores number of detected spots per channel
		ArrayList<Long> channelPoint = new ArrayList<>(0);
		// stores the intensity values
		ArrayList<Float> intensity = new ArrayList<>(0);

		// Ensure that the input is a FloatType
		RandomAccessibleInterval<RealType> wrapped = ImageJFunctions.wrapReal(imp);
		RandomAccessibleInterval<FloatType> input = Converters.convert(
				wrapped,
				(a, b) -> b.setReal(a.getRealFloat()),
				new FloatType());

		RadialSymmetry.processSliceBySlice(input, rai, rsm, impDim, allSpots, timePoint, channelPoint, intensity);

		if (parameterType.equals(paramChoice[1])) { // interactive
			// TODO: keep here?
			imp.deleteRoi();

			Visualization.showVisualization(imp, allSpots, intensity, timePoint, showInliers, showDetections,
					params.getSigmaDoG(), params.getAnisotropyCoefficient());
			double histThreshold = Visualization.getHistThreshold(); // used to show the overlays
			ShowResult.ransacResultTable(allSpots, timePoint, channelPoint, intensity, histThreshold);
		} else if (parameterType.equals(paramChoice[0])) { // manual
			// write the result to the csv file
			double histThreshold = 0; // take all of the points that were detected
			ShowResult.ransacResultTable(allSpots, timePoint, channelPoint, intensity, histThreshold);
		} else
			System.out.println("Wrong parameters' mode");

	}

	public void setDefaultParams() {
		// defaultImg = ; // TODO: 
		// defaultParam = 0;
		defaultGauss = gaussFit;
		defaultRANSAC = RANSAC;
		defaultAnisotropy = anisotropy;
		defaultDetections = showDetections;
		defaultInliers = showInliers;
	}



	public static void main(String[] args) {
		net.imagej.ImageJ ij = new net.imagej.ImageJ();
		//ij.launch( "/Users/spreibi/Downloads/N2_267-1.tif" );
		//ij.launch( "/Users/spreibi/Downloads/C0-N2_352_cropped_1240.tif" );
		ij.launch( "/home/kharrington/Data/Radial_Symmetry/N2_352-1.tif" );
		ij.command().run(Radial_Symmetry.class, true);

	}
}
