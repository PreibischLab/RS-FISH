package gui;

import java.awt.Button;
import java.awt.Color;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Rectangle;
import java.awt.Scrollbar;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.SingularValueDecomposition;

import fiji.tool.SliceListener;
import fiji.tool.SliceObserver;
import fit.PointFunctionMatch;
import fit.Spot;
import gradient.Gradient;
import gradient.GradientPreCompute;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.io.Opener;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.plugin.RoiInterpolator;
import ij.process.FloatProcessor;
import mpicbg.imglib.multithreading.SimpleMultiThreading;
import mpicbg.imglib.util.Util;
import mpicbg.imglib.wrapper.ImgLib2;
import mpicbg.models.PointMatch;
import mpicbg.spim.io.IOFunctions;
import mpicbg.spim.registration.detection.DetectionSegmentation;
import net.imglib2.Cursor;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
// additional libraries to switch from imglib1 to imglib2
import net.imglib2.algorithm.dog.DogDetection;
import net.imglib2.algorithm.localextrema.RefinedPeak;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.img.imageplus.ImagePlusImgs;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

public class InteractiveRadialSymmetry implements PlugIn {
	// RANSAC parameters
	// initial values
	final int ransacInitSupportRadius = 10;
	final float ransacInitInlierRatio = 0.75f;
	final float ransacInitMaxError = 3;
	// current value
	int numIterations = 100;
	// important to keep them static here 
	static float maxError = 0.15f;
	static float inlierRatio = (float) (20.0 / 100.0);
	static int supportRadius = 10;
	// min/max value
	int supportRadiusMin = 1;
	int supportRadiusMax = 50;
	float inlierRatioMin = (float) (0.0 / 100.0); // 0%
	float inlierRatioMax = 1; // 100%
	float maxErrorMin = 0.0001f;
	float maxErrorMax = 10.00f;
	// ----------------------------------------

	// DoG parameters
	// initial
	final int sigmaInit = 5;
	final float thresholdInit = 0.03f;
	// current
	float sigma = 0.5f;
	float sigma2 = 0.5f;
	float threshold = 0.0001f;
	// min/max value
	float sigmaMin = 0.5f;
	float sigmaMax = 10f;
	float thresholdMin = 0.0001f;
	float thresholdMax = 1f;
	// --------------------------------

	// Stuff above looks super relevant
	// Only constants 
	// keep all for now 

	// TODO: keep these params
	final int extraSize = 40;
	final int scrollbarSize = 1000;
	float imageSigma = 0.5f;

	// TODO: keep these
	double minIntensityImage = Double.NaN;
	double maxIntensityImage = Double.NaN;

	// TODO: used to choose the image
	// I really want to make them local!
	String imgTitle; 
	String parameterAdjustment;

	// TODO: after moving to imglib2 REMOVE
	// steps per octave
	public static int standardSensitivity = 4;
	int sensitivity = standardSensitivity;

	// TODO: keep observers
	SliceObserver sliceObserver;
	RoiListener roiListener;

	// TODO: you probably need one image plus object 
	ImagePlus imp;
	int channel = 0;
	Rectangle rectangle;

	// TODO: You will use this (both)
	ArrayList<Point> peaks2;
	ArrayList<RefinedPeak<Point>> peaks3;

	// TODO: Variables for imglib1 to imglib2 conversion
	RandomAccessibleInterval<FloatType> slice;
	// TODO: attempt to move to imglib2? 
	RandomAccessibleInterval<FloatType> img2;

	Color originalColor = new Color(0.8f, 0.8f, 0.8f);
	Color inactiveColor = new Color(0.95f, 0.95f, 0.95f);

	// TODO: what the fuck is the difference between standardRectangle and Rectangle
	public Rectangle standardRectangle;
	// TODO: keep listeners flags
	boolean isComputing = false;
	boolean isStarted = false;

	// TODO: Do you realy need all of the parameters? 

	public static enum ValueChange {
		SIGMA, THRESHOLD, SLICE, ROI, ALL, SUPPORTREGION, INLIERRATIO, MAXERROR
	}

	boolean isFinished = false;
	boolean wasCanceled = false;

	public boolean isFinished() {
		return isFinished;
	}

	public boolean wasCanceled() {
		return wasCanceled;
	}

	public InteractiveRadialSymmetry(final ImagePlus imp, final int channel) {
		this.imp = imp;
		this.channel = channel;
	}

	public InteractiveRadialSymmetry(final ImagePlus imp) {
		this.imp = imp;
	}

	public InteractiveRadialSymmetry() {
	}

	protected boolean showInitialDialog(/*String imgTitle, String parameterAdjustment,*/){
		boolean failed = false;

		// check that the are images
		final int[] imgIdList = WindowManager.getIDList();
		if (imgIdList == null || imgIdList.length < 1) {
			IJ.error("You need at least one open image.");
			failed = true;
		}

		// titles of the images		
		final String[] imgList = new String[imgIdList.length];
		for (int i = 0; i < imgIdList.length; ++i)
			imgList[i] = WindowManager.getImage(imgIdList[i]).getTitle();

		// choose image to process and method to use
		GenericDialog initialDialog = new GenericDialog("Initial Setup");

		if (defaultImg >= imgList.length)
			defaultImg = 0;

		initialDialog.addChoice("Image for detection", imgList, imgList[defaultImg]);
		initialDialog.addChoice("Define_Parameters", paramChoice, paramChoice[defaultParam]);
		initialDialog.addCheckbox("Do_additional_gauss_fit", defaultGauss);
		initialDialog.showDialog();

		// TODO: I want ot use these as local varaibles but used as global instead
		// Save current index and current choice here 
		imgTitle = initialDialog.getNextChoice();
		parameterAdjustment = initialDialog.getNextChoice();

		if (initialDialog.wasCanceled())
			failed = true;

		return failed;
	}

	// necessary!
	@SuppressWarnings("unused")
	public int setup( String arg, ImagePlus imp) {
		return 0;
	}

	@Override
	public void run(String arg) {

		// TODO: MOVE ALL RETURN STATEMENTS TO THE VARIABLE + ONE RETURN STATEMENT

		/*
		 * Check if the image is there (+) Interactive/Offline gui if offline
		 * run the calcuclation else show dog gui show ransac gui after
		 * parameters are set run algo for the whole image or concede
		 */

		// move this return
		boolean initialDialogWasCanceled = showInitialDialog(/*imgTitle, parameterAdjustment, */);
		if (initialDialogWasCanceled)
			return; 

		System.out.println("Image used : " + imgTitle);
		System.out.println("Parameters : " + parameterAdjustment);

		if (imp == null)
			imp = WindowManager.getImage(imgTitle);
		// TODO: Check what of the stuff below is necessary
		// // if one of the images is rgb or 8-bit color convert them to
		// hyperstack
		// imp = Hyperstack_rearranger.convertToHyperStack( imp );
		//
		// // test if you can deal with this image (2d? 3d? channels?
		// timepoints?)
		// 3d + time should be fine.
		// right now works with 2d + time

		//TODO move this return
		if (imp.getType() == ImagePlus.COLOR_RGB || imp.getType() == ImagePlus.COLOR_256) {
			IJ.log("Color images are not supported, please convert to 8, 16 or 32-bit grayscale");
			return;
		}

		// if interactive
		if (parameterAdjustment.compareTo(paramChoice[1]) == 0) {
			// TODO: is this rectangle really necessary
			// here comes the normal work flow
			standardRectangle = new Rectangle(imp.getWidth() / 4, imp.getHeight() / 4, imp.getWidth() / 2,
					imp.getHeight() / 2);
			// TODO: Do I need this ROI?
			Roi roi = imp.getRoi();
			if (roi == null) {
				// IJ.log( "A rectangular ROI is required to define the area..." );
				imp.setRoi(standardRectangle);
				roi = imp.getRoi();
			}
			if (roi.getType() != Roi.RECTANGLE) {
				IJ.log("Only rectangular rois are supported...");
				return;
			}
			// TODO: this convertion looks totally fine!
			imp.setPosition(channel, imp.getSlice(), 0);
			slice = ImageJFunctions.convertFloat(imp);			
			// initialize variables for interactive preview
			// called before updatePreview() !
			ransacPreviewInitialize();
			// show the interactive kit
			displaySliders();
			// show the interactive ransac kit
			displayRansacSliders();
			// add listener to the imageplus slice slider
			sliceObserver = new SliceObserver(imp, new ImagePlusListener());

			// test of the slice


			// compute first version
			updatePreview(ValueChange.ALL);
			isStarted = true;
			// check whenever roi is modified to update accordingly
			roiListener = new RoiListener();
			imp.getCanvas().addMouseListener(roiListener);
		} 
		else 
		{
			// TODO: here we apply algo to the whole image 
			// TODO: another gui window pops up: to set up the parameters
			// here comes the normal work flow
			standardRectangle = new Rectangle(0, 0, imp.getWidth(), imp.getHeight());
			imp.setRoi(standardRectangle);
			// TODO: maybe you have to adjust intensities
			// img2 = ImageJFunctions.wrapFloat(imp);
			int curSliceIndex = imp.getSlice();
			imp.setPosition(channel, curSliceIndex, 0);
			slice = ImageJFunctions.convertFloat(imp);

			// initialize variables for the result
			ransacPreviewInitialize();
			// TODO: here you want to run the algorithm for every slice without interaction with the image
			// You need something like apply to stack button here
			// which you run once without any listeners
			displayAdvancedGenericDialog();
			// 			displayManualGUI();
			// check whenever roi is modified to update accordingly
			// roiListener = new RoiListener();
			// imp.getCanvas().addMouseListener(roiListener);
		}

	}

	// TODO: I believe that these processor thing can be avoided
	// Have a look what results you need and which you can skip
	// Looks like I have to use this for synchronization -- display <==> image
	FloatProcessor ransacFloatProcessor;
	ImagePlus impRansacError;
	// used to show the results -- error for ransac
	RandomAccessibleInterval<FloatType> ransacPreview;


	/**
	 * Initialize preview variables for RANSAC
	 */
	// TODO: might be not necessary
	protected void ransacPreviewInitialize() {		
		int width = (int)slice.dimension(0);
		int height = (int)slice.dimension(1);		

		ransacFloatProcessor = new FloatProcessor(width, height);

		float[] pixels = (float[]) ransacFloatProcessor.getPixels();
		impRansacError = new ImagePlus("RANSAC preview", ransacFloatProcessor);
		ransacPreview = ArrayImgs.floats(pixels, width, height);
		impRansacError.show();
	}


	// this function will show the result of RANSAC
	// proper window -> dialog view with the columns
	protected void showRansacResultTable(final ArrayList<Spot> spots) {
		IOFunctions.println("Running RANSAC ... ");
		IOFunctions.println("Spots found = " + spots.size());
		// real output
		ResultsTable rt = new ResultsTable();
		String[] xyz = { "x", "y", "z" };
		for (Spot spot : spots) {
			rt.incrementCounter();
			for (int d = 0; d < spot.numDimensions(); ++d) {
				rt.addValue(xyz[d], String.format(java.util.Locale.US, "%.2f", spot.getFloatPosition(d)));
			}
		}
		rt.show("Results");

	}


	// TODO: REMOVE WHEN DONE
	public static <T extends RealType<T>> void printCoordinates(RandomAccessibleInterval<T> img) {
		for (int d = 0; d < img.numDimensions(); ++d) {
			System.out.println("[" + img.min(d) + " " + img.max(d) + "] ");
		}
	}

	// extended or shorten the size of the boundaries
	protected <T extends RealType<T>> void adjustBoundaries(RandomAccessibleInterval<T> inImg, long[] size, long[] min,
			long[] max, long[] fullImgMax) {
		final int numDimensions = inImg.numDimensions();
		for (int d = 0; d < numDimensions; ++d) {
			min[d] = inImg.min(d) - size[d];
			max[d] = inImg.max(d) + size[d];
			// check that it does not exceed bounds of the underlying image
			min[d] = Math.max(min[d], 0);
			max[d] = Math.min(max[d], fullImgMax[d]);
		}
	}

	// check if peak is inside of the rectangle
	protected boolean isInside(RefinedPeak<Point> peak){
		final float x = peak.getFloatPosition(0);
		final float y = peak.getFloatPosition(1);

		boolean res = (x >= (rectangle.x) && y >= (rectangle.y) && 
				x < (rectangle.width + rectangle.x - 1) && y < (rectangle.height + rectangle.y - 1));

		return res;
	}

	/**
	 * Copy peaks found by DoG to lighter ArrayList (!imglib2)
	 */	
	protected void copyPeaks2(final ArrayList<long[]> simplifiedPeaks) {		
		int numDimensions = img2.numDimensions();
		long[] coordinates = new long[numDimensions];

		// TODO: here should be the threshold for the peak values
		for (final RefinedPeak<Point> peak : peaks3){
			// TODO: add threshold value
			if (isInside(peak)){
				for (int d = 0; d < peak.numDimensions(); ++d){
					coordinates[d] = Util.round(peak.getDoublePosition(d));
				}
				simplifiedPeaks.add(coordinates.clone()); // TODO: get rid of clone but we check that it is done correctly
			}
		}

	}


	protected void runRansac2() {
		final ArrayList<long[]> simplifiedPeaks = new ArrayList<>(1);

		// extract peaks for the roi
		copyPeaks2(simplifiedPeaks);
		int numDimensions = slice.numDimensions();

		final long[] range = new long[] { supportRadius, supportRadius };

		final long[] min = new long[numDimensions];
		final long[] max = new long[numDimensions];
		// hard coded because I there is no better function
		final long[] fullImgMax = new long[numDimensions];

		// TODO: check if you need this -1 here; comes from the imglib1	
		for (int d = 0; d < numDimensions; ++d)
			fullImgMax[d] = slice.dimension(d) - 1;

		IntervalView<FloatType> roi = Views.interval(slice, new long []{rectangle.x, rectangle.y}, new long []{rectangle.width + rectangle.x - 1, rectangle.height + rectangle.y - 1});
		adjustBoundaries(roi, range, min, max, fullImgMax);

		IntervalView<FloatType> extendedRoi =  Views.interval(slice, min, max); 

		// TODO: some bounding strategy might be necessary

		final Gradient derivative = new GradientPreCompute(extendedRoi);

		System.out.println("Debug: output: runRansac2()");


		final ArrayList<Spot> spots = Spot.extractSpots(extendedRoi, extendedRoi, simplifiedPeaks, derivative, range);

		// add the values for the gauss fit 
		final double[] peakValues = new double[spots.size()];

		System.out.println("hello: " + peakValues.length);

		// TODO: fix the gaussian fit! not 0 background
		// 

		// FIXME: !!! 
		// to sub properly and fit this part into the previously writtren
		// code you sub and add plane here 
		// the problem is that back-addition is not done!
		// TODO: CHECKED THE RESULT 
		// IT IS CORRECT FOR 2D 
		// THE RESULTS ARE SHOWN IN SOURCE NOT IMP

		// FIXME: This part was correct need to uncomment it


		for(int j = 0; j < spots.size(); ++j){
			double [] coefficients = new double [numDimensions + 1]; // z y x 1
			double [] position = new double [numDimensions]; // x y z
			long [] spotMin = new long [numDimensions];
			long [] spotMax = new long [numDimensions]; 

			backgroundSubtraction(spots.get(j), extendedRoi, coefficients, spotMin, spotMax);

			Cursor <FloatType> cursor = Views.interval(extendedRoi, spotMin, spotMax).localizingCursor();
			// System.out.println(coefficients[0] + " " + coefficients[1] + " " + coefficients[2]);

			while(cursor.hasNext()){
				cursor.fwd();
				cursor.localize(position);				
				double total = coefficients[numDimensions];	
				for (int d = 0; d < numDimensions; ++d){
					total += coefficients[d]*position[numDimensions - d - 1]; 
				}

				// DEBUG: 
				//				if (j == 0){
				//					System.out.println("before: " + cursor.get().get());
				//				}

				cursor.get().set(cursor.get().get() - (float)total);
				// DEBUG:
				//				if (j == 0){
				//					System.out.println("after:  " + cursor.get().get());
				//				}

			}		
		}

		// ImageJFunctions.show(source).setTitle("This one is actually modified with background subtraction");

		Spot.ransac(spots, numIterations, maxError, inlierRatio);
		for (final Spot spot : spots)
			spot.computeAverageCostInliers();
		showRansacResult(spots);
	}


	protected void runRansacInteractive() {
		final ArrayList<long[]> simplifiedPeaks = new ArrayList<>(1);

		// extract peaks for the roi
		copyPeaks2(simplifiedPeaks);

		int numDimensions = img2.numDimensions(); // DEBUG: should always be 2

		final long[] range = new long[numDimensions];
		final long[] min = new long[numDimensions];
		final long[] max = new long[numDimensions];
		final long[] fullImgMax = new long[numDimensions];

		// TODO: check if you need this -1 here; comes from the imglib1	
		for (int d = 0; d < numDimensions; ++d){
			fullImgMax[d] = slice.dimension(d) - 1; // TODO: slice or img2 ?! 
			range[d] = supportRadius;
		}

		IntervalView<FloatType> roi = Views.interval(img2, new long []{rectangle.x, rectangle.y}, new long []{rectangle.width + rectangle.x - 1, rectangle.height + rectangle.y - 1});
		adjustBoundaries(roi, range, min, max, fullImgMax);

		// ImageJFunctions.show(img2).setTitle("img2");
		// ImageJFunctions.show(roi).setTitle("roi");

		// TODO: some bounding strategy might be necessary
		final Gradient derivative = new GradientPreCompute(img2);
		final ArrayList<Spot> spots = Spot.extractSpots(roi, img2, simplifiedPeaks, derivative, range);

		// add the values for the gauss fit 
		final double[] peakValues = new double[spots.size()];

		System.out.println("hello: " + peakValues.length);

		// TODO: fix the gaussian fit! not 0 background
		// 

		// FIXME: !!! 
		// to sub properly and fit this part into the previously writtren
		// code you sub and add plane here 
		// the problem is that back-addition is not done!
		// TODO: CHECKED THE RESULT 
		// IT IS CORRECT FOR 2D 
		// THE RESULTS ARE SHOWN IN SOURCE NOT IMP

		// FIXME: This part was correct need to uncomment it


		//		for(int j = 0; j < spots.size(); ++j){
		//			double [] coefficients = new double [numDimensions + 1]; // z y x 1
		//			double [] position = new double [numDimensions]; // x y z
		//			long [] spotMin = new long [numDimensions];
		//			long [] spotMax = new long [numDimensions]; 
		//
		//			backgroundSubtraction(spots.get(j), extendedRoi, coefficients, spotMin, spotMax);
		//
		//			Cursor <FloatType> cursor = Views.interval(extendedRoi, spotMin, spotMax).localizingCursor();
		//			// System.out.println(coefficients[0] + " " + coefficients[1] + " " + coefficients[2]);
		//
		//			while(cursor.hasNext()){
		//				cursor.fwd();
		//				cursor.localize(position);				
		//				double total = coefficients[numDimensions];	
		//				for (int d = 0; d < numDimensions; ++d){
		//					total += coefficients[d]*position[numDimensions - d - 1]; 
		//				}
		//
		//				// DEBUG: 
		//				//				if (j == 0){
		//				//					System.out.println("before: " + cursor.get().get());
		//				//				}
		//
		//				cursor.get().set(cursor.get().get() - (float)total);
		//				// DEBUG:
		//				//				if (j == 0){
		//				//					System.out.println("after:  " + cursor.get().get());
		//				//				}
		//
		//			}		
		//		}

		// ImageJFunctions.show(source).setTitle("This one is actually modified with background subtraction");

		Spot.ransac(spots, numIterations, maxError, inlierRatio);
		for (final Spot spot : spots)
			spot.computeAverageCostInliers();
		showRansacResult(spots);
	}



	// TODO: at this point only uses corner values
	// TODO: extend to using the boundary values too

	protected void backgroundSubtraction(Spot spot, IntervalView<FloatType> roi, double[] coefficients, long[] min, long[] max){

		int numDimensions = spot.numDimensions();

		for (int d = 0; d < numDimensions; ++d){
			min[d] = Long.MAX_VALUE;
			max[d] = Long.MIN_VALUE;
		}

		for (PointMatch pm : spot.candidates){
			double [] coordinates = pm.getP1().getL();
			for (int d = 0; d < coordinates.length; ++d){			 
				if (min[d] > (long)coordinates[d]){
					min[d] = (long)coordinates[d];
				}
				if (max[d] < (long)coordinates[d]){
					max[d] = (long)coordinates[d];
				}	
			}		 
		}

		// this is a 2x2x..x2 hypercube it stores the values of the corners for plane fitting
		long [] valuesArray = new long[numDimensions];
		for (int d = 0; d < numDimensions; ++d)
			valuesArray[d] = 2;

		Img<FloatType> values = ArrayImgs.floats(valuesArray);			
		if (numDimensions > 3){
			System.out.println("Backgound Subtraction: the dimensionality is wrong");
		}

		// assign proper values to the corners)
		RandomAccess<FloatType> ra = roi.randomAccess();
		Cursor<FloatType> cursor = values.localizingCursor();

		double [][] A = new double[(int)values.size()][numDimensions + 1];
		double [] b = new double[(int)values.size()];

		int rowCount = 0;
		while(cursor.hasNext()){
			cursor.fwd();
			long[] initialPos = new long[numDimensions];
			double[] position = new double[numDimensions];
			cursor.localize(position);

			// z y x 1 order
			for (int d = 0; d < numDimensions; ++d){
				initialPos[d] =  (position[d] == 1 ? max[d] : min[d]);
				A[rowCount][numDimensions - d - 1] = (position[d] == 1 ? max[d] : min[d]);  
			}
			A[rowCount][numDimensions] = 1;

			ra.setPosition(initialPos);
			b[rowCount] = ra.get().get();

			// System.out.println(b[rowCount]);			
			rowCount++;
		}

		RealMatrix mA = new Array2DRowRealMatrix(A, false);
		RealVector mb = new ArrayRealVector(b, false);
		DecompositionSolver solver = new SingularValueDecomposition(mA).getSolver();
		RealVector mX =  solver.solve(mb);

		// System.out.println(coefficients[0] + " " + coefficients[1] + " " + coefficients[2]);

		// FIXME: This part is done outside of the function for now
		// subtract the values this part 
		// return the result
		// TODO: why proper copying is not working here ?! 
		for (int i  = 0; i < coefficients.length; i++)
			coefficients[i] = mX.toArray()[i];

		// System.out.println(coefficients[0] + " " + coefficients[1] + " " + coefficients[2]);


	}

	protected void runRansac3D() {
		final ArrayList<long[]> simplifiedPeaks = new ArrayList<>(1);
		// extract peaks for the roi

		//		for (final DifferenceOfGaussianPeak<mpicbg.imglib.type.numeric.real.FloatType> peak : peaks) {		
		//			simplifiedPeaks.add(new long[] { Util.round(peak.getPosition(0)), Util.round(peak.getPosition(1)), Util.round(peak.getPosition(2)) });
		//		}

		for (final RefinedPeak<Point> peak : peaks3){
			simplifiedPeaks.add(new long[] { Util.round(peak.getDoublePosition(0)), Util.round(peak.getDoublePosition(1)), Util.round(peak.getDoublePosition(2)) });
		}

		int numDimensions = img2.numDimensions();
		final long[] range = new long[] { supportRadius, supportRadius, supportRadius};

		final long[] min = new long[numDimensions];
		final long[] max = new long[numDimensions];

		for (int d = 0; d < numDimensions; ++d) {
			min[d] = 0;
			max[d] = img2.dimension(d) - 1;
		}

		// TODO: MIRROR STRATEGY ?!
		IntervalView<FloatType> extendedRoi = Views
				.interval(img2, min, max); 

		final Gradient derivative = new GradientPreCompute(extendedRoi);
		final ArrayList<Spot> spots = Spot.extractSpots(extendedRoi, extendedRoi, simplifiedPeaks, derivative, range);

		Spot.ransac(spots, numIterations, maxError, inlierRatio);
		for (final Spot spot : spots)
			spot.computeAverageCostInliers();

		// TODO: make a 3D output engine
		showRansacResultTable(spots);
	}

	protected void runRansacAdvanced23D(){
		final ArrayList<long[]> simplifiedPeaks = new ArrayList<>(1);
		int numDimensions = img2.numDimensions();
		long[] coordinates = new long[numDimensions];

		// TODO: here should be the threshold for the peak values
		for (final RefinedPeak<Point> peak : peaks3){
			// TODO: if threshold
			for (int d = 0; d < peak.numDimensions(); ++d){
				coordinates[d] = Util.round(peak.getDoublePosition(d));
				System.out.print(coordinates[d] + " ");
			}
			System.out.println();
			simplifiedPeaks.add(coordinates.clone()); // TODO: get rid of clone but we check that it is done correctly
		}

		// TODO: Do I need the adjustBoundaries function here

		final long[] range = new long[numDimensions];
		final long[] min = new long[numDimensions];
		final long[] max = new long[numDimensions];

		for (int d = 0; d <numDimensions; ++d){
			range[d] = supportRadius;
			min[d] = 0;
			max[d] = img2.dimension(d) - 1;
		}

		// TODO: MIRROR STRATEGY ?!
		IntervalView<FloatType> extendedRoi = Views
				.interval(img2, min, max); 

		final Gradient derivative = new GradientPreCompute(extendedRoi);
		final ArrayList<Spot> spots = Spot.extractSpots(extendedRoi, extendedRoi, simplifiedPeaks, derivative, range);

		Spot.ransac(spots, numIterations, maxError, inlierRatio);
		for (final Spot spot : spots)
			spot.computeAverageCostInliers();

		// TODO: make a 3D output engine
		showRansacResultTable(spots);
	}

	// draw detected points
	// TODO: create a table with the results here
	protected void showRansacResult(final ArrayList<Spot> spots) {
		// make draw global
		for (final FloatType t : Views.iterable(ransacPreview))
			t.setZero();

		// TODO: Automated coloring fails on Mac but works fine on ubuntu
		Spot.drawRANSACArea(spots, ransacPreview);

		// TODO: create a separate function for this part
		double displayMaxError = 0;
		for ( final Spot spot : spots ){
			if ( spot.inliers.size() == 0 )
				continue;
			for ( final PointFunctionMatch pm : spot.inliers )
				if (displayMaxError < pm.getDistance())
					displayMaxError = pm.getDistance();
		}	

		impRansacError.setDisplayRange(0, displayMaxError);
		impRansacError.updateAndDraw();
		drawDetectedSpots(spots, imp); 

		// 		ImageJFunctions.show(ransacPreview).setTitle("2nd fig");

		//		Overlay overlay = drawImp.getOverlay();
		//		if (overlay == null) {
		// System.out.println("If this message pops up probably something
		// went wrong.");
		//			overlay = new Overlay();
		//			drawImp.setOverlay(overlay);
		//		}

		//		overlay.clear();
		// TODO: Figure out if setSlice is necessary at all
		// drawImp.setSlice(imp.getSlice());
		//		drawImp.setRoi(imp.getRoi());
		//		drawDetectedSpots(spots, drawImp);
		// showRansacLog(spots);
		// showRansacResultTable(spots);
	}

	protected void drawDetectedSpots(final ArrayList<Spot> spots, ImagePlus imagePlus) {
		// extract peaks to show
		// we will overlay them with RANSAC result
		Overlay overlay = imagePlus.getOverlay();

		if (overlay == null) {
			System.out.println("If this message pops up probably something went wrong.");
			overlay = new Overlay();
			imagePlus.setOverlay(overlay);
		}

		for (final Spot spot : spots) {
			if (spot.inliers.size() == 0)
				continue;

			final double[] location = new double[slice.numDimensions()];

			spot.center.getSymmetryCenter(location);
			final OvalRoi or = new OvalRoi(location[0] - sigma, location[1] - sigma, Util.round(sigma + sigma2),
					Util.round(sigma + sigma2));

			or.setStrokeColor(Color.ORANGE);
			overlay.add(or);
		}
		imagePlus.updateAndDraw();
	}


	protected void displayAdvancedGenericDialog(){
		// final String title = "Set Stack Parameters";
		// final int width = 260, height = 200; 

		GenericDialog gd = new GenericDialog("Set Stack Parameters");

		gd.addNumericField("Sigma:", this.sigma, 2);
		gd.addNumericField("Threshold:", this.threshold, 5);
		gd.addNumericField("Support Region Radius:", this.supportRadius, 0);
		gd.addNumericField("Inlier Ratio:", this.inlierRatio, 2);
		gd.addNumericField("Max Error:", this.maxError, 2);

		gd.showDialog();
		if (gd.wasCanceled()) return;

		// TODO: if canceled was not clicked perform the processing of the image
		// TODO: Check that the value in the field is not NaN		
		sigma = (float)gd.getNextNumber();
		threshold = (float)gd.getNextNumber();
		supportRadius = (int)Math.round(gd.getNextNumber());
		inlierRatio = (float)gd.getNextNumber();
		maxError = (float)gd.getNextNumber();

		runAdvancedVersion();
	}

	protected void runAdvancedVersion(){
		int numDimensions = slice.numDimensions(); 

		unifiedRunAdvancedVersion();

		//		if (numDimensions == 2){
		//			run2DAdvancedVersion2();
		//		}
		//		else{
		//			if (numDimensions == 3){
		//				run3DAdvancedVersion();
		//			}
		//			else{
		//				System.out.println("Only 2D and 3D images are supported");
		//			}
		//		}		
		if (true) return;
	}

	// TODO: Copy code from here to give some feedback to user


	// unified call for nD cases 
	// FIXME: now only 2D and 3D  
	protected void unifiedRunAdvancedVersion(){

		img2 = ImageJFunctions.wrap(imp);

		long [] min = new long [img2.numDimensions()]; 
		long [] max = new long [img2.numDimensions()]; 

		for (int d = 0; d < img2.numDimensions(); ++d){
			min[d] = img2.min(d);
			max[d] = img2.max(d);
		}

		// full image
		rectangle = new Rectangle((int)min[0], (int)min[1], (int)max[0], (int)max[1]);

		final float k, K_MIN1_INV;
		final float[] sigma, sigmaDiff;

		k = (float) DetectionSegmentation.computeK(sensitivity);
		K_MIN1_INV = DetectionSegmentation.computeKWeight(k);
		sigma = DetectionSegmentation.computeSigma(k, this.sigma);
		sigmaDiff = DetectionSegmentation.computeSigmaDiff(sigma, imageSigma);

		// the upper boundary
		this.sigma2 = sigma[1];

		double [] calibration = new double [img2.numDimensions()];
		for (int d = 0; d < img2.numDimensions(); ++d)
			calibration[d] = 1;

		final DogDetection<FloatType> dog2 = new DogDetection<>(img2, calibration, this.sigma, this.sigma2 , DogDetection.ExtremaType.MINIMA,  thresholdMin / 4, false);
		dog2.setKeepDoGImg(true);
		peaks2 = dog2.getPeaks();
		peaks3 = dog2.getSubpixelPeaks();

		if (img2.numDimensions() == 2 || img2.numDimensions() == 3 )
			//runRansac2();
			runRansacAdvanced23D();
		else
			System.out.println("Wrong dimensionality. Currently supported 2D/3D!");

	}


	// this one is old fashioned you need a generic dialog to solve this task
	// it can macro recorded 
	/**
	 * Instantiates the panel for adjusting the RANSAC parameters
	 */
	protected void displayManualGUI() {
		final Frame frame = new Frame("Set Stack Parameters");
		frame.setSize(260, 200);

		/* Instantiation */
		final GridBagLayout layout = new GridBagLayout();
		final GridBagConstraints c = new GridBagConstraints();

		// TODO: check if these guys are correct
		// maybe it is better to use the constructor for this task
		this.sigma = sigmaInit;
		this.threshold = thresholdInit;
		this.supportRadius = ransacInitSupportRadius;
		this.inlierRatio = ransacInitInlierRatio;
		this.maxError  = ransacInitMaxError;

		// --------- 

		final TextField SigmaTextField = new TextField(String.format(java.util.Locale.US, "%.2f", this.sigma)); 
		SigmaTextField.setEditable(true);
		SigmaTextField.setCaretPosition(String.format(java.util.Locale.US, "%.2f", this.sigma).length());

		final TextField ThresholdTextField = new TextField(String.format(java.util.Locale.US, "%.2f", this.threshold));
		ThresholdTextField.setEditable(true);
		ThresholdTextField.setCaretPosition(String.format(java.util.Locale.US, "%.2f", this.threshold).length());

		final TextField SupportRegionTextField = new TextField(Integer.toString(this.supportRadius));
		SupportRegionTextField.setEditable(true);
		SupportRegionTextField.setCaretPosition(Integer.toString(this.supportRadius).length());

		final TextField InliersTextField = new TextField(String.format(java.util.Locale.US, "%.2f", this.inlierRatio)); 
		InliersTextField.setEditable(true);
		InliersTextField.setCaretPosition(String.format(java.util.Locale.US, "%.2f", this.inlierRatio).length());

		final TextField MaxErrorTextField = new TextField(String.format(java.util.Locale.US, "%.2f", this.maxError)); 
		MaxErrorTextField.setEditable(true);
		MaxErrorTextField.setCaretPosition(String.format(java.util.Locale.US, "%.2f", this.maxError).length());		

		// --------- 
		final Label sigmaText = new Label(
				"Sigma:", Label.CENTER);
		final Label thresholdText = new Label(
				"Threshold: ", Label.CENTER);
		final Label supportRegionText = new Label(
				"Support Region Radius:", Label.CENTER);
		final Label inlierRatioText = new Label(
				"Inlier Ratio:", Label.CENTER);
		final Label maxErrorText = new Label("Max Error:",
				Label.CENTER);

		final Button button = new Button("Apply");
		final Button cancel = new Button("Cancel");

		// /* Location */
		frame.setLayout(layout);

		// insets constants
		int inTop = 0;
		int inRight = 5;
		int inBottom = 0;
		int inLeft = inRight;

		c.fill = GridBagConstraints.HORIZONTAL;

		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 0.50;
		c.gridwidth = 1;
		frame.add(sigmaText, c);

		c.gridx = 1;
		c.weightx = 0.50;
		c.gridwidth = 1;
		c.insets = new Insets(inTop, inLeft, inBottom, inRight);
		frame.add(SigmaTextField, c);		

		c.gridx = 0;
		c.gridy++;
		c.weightx = 0.50;
		c.gridwidth = 1;
		frame.add(thresholdText, c);

		c.gridx = 1;
		c.weightx = 0.50;
		c.gridwidth = 1;
		c.insets = new Insets(inTop, inLeft, inBottom, inRight);
		frame.add(ThresholdTextField, c);	

		c.gridx = 0;
		c.gridy++;
		c.weightx = 0.50;
		c.gridwidth = 1;
		frame.add(supportRegionText, c);

		c.gridx = 1;
		c.weightx = 0.50;
		c.gridwidth = 1;
		c.insets = new Insets(inTop, inLeft, inBottom, inRight);
		frame.add(SupportRegionTextField, c);	

		c.gridx = 0;
		c.gridy++;
		c.weightx = 0.50;
		c.gridwidth = 1;
		frame.add(inlierRatioText, c);

		c.gridx = 1;
		c.weightx = 0.50;
		c.gridwidth = 1;
		c.insets = new Insets(inTop, inLeft, inBottom, inRight);
		frame.add(InliersTextField, c);	

		c.gridx = 0;
		c.gridy++;
		c.weightx = 0.50;
		c.gridwidth = 1;
		frame.add(maxErrorText, c);

		c.gridx = 1;
		c.weightx = 0.50;
		c.gridwidth = 1;
		c.insets = new Insets(inTop, inLeft, inBottom, inRight);
		frame.add(MaxErrorTextField, c);	

		c.gridx = 0;
		c.gridy++;
		c.weightx = 0.50;
		c.gridwidth = 1;
		c.insets = new Insets(5, 50, 0, 50);
		frame.add(button, c);

		c.gridx = 1;
		c.weightx = 0.50;
		c.gridwidth = 1;
		c.insets = new Insets(0, 50, 0, 50);
		frame.add(cancel, c);

		// /* Configuration */
		// TODO: add apply button not the "ok" one
		button.addActionListener(new FinishedButtonListener(frame, false));
		cancel.addActionListener(new FinishedButtonListener(frame, true));

		// TODO: Check if you need this part at all
		frame.addWindowListener(new FrameListener(frame));
		frame.setVisible(true);
	}

	/**
	 * Instantiates the panel for adjusting the RANSAC parameters
	 */
	protected void displayRansacSliders() {
		final Frame frame = new Frame("Adjust RANSAC Values");
		frame.setSize(260, 200);

		/* Instantiation */
		final GridBagLayout layout = new GridBagLayout();
		final GridBagConstraints c = new GridBagConstraints();

		int scrollbarInitialPosition = computeScrollbarPositionFromValue(ransacInitSupportRadius, supportRadiusMin,
				supportRadiusMax, scrollbarSize);
		final Scrollbar supportRegionScrollbar = new Scrollbar(Scrollbar.HORIZONTAL, scrollbarInitialPosition, 10, 0,
				10 + scrollbarSize);
		this.supportRadius = ransacInitSupportRadius;

		final TextField SupportRegionTextField = new TextField(Integer.toString(this.supportRadius));
		SupportRegionTextField.setEditable(true);
		SupportRegionTextField.setCaretPosition(Integer.toString(this.supportRadius).length());

		scrollbarInitialPosition = computeScrollbarPositionFromValue(ransacInitInlierRatio, inlierRatioMin,
				inlierRatioMax, scrollbarSize);
		final Scrollbar inlierRatioScrollbar = new Scrollbar(Scrollbar.HORIZONTAL, scrollbarInitialPosition, 10, 0,
				10 + scrollbarSize);
		this.inlierRatio = ransacInitInlierRatio;

		final float log1001 = (float) Math.log10(scrollbarSize + 1);
		scrollbarInitialPosition = 1001
				- (int) Math.pow(10, (maxErrorMax - ransacInitMaxError) / (maxErrorMax - maxErrorMin) * log1001);

		final Scrollbar maxErrorScrollbar = new Scrollbar(Scrollbar.HORIZONTAL, scrollbarInitialPosition, 10, 0,
				10 + scrollbarSize);
		this.maxError = ransacInitMaxError;

		final Label supportRegionText = new Label(
				"Support Region Radius:" /* = " + this.supportRegion */, Label.CENTER);
		final Label inlierRatioText = new Label(
				"Inlier Ratio = " + String.format(java.util.Locale.US, "%.2f", this.inlierRatio), Label.CENTER);
		final Label maxErrorText = new Label("Max Error = " + String.format(java.util.Locale.US, "%.4f", this.maxError),
				Label.CENTER);

		final Button button = new Button("Done");
		final Button cancel = new Button("Cancel");

		// /* Location */
		frame.setLayout(layout);

		// insets constants
		int inTop = 0;
		int inRight = 5;
		int inBottom = 0;
		int inLeft = inRight;

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 0.50;
		c.gridwidth = 1;
		frame.add(supportRegionText, c);

		c.gridx = 1;
		c.weightx = 0.50;
		c.gridwidth = 1;
		c.insets = new Insets(inTop, inLeft, inBottom, inRight);
		frame.add(SupportRegionTextField, c);

		c.gridx = 0;
		c.gridy = 1;
		c.gridwidth = 2;
		c.insets = new Insets(5, inLeft, inBottom, inRight);
		frame.add(supportRegionScrollbar, c);

		c.gridx = 0;
		c.gridy = 2;
		c.gridwidth = 2;
		frame.add(inlierRatioText, c);

		c.gridx = 0;
		c.gridy = 3;
		c.gridwidth = 2;
		c.insets = new Insets(inTop, inLeft, inBottom, inRight);
		frame.add(inlierRatioScrollbar, c);

		c.gridx = 0;
		c.gridy = 4;
		c.gridwidth = 2;
		frame.add(maxErrorText, c);

		c.gridx = 0;
		c.gridy = 5;
		c.gridwidth = 2;
		c.insets = new Insets(inTop, inLeft, inBottom, inRight);
		frame.add(maxErrorScrollbar, c);

		++c.gridy;
		c.insets = new Insets(5, 50, 0, 50);
		frame.add(button, c);

		++c.gridy;
		c.insets = new Insets(0, 50, 0, 50);
		frame.add(cancel, c);

		// /* Configuration */
		supportRegionScrollbar.addAdjustmentListener(new GeneralListener(supportRegionText, supportRadiusMin,
				supportRadiusMax, ValueChange.SUPPORTREGION, SupportRegionTextField));
		inlierRatioScrollbar.addAdjustmentListener(new GeneralListener(inlierRatioText, inlierRatioMin, inlierRatioMax,
				ValueChange.INLIERRATIO, new TextField()));
		maxErrorScrollbar.addAdjustmentListener(
				new GeneralListener(maxErrorText, maxErrorMin, maxErrorMax, ValueChange.MAXERROR, new TextField()));

		SupportRegionTextField.addActionListener(new TextFieldListener(supportRegionText, supportRadiusMin,
				supportRadiusMax, ValueChange.SUPPORTREGION, SupportRegionTextField, supportRegionScrollbar));

		button.addActionListener(new FinishedButtonListener(frame, false));
		cancel.addActionListener(new FinishedButtonListener(frame, true));

		frame.addWindowListener(new FrameListener(frame));

		frame.setVisible(true);
	}

	/**
	 * Instantiates the panel for adjusting the parameters
	 */
	protected void displaySliders() {
		final Frame frame = new Frame("Adjust Difference-of-Gaussian Values");
		frame.setSize(360, 170);

		/* Instantiation */
		final GridBagLayout layout = new GridBagLayout();
		final GridBagConstraints c = new GridBagConstraints();

		int scrollbarInitialPosition = computeScrollbarPositionFromValue(sigmaInit, sigmaMin, sigmaMax, scrollbarSize);
		final Scrollbar sigma1 = new Scrollbar(Scrollbar.HORIZONTAL, scrollbarInitialPosition, 10, 0,
				10 + scrollbarSize);
		this.sigma = sigmaInit;

		final float log1001 = (float) Math.log10(scrollbarSize + 1);
		scrollbarInitialPosition = (int) Math
				.round(1001 - Math.pow(10, (thresholdMax - thresholdInit) / (thresholdMax - thresholdMin) * log1001));
		final Scrollbar threshold = new Scrollbar(Scrollbar.HORIZONTAL, scrollbarInitialPosition, 10, 0,
				10 + scrollbarSize);
		this.threshold = thresholdInit;

		this.sigma2 = computeSigma2(this.sigma, this.sensitivity);
		// final int sigma2init = computeScrollbarPositionFromValue(this.sigma2,
		// sigmaMin, sigmaMax, scrollbarSize);

		final Label sigmaText1 = new Label("Sigma 1 = " + String.format(java.util.Locale.US, "%.2f", this.sigma),
				Label.CENTER);

		final Label thresholdText = new Label(
				"Threshold = " + String.format(java.util.Locale.US, "%.4f", this.threshold), Label.CENTER);
		final Button button = new Button("Done");
		final Button cancel = new Button("Cancel");

		/* Location */
		frame.setLayout(layout);

		// insets constants
		int inTop = 0;
		int inRight = 5;
		int inBottom = 0;
		int inLeft = inRight;

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		frame.add(sigmaText1, c);

		++c.gridy;
		c.insets = new Insets(inTop, inLeft, inBottom, inRight);
		frame.add(sigma1, c);

		++c.gridy;
		frame.add(thresholdText, c);

		++c.gridy;
		frame.add(threshold, c);

		// insets for buttons
		int bInTop = 0;
		int bInRight = 120;
		int bInBottom = 0;
		int bInLeft = bInRight;

		++c.gridy;
		c.insets = new Insets(bInTop, bInLeft, bInBottom, bInRight);
		frame.add(button, c);

		++c.gridy;
		c.insets = new Insets(bInTop, bInLeft, bInBottom, bInRight);
		frame.add(cancel, c);

		/* Configuration */
		sigma1.addAdjustmentListener(new SigmaListener(sigmaText1, sigmaMin, sigmaMax, scrollbarSize, sigma1));
		threshold.addAdjustmentListener(new ThresholdListener(thresholdText, thresholdMin, thresholdMax));
		button.addActionListener(new FinishedButtonListener(frame, false));
		cancel.addActionListener(new FinishedButtonListener(frame, true));
		frame.addWindowListener(new FrameListener(frame));

		frame.setVisible(true);
	}

	public static float computeSigma2(final float sigma1, final int sensitivity) {
		final float k = (float) DetectionSegmentation.computeK(sensitivity);
		final float[] sigma = DetectionSegmentation.computeSigma(k, sigma1);

		return sigma[1];
	}

	protected boolean isRoiChanged(final ValueChange change, final Rectangle rect, boolean roiChanged){
		boolean res = false;

		res = roiChanged || img2 == null || change == ValueChange.SLICE || rect.getMinX() != rectangle.getMinX()
				|| rect.getMaxX() != rectangle.getMaxX() || rect.getMinY() != rectangle.getMinY()
				|| rect.getMaxY() != rectangle.getMaxY();

		return res;
	}


	/**
	 * Updates the Preview with the current parameters (sigma, threshold, roi,
	 * slice number + RANSAC parameters)
	 * 
	 * @param change
	 *            - what did change
	 */
	protected void updatePreview(final ValueChange change) {

		// TODO: do you realy need to set all these stuff here again?! 
		// check if Roi changed
		boolean roiChanged = false;
		Roi roi = imp.getRoi();

		if (roi == null || roi.getType() != Roi.RECTANGLE) {
			imp.setRoi(new Rectangle(standardRectangle));
			roi = imp.getRoi();
			roiChanged = true;
		}

		// Do I need this one or it is just the copy of the same thing?
		// sourceRectangle or rectangle
		final Rectangle rect = roi.getBounds(); 

		// TODO: fix bad float comparison! use epsilon
		if (isRoiChanged(change, rect, roiChanged)) {
			rectangle = rect;


			long [] min = new long []{rectangle.x - extraSize/2, rectangle.y - extraSize/2};
			long [] max = new long []{rectangle.width + rectangle.x + extraSize/2 - 1, rectangle.height + rectangle.y + extraSize/2 - 1};

			for (int d = 0; d < 2; ++d)
				System.out.println("[" + min[d] + " " + max[d] + "]");


			if (slice.numDimensions() == 3)
				//TODO: might be +1 or -1
				img2 = Views.interval(Views.extendMirrorSingle( Views.hyperSlice(slice, 2, imp.getCurrentSlice())), min, max);
			else{
				if(slice.numDimensions() == 2){
					img2 = Views.interval(Views.extendMirrorSingle(slice), min, max);
					// img2 = slice; //TODO: might be +1 or -1
					// nothing
				}
				else
					System.out.println("updatePreview: This dimensionality is not supported");
			}

			roiChanged = true;
		}

		// if we got some mouse click but the ROI did not change we can return
		if (!roiChanged && change == ValueChange.ROI) {
			isComputing = false;
			return;
		}

		// TODO: Move DoG to the separate file
		// compute the Difference Of Gaussian if necessary
		if (roiChanged || change == ValueChange.SIGMA || change == ValueChange.SLICE
				|| change == ValueChange.ALL || peaks3 == null) {
			runDogDetection();

		}

		// showPeaks();
		showPeaks2();
		imp.updateAndDraw();

		runRansacInteractive();
		isComputing = false;
	}

	protected void runDogDetection(){

		final float k, K_MIN1_INV;
		final float[] sigma, sigmaDiff;

		k = (float) DetectionSegmentation.computeK(sensitivity);
		K_MIN1_INV = DetectionSegmentation.computeKWeight(k);
		sigma = DetectionSegmentation.computeSigma(k, this.sigma);
		sigmaDiff = DetectionSegmentation.computeSigmaDiff(sigma, imageSigma);

		// the upper boundary
		this.sigma2 = sigma[1];

		double [] calibration = new double [img2.numDimensions()];
		for (int d = 0; d < img2.numDimensions(); ++d)
			calibration[d] = 1;

		// TODO: Automatically extended here
		final DogDetection<FloatType> dog2 = new DogDetection<>(img2, calibration, this.sigma, this.sigma2 , DogDetection.ExtremaType.MINIMA,  thresholdMin / 4, false);
		dog2.setKeepDoGImg(true);
		peaks2 = dog2.getPeaks();
		peaks3 = dog2.getSubpixelPeaks();

	}


	// extract peaks to show
	// TODO: Check changes: but should be fine now
	protected void showPeaks2() {
		System.out.println("showPeaks(): imglib2: Done!");

		Overlay o = imp.getOverlay();

		if (o == null) {
			o = new Overlay();
			imp.setOverlay(o);
		}

		o.clear();
		for (final RefinedPeak<Point> peak : peaks3) {

			final float x = peak.getFloatPosition(0);
			final float y = peak.getFloatPosition(1);

			// TODO: This check criteria is totally wrong!!!
			if (isInside(peak) && peak.getValue() > threshold){ // I guess the peak.getValue function returns the value in scale-space

				final OvalRoi or = new OvalRoi(Util.round(x - sigma),
						Util.round(y - sigma), Util.round(sigma + sigma2),
						Util.round(sigma + sigma2));

				or.setStrokeColor(Color.RED);
				o.add(or);
			}
		}

	}


	/**
	 * Tests whether the ROI was changed and will recompute the preview
	 * 
	 * @author Stephan Preibisch
	 */
	protected class RoiListener implements MouseListener {
		@Override
		public void mouseClicked(MouseEvent e) {
		}

		@Override
		public void mouseEntered(MouseEvent e) {
		}

		@Override
		public void mouseExited(MouseEvent e) {
		}

		@Override
		public void mousePressed(MouseEvent e) {
		}

		@Override
		public void mouseReleased(final MouseEvent e) {
			// here the ROI might have been modified, let's test for that
			final Roi roi = imp.getRoi();

			if (roi == null || roi.getType() != Roi.RECTANGLE)
				return;

			while (isComputing)
				SimpleMultiThreading.threadWait(10);

			updatePreview(ValueChange.ROI);
		}

	}

	protected class FinishedButtonListener implements ActionListener {
		final Frame parent;
		final boolean cancel;

		public FinishedButtonListener(Frame parent, final boolean cancel) {
			this.parent = parent;
			this.cancel = cancel;
		}

		@Override
		public void actionPerformed(final ActionEvent arg0) {
			wasCanceled = cancel;
			close(parent, sliceObserver, imp, roiListener);
		}
	}

	protected class FrameListener extends WindowAdapter {
		final Frame parent;

		public FrameListener(Frame parent) {
			super();
			this.parent = parent;
		}

		@Override
		public void windowClosing(WindowEvent e) {
			close(parent, sliceObserver, imp, roiListener);
		}
	}

	protected final void close(final Frame parent, final SliceObserver sliceObserver, final ImagePlus imp,
			final RoiListener roiListener) {
		if (parent != null)
			parent.dispose();

		if (sliceObserver != null)
			sliceObserver.unregister();

		if (imp != null) {
			if (roiListener != null)
				imp.getCanvas().removeMouseListener(roiListener);

			imp.getOverlay().clear();
			imp.updateAndDraw();
		}

		isFinished = true;
	}

	protected class SigmaListener implements AdjustmentListener {
		final Label label;
		final float min, max;
		final int scrollbarSize;

		final Scrollbar sigmaScrollbar1;
		// final Scrollbar sigmaScrollbar2;
		// final Label sigmaText2;

		public SigmaListener(final Label label, final float min, final float max, final int scrollbarSize,
				final Scrollbar sigmaScrollbar1) {
			this.label = label;
			this.min = min;
			this.max = max;
			this.scrollbarSize = scrollbarSize;

			this.sigmaScrollbar1 = sigmaScrollbar1;
			// this.sigmaScrollbar2 = sigmaScrollbar2;
			// this.sigmaText2 = sigmaText2;
		}

		@Override
		public void adjustmentValueChanged(final AdjustmentEvent event) {
			sigma = computeValueFromScrollbarPosition(event.getValue(), min, max, scrollbarSize);

			if (sigma > sigma2) {
				sigma = sigma2 - 0.001f;
				sigmaScrollbar1.setValue(computeScrollbarPositionFromValue(sigma, min, max, scrollbarSize));
			}

			label.setText("Sigma 1 = " + String.format(java.util.Locale.US, "%.2f", sigma));

			// Real time change of the radius
			// if ( !event.getValueIsAdjusting() )
			{
				while (isComputing) {
					SimpleMultiThreading.threadWait(10);
				}
				updatePreview(ValueChange.SIGMA);
			}
		}
	}

	protected static float computeValueFromScrollbarPosition(final int scrollbarPosition, final float min,
			final float max, final int scrollbarSize) {
		return min + (scrollbarPosition / (float) scrollbarSize) * (max - min);
	}

	protected static int computeScrollbarPositionFromValue(final float sigma, final float min, final float max,
			final int scrollbarSize) {
		return Util.round(((sigma - min) / (max - min)) * scrollbarSize);
	}

	protected class ThresholdListener implements AdjustmentListener {
		final Label label;
		final float min, max;
		final float log1001 = (float) Math.log10(1001);

		public ThresholdListener(final Label label, final float min, final float max) {
			this.label = label;
			this.min = min;
			this.max = max;
		}

		@Override
		public void adjustmentValueChanged(final AdjustmentEvent event) {
			threshold = min + ((log1001 - (float) Math.log10(1001 - event.getValue())) / log1001) * (max - min);
			label.setText("Threshold = " + String.format(java.util.Locale.US, "%.4f", threshold));

			if (!isComputing) {
				updatePreview(ValueChange.THRESHOLD);
			} else if (!event.getValueIsAdjusting()) {
				while (isComputing) {
					SimpleMultiThreading.threadWait(10);
				}
				updatePreview(ValueChange.THRESHOLD);
			}
		}
	}

	protected class ImagePlusListener implements SliceListener {
		@Override
		public void sliceChanged(ImagePlus arg0) {
			if (isStarted) {
				// System.out.println("Slice changed!");
				while (isComputing) {
					SimpleMultiThreading.threadWait(10);
				}
				updatePreview(ValueChange.SLICE);
			}
		}
	}

	// changes value of the scroller so that it is the same as in the text field
	protected class TextFieldListener implements ActionListener {
		final Label label;
		final TextField textField;
		final int min, max;
		final ValueChange valueAdjust;
		final Scrollbar scrollbar;

		public TextFieldListener(final Label label, final int min, final int max, ValueChange valueAdjust,
				TextField textField, Scrollbar scrollbar) {
			this.label = label;
			this.min = min;
			this.max = max;
			this.valueAdjust = valueAdjust;
			this.textField = textField;
			this.scrollbar = scrollbar;
		}

		// function checks that the textfield contains number
		// add ensures that the number is inside region [min, max]
		public int ensureNumber(String number, int min, int max) {
			boolean isInteger = Pattern.matches("^\\d*$", number);
			int res = -1;
			// TODO: instead of if/else write full try/catch block
			if (isInteger) {
				res = Integer.parseInt(number);
				if (res > max)
					res = max;
				if (res < min)
					res = min;
			} else {
				System.out.println("Not a valid number. Radius set to 10.");
				res = 10;
				// idle
			}
			return res;
		}

		@Override
		public void actionPerformed(final ActionEvent event) {
			// check that the value is in (min, max)
			// adjust and grab value

			int value = ensureNumber(textField.getText(), min, max);

			// System.out.println("value in the text field = " + value);
			String labelText = "";

			if (valueAdjust == ValueChange.SUPPORTREGION) {
				// set the value for the support region
				supportRadius = value;
				// set label
				labelText = "Support Region Radius:"; // = " + supportRegion;
				// calculate new position of the scrollbar
				int newScrollbarPosition = computeScrollbarPositionFromValue(supportRadius, min, max, scrollbarSize);
				// adjust the scrollbar position!
				scrollbar.setValue(newScrollbarPosition);
				// set new value for text label
				label.setText(labelText);
			} else {
				System.out.println("There is error in the support region adjustment");
			}

			while (isComputing) {
				SimpleMultiThreading.threadWait(10);
			}
			updatePreview(ValueChange.SUPPORTREGION);
		}
	}

	// general listener used by ransac
	protected class GeneralListener implements AdjustmentListener {
		final Label label;
		final TextField textField;
		final float min, max;
		final ValueChange valueAdjust;

		public GeneralListener(final Label label, final float min, final float max, ValueChange valueAdjust,
				TextField textField) {
			this.label = label;
			this.min = min;
			this.max = max;
			this.valueAdjust = valueAdjust;
			this.textField = textField;
		}

		@Override
		public void adjustmentValueChanged(final AdjustmentEvent event) {
			float value = computeValueFromScrollbarPosition(event.getValue(), min, max, scrollbarSize);
			String labelText = "";

			if (valueAdjust == ValueChange.SUPPORTREGION) {
				supportRadius = (int) value;
				labelText = "Support Region Radius:"; // = " + supportRegion ;
				textField.setText(Integer.toString(supportRadius));
			} else if (valueAdjust == ValueChange.INLIERRATIO) {
				inlierRatio = value;
				// this is ugly fix of the problem when inlier's ratio is 1.0
				if (inlierRatio >= 0.999)
					inlierRatio = 0.99999f;
				labelText = "Inlier Ratio = " + String.format(java.util.Locale.US, "%.2f", inlierRatio);
			} else { // MAXERROR
				final float log1001 = (float) Math.log10(1001);
				value = min + ((log1001 - (float) Math.log10(1001 - event.getValue())) / log1001) * (max - min);
				maxError = value;
				labelText = "Max Error = " + String.format(java.util.Locale.US, "%.4f", maxError);
			}
			label.setText(labelText);
			if (!isComputing) {
				updatePreview(valueAdjust);
			} else if (!event.getValueIsAdjusting()) {
				while (isComputing) {
					SimpleMultiThreading.threadWait(10);
				}
				updatePreview(valueAdjust);
			}
		}
	}

	// way the image will be processed
	public static String[] paramChoice = new String[] { "Manual", "Interactive" };
	public static int defaultImg = 0;
	public static int defaultParam = 0;
	public static boolean defaultGauss = false;

	public static void main(String[] args) {
		new ImageJ();

		String pathMac = "/Users/kkolyva/Desktop/latest_desktop/";// "/Users/kkolyva/Downloads/Dies/IMG_1458gi.tif";// 
		String pathUbuntu = "/home/milkyklim/eclipse.input/";

		String path;


		String osName = System.getProperty("os.name").toLowerCase();
		boolean isMacOs = osName.startsWith("mac os x");
		if (isMacOs) 
		{
			path = pathMac; 
		}
		else{
			path = pathUbuntu;
		}

		path = path.concat("multiple_dots.tif");
		System.out.println(path);

		ImagePlus imp = new Opener().openImage(path);

		if (imp == null)
			System.out.println("image was not loaded");

		imp.show();
		// 	imp.setSlice(20);


		// imp.setRoi(imp.getWidth() / 4, imp.getHeight() / 4, imp.getWidth() / 2, imp.getHeight() / 2);

		new InteractiveRadialSymmetry().run(null);

		System.out.println("DOGE!");
	}
}
