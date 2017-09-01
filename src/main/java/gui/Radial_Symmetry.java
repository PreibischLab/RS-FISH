package gui;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.util.ArrayList;
import java.util.Map;

import compute.RadialSymmetry;
import fit.Spot;
import gauss.GaussianMaskFit;
import gui.imagej.GenericDialogGUIParams;
import gui.interactive.HelperFunctions;
import gui.interactive.InteractiveRadialSymmetry;
import ij.CompositeImage;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.io.Opener;
import ij.plugin.PlugIn;
import imglib2.RealTypeNormalization;
import imglib2.TypeTransformingRandomAccessibleInterval;
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
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.multithreading.SimpleMultiThreading;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import parameters.GUIParams;
import parameters.RadialSymmetryParameters;
import test.TestGauss3d;
import visualization.Detections;
import visualization.Inliers;

public class Radial_Symmetry implements PlugIn {
	// used to save previous values of the fields
	public static String[] paramChoice = new String[] { "Manual", "Interactive" };
	public static int defaultImg = 0;
	public static int defaultParam = 0;
	public static boolean defaultGauss = true; 
	public static boolean defaultRANSAC = true;
	public static float defaultAnisotropy = 1.0f; 

	public static boolean defaultDetections = true;
	public static boolean defaultInliers = false; 

	// steps per octave
	public static int defaultSensitivity = 4;

	// TODO: used to choose the image
	ImagePlus imp;
	int parameterType;
	boolean RANSAC;
	float anisotropy;
	// use gaussfit 
	boolean gaussFit; // defines if we perform the gauss fit or linear interpolation for peak intensities
	boolean showDetections;
	boolean showInliers;

	// defines the resolution in x y z dimensions
	double[] calibration;

	@Override
	public void run(String arg) {
		boolean wasCanceled = chooseImageDialog();

		if (wasCanceled) return;

		wasCanceled = initialDialog();

		if (!wasCanceled) // if user didn't cancel
		{
			if (imp.getNChannels() > 1) {
				// TODO: REMOVE: This part is fixed
				// IJ.log("Multichannel images are not supported yet ...");
				// return;
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

			if (parameterType == 0) // Manual
			{
				// set the parameters in the manual mode
				GenericDialogGUIParams gdGUIParams = new GenericDialogGUIParams(params);
				wasCanceled = gdGUIParams.automaticDialog();

				if (wasCanceled)
					return;

				// calculations are performed further
			} else // interactive
			{
				params.setAnisotropyCoefficient(anisotropy);

				InteractiveRadialSymmetry irs = new InteractiveRadialSymmetry(imp, params, min, max);

				do {
					SimpleMultiThreading.threadWait(100);
				} while (!irs.isFinished());

				if (irs.wasCanceled())
					return;
			}

			// back up the parameter values to the default variables
			params.setDefaultValues();
			calibration = HelperFunctions.initCalibration(imp, imp.getNDimensions()); 

			RadialSymmetryParameters rsm = new RadialSymmetryParameters(params, calibration);

			RandomAccessibleInterval<FloatType> rai;
			if (!Double.isNaN(min) && !Double.isNaN(max)) // if normalizable
				rai = new TypeTransformingRandomAccessibleInterval<>(ImageJFunctions.wrap(imp),
						new RealTypeNormalization<>(min, max - min), new FloatType());
			else // otherwise use
				rai = ImageJFunctions.wrap(imp);

			// RandomAccessibleInterval<FloatType> rai = new
			// TypeTransformingRandomAccessibleInterval<>(
			// ImageJFunctions.wrap(imp), new RealTypeNormalization<>( min, max
			// - min ), new FloatType() );
			// x y c z t
			int[] impDim = imp.getDimensions();

			long[] dim; // stores x y z dimensions
			if (impDim[3] == 1) { // if there is no z dimension
				dim = new long[] { impDim[0], impDim[1] };
			} else { // 3D image
				dim = new long[] { impDim[0], impDim[1], impDim[3] };
			}

			RandomAccessibleInterval<FloatType> timeFrame;
			ArrayList<Spot> allSpots = new ArrayList<>(1);

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

			// Visualization incoming
			if (showInliers)
				Inliers.showInliers(ImageJFunctions.wrapReal(imp), allSpots);
			if (showDetections)
				new Detections(ImageJFunctions.wrapReal(imp), allSpots).showDetections();
		}
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
				ArrayList<Spot> filteredSpots = HelperFunctions.filterSpots(rs.getSpots(), 1 );

				allSpots.addAll(filteredSpots);

				// set the number of points found for the current time step
				timePoint.add(new Long(filteredSpots.size()));

				// user wants to have the gauss fit here
				if (gaussFit) {
					// fitGaussianMask(timeFrame, rs.getSpots(), sigma);

					// TODO: make spot implement Localizable and just return the original location for the Localize methods
					// HelperFunctions.copyToLocalizable(filteredSpots, peaks);

					double [] typicalSigmas = new double[numDimensions];
					for (int d = 0; d < numDimensions; d++)
						typicalSigmas[d] = sigma;

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

				// IOFunctions.println("t: " + t + " " + "c: " + c);
			}
			if (c != 0)
				channelPoint.add(new Long(allSpots.size() - channelPoint.get(c)));
			else
				channelPoint.add(new Long(allSpots.size()));
		}

	}
	
	
	

	// triggers the gaussian fit if user wants it
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


	// user chooses the image here
	protected boolean chooseImageDialog(){
		boolean failed = false;
		// check that the are images
		final int[] imgIdList = WindowManager.getIDList();
		if (imgIdList == null || imgIdList.length < 1) {
			IJ.error("You need at least one open image.");
			failed = true;
		}
		else{
			// titles of the images
			final String[] imgList = new String[imgIdList.length];
			for (int i = 0; i < imgIdList.length; ++i)
				imgList[i] = WindowManager.getImage(imgIdList[i]).getTitle();

			if (defaultImg >= imgList.length)
				defaultImg = 0;

			GenericDialog gd = new GenericDialog("Choose the image");
			gd.addChoice("Image_for_detection", imgList, imgList[defaultImg]);
			gd.showDialog();

			if (gd.wasCanceled()) {
				failed = true;
			} else {
				int tmp = defaultImg = gd.getNextChoiceIndex();
				this.imp = WindowManager.getImage(imgIdList[tmp]);
			}
		}

		return failed;
	}



	/*
	 * shows the initial GUI dialog user has to choose an image a processing
	 * method -- advanced/interactive
	 */
	protected boolean initialDialog() {
		boolean failed = false;


		// choose image to process and method to use
		GenericDialog initialDialog = new GenericDialog("Initial Setup");

		initialDialog.addChoice("Define_Parameters", paramChoice, paramChoice[defaultParam]);
		initialDialog.addCheckbox("Do_additional_gauss_fit", defaultGauss);
		initialDialog.addCheckbox("Use_RANSAC", defaultRANSAC);
				
		if (imp.getNDimensions() != 2)
			initialDialog.addNumericField("Anisotropy_coefficient", defaultAnisotropy, 2);

		initialDialog.addMessage("*Use the \"Anisotropy Coeffcient Plugin\"\nto calculate the coefficient or\n leave 1.0 for a reasonable result.", new Font("Arial", 0, 10), new Color(255, 0, 0));

		initialDialog.addMessage("Visualization:");
		initialDialog.addCheckbox("Show_RANSAC_results", defaultInliers);
		initialDialog.addCheckbox("Show_detections", defaultDetections);

		
		
		initialDialog.showDialog();

		if (initialDialog.wasCanceled()) {
			failed = true;
		} else {
			// Save current index and current choice here
			this.parameterType = defaultParam = initialDialog.getNextChoiceIndex();
			this.gaussFit = defaultGauss = initialDialog.getNextBoolean();
			this.RANSAC = defaultRANSAC = initialDialog.getNextBoolean();
		
			if (imp.getNDimensions() != 2)
				defaultAnisotropy = (float)initialDialog.getNextNumber();
			this.anisotropy = defaultAnisotropy;
		
			this.showInliers = defaultInliers = initialDialog.getNextBoolean();
			this.showDetections = defaultDetections = initialDialog.getNextBoolean();
			
		}

		return failed;
	}

	public static void main(String[] args) {
		// File path = new File( "/Volumes/Samsung_T3/2017-08-07-stephan-radial-symmetry-pipeline/Simulated_3D_2x.tif" );
		File path = new File( "/media/milkyklim/Samsung_T3/2017-08-24-intronic-probes/N2_dpy-23_ex_int_ama-1_015/channels/c3/N2_dpy-23_ex_int_ama-1_015.nd2 - N2_dpy-23_ex_int_ama-1_015.nd2 (series 03) - C=2-32.tif" );
		// File path = new File( "/home/milkyklim/Desktop/Image 0-1-1000.tif" );
		
		if (!path.exists())
			throw new RuntimeException("'" + path.getAbsolutePath() + "' doesn't exist.");

		new ImageJ();
		System.out.println("Opening '" + path + "'");

		ImagePlus imp = new Opener().openImage(path.getAbsolutePath());

		if (imp == null)
			throw new RuntimeException("image was not loaded");

		imp.show();

		imp.setSlice(121);

		// new Radial_Symmetry().chooseImageDialog();

		new Radial_Symmetry().run(new String());
		System.out.println("Doge!");
	}
}
