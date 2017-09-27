package gui;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import net.imagej.Dataset;
import net.imagej.legacy.LegacyService;
import net.imagej.patcher.LegacyInjector;
import net.imglib2.Cursor;
import net.imglib2.Localizable;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.algorithm.localization.EllipticGaussianOrtho;
import net.imglib2.algorithm.localization.LevenbergMarquardtSolver;
import net.imglib2.algorithm.localization.MLEllipticGaussianEstimator;
import net.imglib2.algorithm.localization.PeakFitter;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.multithreading.SimpleMultiThreading;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.command.CommandModule;
import org.scijava.command.CommandService;
import org.scijava.command.ContextCommand;
import org.scijava.convert.ConvertService;
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
import histogram.Histogram;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;

import imglib2.RealTypeNormalization;
import imglib2.TypeTransformingRandomAccessibleInterval;
import parameters.GUIParams;
import parameters.RadialSymmetryParameters;
import test.TestGauss3d;
import visualization.Detections;
import visualization.Inliers;

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
	
	// TODO: used to choose the image
	@Parameter(autoFill=false, label="Image")
	ImagePlus imp;

	@Parameter(choices={ "Manual", "Interactive" }, label="Parameter's mode")
	String parameterType = paramChoice[defaultParam];

	@Parameter(label="Anisotropy coefficient")
	float anisotropy = defaultAnisotropy;

	@Parameter(label=" ", visibility=ItemVisibility.MESSAGE, persist = false)
	String anisotropyLabel = "<html>*Use the \"Anisotropy Coeffcient Plugin\"<br/>to calculate the anisotropy coefficient<br/> or leave 1.00 for a reasonable result.";

	// @Parameter
	// int parameterType = defaultParam;
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

		if (imp.getNChannels() > 1) {
			logService.info("Multichannel image detected. We recommend to adjust the parameters for each channel separately.");
		}

		long[] dimensions = new long[imp.getNDimensions()];
		for (int d = 0; d < imp.getNDimensions(); ++d) {
			dimensions[d] = imp.getDimensions()[d];
		}

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
			Future<CommandModule> run = commandService.run(GenericDialogGUIParams.class, true, "guiParams", params);
			try {
				CommandModule commandModule = run.get();
				if (commandModule.isCanceled()) return;
			} catch (Exception e) {
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

		// x y c z t
		int[] impDim = imp.getDimensions();

		long[] dim; // stores x y z dimensions
		if (impDim[3] == 1) { // if there is no z dimension
			dim = new long[] { impDim[0], impDim[1] };
		} else { // 3D image
			dim = new long[] { impDim[0], impDim[1], impDim[3] };
		}

		ArrayList<Spot> allSpots = new ArrayList<>(0);

		// stores number of detected spots per time point
		ArrayList<Long> timePoint = new ArrayList<>(0);
		// stores number of detected spots per channel
		ArrayList<Long> channelPoint = new ArrayList<>(0);
		// stores the intensity values for gauss fitting
		ArrayList<Float> intensity = new ArrayList<>(0);

		processSliceBySlice(imp, rai, rsm, impDim, dim, gaussFit, params.getSigmaDoG(), allSpots, timePoint,
				channelPoint, intensity);

		// DEBUG:
		// System.out.println("total # of channels " + channelPoint.size());
		// System.out.println("total # of timepoits" + timePoint.size());
		
		RadialSymmetry.ransacResultTable(allSpots, timePoint, channelPoint, intensity);
		Visualization.showVisualization(imp, allSpots, intensity, showInliers, showDetections);
	}

	// process each 2D/3D slice of the image to search for the spots
	public static void processSliceBySlice(ImagePlus imp, RandomAccessibleInterval<FloatType> rai, RadialSymmetryParameters rsm,
			int[] impDim, long[] dim, boolean gaussFit, double sigma, ArrayList<Spot> allSpots,
			ArrayList<Long> timePoint, ArrayList<Long> channelPoint, ArrayList<Float> intensity) {
		RandomAccessibleInterval<FloatType> timeFrame;
		ImagePlus xyzImp; // stores non-normalized xyz stack

		int numDimensions = dim.length;

		// impDim <- x y c z t
		for (int c = 0; c < impDim[2]; c++) {
			for (int t = 0; t < impDim[4]; t++) {
				// "-1" because of the imp offset
				timeFrame = copyImg(rai, c, t, dim, impDim);

				RadialSymmetry rs = new RadialSymmetry(rsm, timeFrame);

				// TODO: if the detect spot has at least 1 inlier add it
				// FIXME: is this part necessary? 
				ArrayList<Spot> filteredSpots = HelperFunctions.filterSpots(rs.getSpots(), 1 );

				allSpots.addAll(filteredSpots);

				// set the number of points found for the current time step
				timePoint.add(new Long(filteredSpots.size()));

				// user wants to have the gauss fit here
				if (gaussFit) { // TODO: fix the problem with the computations of this one

					double [] typicalSigmas = new double[numDimensions];
					for (int d = 0; d < numDimensions; d++)
						typicalSigmas[d] = sigma;

					if (numDimensions == 3) typicalSigmas[numDimensions - 1] *= rsm.getParams().getAnisotropyCoefficient();

					xyzImp = getXyz(imp); // grabbed the non-normalized xyz-stack  

					PeakFitter<FloatType> pf = new PeakFitter<FloatType>(ImageJFunctions.wrap(xyzImp), (ArrayList)filteredSpots,
							new LevenbergMarquardtSolver(), new EllipticGaussianOrtho(), 
							new MLEllipticGaussianEstimator(typicalSigmas)); // use a non-symmetric gauss (sigma_x, sigma_y, sigma_z or sigma_xy & sigma_z)
					pf.process();

					// TODO: make spot implement Localizable - then this is already a HashMap that maps Spot > double[]
					// this is actually a Map< Spot, double[] >
					final Map< Localizable, double[] > fits = pf.getResult();

					// FIXME: is the order consistent
					for ( final Spot spot : filteredSpots )
					{
						double[] params = fits.get( spot );
						intensity.add(new Float(params[numDimensions]));
					}
				}
				else{
					//  iterate over all points and perform the linear interpolation for each of the spots

					xyzImp = getXyz(imp); // grabbed the non-normalized xyz-stack  
					NLinearInterpolatorFactory<FloatType> factory = new NLinearInterpolatorFactory<>();
					RealRandomAccessible<FloatType> interpolant = Views.interpolate(Views.extendMirrorSingle( ImageJFunctions.wrapFloat( xyzImp)), factory);

					for (Spot fSpot : filteredSpots){
						RealRandomAccess<FloatType> rra = interpolant.realRandomAccess();
						double[] position = fSpot.getCenter();
						rra.setPosition(position);
						intensity.add(new Float(rra.get().get()));	
					}

				}
			}
			if (c != 0)
				channelPoint.add(new Long(allSpots.size() - channelPoint.get(c)));
			else
				channelPoint.add(new Long(allSpots.size()));
		}

	}


	// triggers the gaussian fit if user wants it
	// FIXME: Gauss fit should be performed only on the inliers
	public static void fitGaussian(RandomAccessibleInterval<FloatType> timeFrame, ArrayList<Spot> spots, double sigma) {

		int numDimensions = timeFrame.numDimensions();

		// here we might have the 3D or 2D spot

		// TODO: Check that this part is working
		for (final Spot spot : spots) {
			// TODO: check that center is correct
			double[] location = new double[numDimensions];
			double[] sigmaSpot = new double[numDimensions];

			for (int d = 0; d < numDimensions; d++) {
				sigmaSpot[d] = sigma;
				location[d] = spot.getCenter()[d];
			}

			long[] minSpot = new long[numDimensions];
			long[] maxSpot = new long[numDimensions];
			HelperFunctions.setMinMaxLocation(location, sigmaSpot, minSpot, maxSpot);

			GaussianMaskFit.gaussianMaskFit(Views.interval(timeFrame, minSpot, maxSpot), location, sigmaSpot, null);

		}
	}

	// triggers the gaussian mask fit if user wants it
	public static void fitGaussianMask(RandomAccessibleInterval<FloatType> timeFrame, ArrayList<Spot> spots,
			double sigma) {

		int numDimensions = timeFrame.numDimensions();

		// here we might have the 3D or 2D spot

		// TODO: Check that this part is working
		for (final Spot spot : spots) {
			// TODO: check that center is correct
			double[] location = new double[numDimensions];
			double[] sigmaSpot = new double[numDimensions];

			for (int d = 0; d < numDimensions; d++) {
				sigmaSpot[d] = sigma;
				location[d] = spot.getCenter()[d];
			}

			long[] minSpot = new long[numDimensions];
			long[] maxSpot = new long[numDimensions];
			HelperFunctions.setMinMaxLocation(location, sigmaSpot, minSpot, maxSpot);

			GaussianMaskFit.gaussianMaskFit(Views.interval(timeFrame, minSpot, maxSpot), location, sigmaSpot, null);
		}
	}

	// DEBUG:
	public static void showPoints(Img<FloatType> image, ArrayList<Spot> spots, double[] sigma) {
		for (int i = 0; i < spots.size(); ++i) {
			// final Spot spot = spots.get(i);
			// if not discarded
			if (spots.get(i).numRemoved != spots.get(i).candidates.size()) {
				final double[] location = new double[] { spots.get(i).getFloatPosition(0),
						spots.get(i).getFloatPosition(1), spots.get(i).getFloatPosition(2) };
				TestGauss3d.addGaussian(image, location, sigma);
			}

		}
	}

	// returns only xyz stack from the ImagePlus object
	public static ImagePlus getXyz(ImagePlus imp){
		final ImageStack stack = new ImageStack(imp.getWidth(), imp.getHeight() );
		int [] impDimensions = imp.getDimensions();

		for (int z = 0; z < impDimensions[3]; z++){
			int id = imp.getStackIndex(1, z + 1, 1);
			stack.addSlice(imp.getStack().getProcessor( id ));
		}

		ImagePlus xyzImp = new ImagePlus("merge", stack );
		// xyzImp.setDimensions( 1, impDimensions[3], 1 );

		return xyzImp;
	}


	// clunky function to handle different space-time cases
	// TODO: check that it is working properly for all cases
	// TODO: can be rewritten with ImagePlus operations
	public static RandomAccessibleInterval<FloatType> copyImg(RandomAccessibleInterval<FloatType> rai, long channel,
			long time, long[] dim, int[] impDim) {
		// this one will be returned
		RandomAccessibleInterval<FloatType> img = ArrayImgs.floats(dim);

		Cursor<FloatType> cursor = Views.iterable(img).localizingCursor();
		RandomAccess<FloatType> ra = rai.randomAccess();

		long[] pos = new long[dim.length];

		while (cursor.hasNext()) {
			// move over output image
			cursor.fwd();
			cursor.localize(pos);
			// set the corresponding position (channel + time)
			if (impDim[2] != 1 && impDim[3] != 1 && impDim[4] != 1) { // full 5D
				// stack
				ra.setPosition(new long[] { pos[0], pos[1], channel, pos[2], time });
			} else {
				if (impDim[2] != 1 && impDim[3] != 1) { // channels + z
					ra.setPosition(new long[] { pos[0], pos[1], channel, pos[2] });
				} else if (impDim[2] != 1 && impDim[4] != 1) { // channels +
					// time
					ra.setPosition(new long[] { pos[0], pos[1], channel, time });
				} else if (impDim[3] != 1 && impDim[4] != 1) { // z + time
					ra.setPosition(new long[] { pos[0], pos[1], pos[2], time });
				} else if (impDim[2] != 1) { // c
					ra.setPosition(new long[] { pos[0], pos[1], pos[3] }); // fixed ?
				} else if (impDim[3] != 1) { // z
					ra.setPosition(new long[] { pos[0], pos[1], pos[2] });
				} else if (impDim[4] != 1) { // t
					ra.setPosition(new long[] { pos[0], pos[1], time }); // fix
				} else // 2D image
					ra.setPosition(new long[] { pos[0], pos[1] });
			}

			cursor.get().setReal(ra.get().getRealFloat());
		}

		return img;
	}

	public static void main(String[] args) {
		// for the historical reasons
		System.out.println("DOGE!");
	}
}
