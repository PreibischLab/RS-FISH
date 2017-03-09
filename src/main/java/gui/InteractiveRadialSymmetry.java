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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Pattern;

import net.imglib2.Cursor;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
// additional libraries to switch from imglib1 to imglib2
import net.imglib2.algorithm.dog.DogDetection;
import net.imglib2.algorithm.localextrema.RefinedPeak;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.SingularValueDecomposition;

import background.NormalizedGradient;
import background.NormalizedGradientAverage;
import background.NormalizedGradientMedian;
import background.NormalizedGradientRANSAC;
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
import ij.process.FloatProcessor;
import mpicbg.imglib.multithreading.SimpleMultiThreading;
import mpicbg.imglib.util.Util;
import mpicbg.models.PointMatch;
import mpicbg.spim.io.IOFunctions;

public class InteractiveRadialSymmetry implements PlugIn {
	// RANSAC parameters
	// initial values
	final int ransacInitSupportRadius = 5;
	final float ransacInitInlierRatio = 0.75f;
	final float ransacInitMaxError = 3;
	// current value
	int numIterations = 100;
	float maxError = 0.15f;
	float inlierRatio = (float) (20.0 / 100.0);
	int supportRadius = 10;
	// min/max value
	int supportRadiusMin = 1;
	int supportRadiusMax = 25;
	float inlierRatioMin = (float) (0.0 / 100.0); // 0%
	float inlierRatioMax = 1; // 100%
	float maxErrorMin = 0.0001f;
	float maxErrorMax = 10.00f;
	// ----------------------------------------

	// Background Subtraction parameters 
	// initial values 
	final float bsRansacInitInlierRatio = 0.75f;
	final float bsRansacInitMaxError = 3;
	// current values 
	float bsMaxError = 0.15f;
	float bsInlierRatio = (float) (20.0 / 100.0);
	
	int bsNumIterations = 100;
	// min/max value
	float bsInlierRatioMin = (float) (0.0 / 100.0); // 0%
	float bsInlierRatioMax = 1; // 100%
	float bsMaxErrorMin = 0.0001f;
	float bsMaxErrorMax = 10.00f;

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
	// defines the resolution in x y z dimensions
	double [] calibration;
	// --------------------------------

	// Stuff above looks super relevant
	// Only constants 
	// keep all for now 

	// TODO: keep these params
	// int extraSize = ransacInitSupportRadius; // deprecated; supportRadius is used instead
	final int scrollbarSize = 1000;
	float imageSigma = 0.5f;

	// TODO: keep these
	double minIntensityImage = Double.NaN;
	double maxIntensityImage = Double.NaN;

	// TODO: used to choose the image
	// I really want to make them local!
	String imgTitle; 
	String parameterAdjustment;
	boolean gaussFit;

	boolean backgroundSubtraction;
	String [] bsMethods = new String []{"Mean", "Median", "RANSAC"};
	String bsMethod;

	// TODO: after moving to imglib2 REMOVE
	// steps per octave
	public static int standardSensitivity = 4;
	int sensitivity = standardSensitivity;

	// TODO: keep observers
	SliceObserver sliceObserver;
	RoiListener roiListener;

	// TODO: you probably need one image plus object 
	ImagePlus imagePlus;
	int channel = 0;
	Rectangle rectangle;

	ArrayList<RefinedPeak<Point>> peaks;

	// TODO: Variables for imglib1 to imglib2 conversion
	RandomAccessibleInterval<FloatType> slice;
	// TODO: always process only this part of the initial image READ ONLY
	RandomAccessibleInterval<FloatType> extendedRoi;
	// TODO: READ/WRITE  image to proress;
	/*Img*/ RandomAccessibleInterval <FloatType> imgOut;

	FloatProcessor ransacFloatProcessor;
	ImagePlus impRansacError;
	// used to show the results -- error for RANSAC
	RandomAccessibleInterval<FloatType> ransacPreview;

	// TODO: keep listeners flags
	boolean isComputing = false;
	boolean isStarted = false;

	public static enum ValueChange {
		SIGMA, THRESHOLD, SLICE, ROI, ALL, SUPPORTRADIUS, INLIERRATIO, MAXERROR, BSINLIERRATIO, BSMAXERROR
	}

	// TODO: switch from the String[] to the enumerator
	public static enum BsMethods {
		MEAN, MEDIAN, RANSAC
	}
	
	boolean isFinished = false;
	boolean wasCanceled = false;	

	// used to save previous values of the fields
	public static String[] paramChoice = new String[] { "Manual", "Interactive" };
	public static int defaultImg = 0;
	public static int defaultParam = 0;
	public static boolean defaultGauss = false;

	public static boolean defaultBackgroundSubtraction = false;
	public static String defaultMethodBS;

	public boolean isFinished() {
		return isFinished;
	}

	public boolean wasCanceled() {
		return wasCanceled;
	}

	public InteractiveRadialSymmetry(final ImagePlus imp, final int channel) {
		this.imagePlus = imp;
		this.channel = channel;
	}

	public InteractiveRadialSymmetry(final ImagePlus imp) {
		this.imagePlus = imp;
	}

	public InteractiveRadialSymmetry() {
	}

	// TODO: POLISH
	/**
	 * shows the initial GUI dialog where user has to choose 
	 * an image and a processing method.
	 * */
	protected boolean initialDialog(/*String imgTitle, String parameterAdjustment,*/){
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

			// choose image to process and method to use
			GenericDialog initialDialog = new GenericDialog("Initial Setup");

			if (defaultImg >= imgList.length)
				defaultImg = 0;

			initialDialog.addChoice("Image_for_detection", imgList, imgList[defaultImg]);
			initialDialog.addChoice("Define_Parameters", paramChoice, paramChoice[defaultParam]);
			initialDialog.addCheckbox("Do_additional_gauss_fit", defaultGauss);
			initialDialog.addCheckbox("Do_background_subtraction", defaultBackgroundSubtraction);
			initialDialog.showDialog();

			// Save current index and current choice here 
			imgTitle = initialDialog.getNextChoice();
			int tmp = initialDialog.getNextChoiceIndex();
			parameterAdjustment = paramChoice[tmp];
			gaussFit = initialDialog.getNextBoolean();

			// keep previous choice 
			defaultParam = tmp;
			defaultGauss = gaussFit;

			// save the state for the background subtraction
			backgroundSubtraction = initialDialog.getNextBoolean();
			defaultBackgroundSubtraction = backgroundSubtraction;	

			if (initialDialog.wasCanceled())
				failed = true;			
		}

		return failed;
	}

	@Override
	public void run(String arg) {  
		// indicator for the broken workflow
		boolean failed = initialDialog(/*imgTitle, parameterAdjustment, */);
		
		if (!failed && backgroundSubtraction){
			fittingDialog();
		}

		if (failed){
			// nothing 
		}
		else{

			System.out.println("Image used     : " + imgTitle);
			System.out.println("Parameters     : " + parameterAdjustment);
			System.out.println("Background Sub : " + bsMethod);

			if (imagePlus == null)
				imagePlus = WindowManager.getImage(imgTitle);
			// TODO: Check what of the stuff below is necessary
			// // if one of the images is rgb or 8-bit color convert them to
			// hyperstack
			// imp = Hyperstack_rearranger.convertToHyperStack( imp );
			//
			// // test if you can deal with this image (2d? 3d? channels?
			// timepoints?)
			// 3d + time should be fine.
			// right now works with 2d + time

			// TODO move this return
			if (imagePlus.getType() == ImagePlus.COLOR_RGB || imagePlus.getType() == ImagePlus.COLOR_256) {
				IJ.log("Color images are not supported, please convert to 8, 16 or 32-bit grayscale");
				failed = true;
			}
			else{
				// if interactive
				if (parameterAdjustment.compareTo(paramChoice[1]) == 0) {
					// here comes the normal work flow
					// initial rectangle
					rectangle = new Rectangle(imagePlus.getWidth() / 4, imagePlus.getHeight() / 4, imagePlus.getWidth() / 2,
							imagePlus.getHeight() / 2);
					// TODO: Do I need this ROI?
					Roi roi = imagePlus.getRoi();
					if (roi == null) {
						// IJ.log( "A rectangular ROI is required to define the area..." );
						imagePlus.setRoi(rectangle);
						roi = imagePlus.getRoi();
					}
					
					if (roi.getType() != Roi.RECTANGLE) {
						IJ.log("Only rectangular rois are supported...");
						failed = true;
					}
					else{
						imagePlus.setPosition(channel, imagePlus.getSlice(), 1);
						slice = ImageJFunctions.convertFloat(imagePlus);	
						// should be called after slice inititalization
						calibration = HelperFunctions.setCalibration(imagePlus, slice.numDimensions());
						// initialize variables for interactive preview
						// called before updatePreview() !
						ransacPreviewInit();
						// show the interactive kit
						interactiveDialog();
						if (backgroundSubtraction && bsMethod.equals("RANSAC") ){ // if RANSAC
							interactiveRansacBSDialog();
						}

						// show the interactive ransac kit
						interactiveRansacDialog();
						// add listener to the imageplus slice slider
						sliceObserver = new SliceObserver(imagePlus, new ImagePlusListener());
						// compute first version
						updatePreview(ValueChange.ALL);
						isStarted = true;
						// check whenever roi is modified to update accordingly
						roiListener = new RoiListener(imagePlus, impRansacError);
						imagePlus.getCanvas().addMouseListener(roiListener);
					}
				} 
				else // automatic 
				{
					// TODO: Do I need the rectangle here?
					// here comes the normal work flow
					rectangle = new Rectangle(0, 0, imagePlus.getWidth(), imagePlus.getHeight());
					imagePlus.setRoi(rectangle);
					// img2 = ImageJFunctions.wrapFloat(imp);
					imagePlus.setPosition(channel, imagePlus.getSlice(), 0);
					slice = ImageJFunctions.convertFloat(imagePlus);
					// should be called after slice inititalization
					calibration = HelperFunctions.setCalibration(imagePlus, slice.numDimensions());
					// initialize variables for the result
					ransacPreviewInit();
					automaticDialog();
				}
			}

		}
		// return failed; // uncomment if necessary 
	}


	/**
	 * Initialize preview variables for RANSAC
	 */
	// TODO: might be not necessary
	protected void ransacPreviewInit() {		
		int width = (int)slice.dimension(0);
		int height = (int)slice.dimension(1);		

		ransacFloatProcessor = new FloatProcessor(width, height);

		float[] pixels = (float[]) ransacFloatProcessor.getPixels();
		impRansacError = new ImagePlus("RANSAC preview", ransacFloatProcessor);
		ransacPreview = ArrayImgs.floats(pixels, width, height);
		impRansacError.show();

		// set same roi for rahsac error image
		Roi roi = imagePlus.getRoi();
		if (roi != null) {
			impRansacError.setRoi(roi);
		}

	}

	// this function will show the result of RANSAC
	// proper window -> dialog view with the columns
	protected void ransacResultTable(final ArrayList<Spot> spots) {
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


	/**
	 * Shows dialog to choose method for background subtraction
	 * */
	protected void fittingDialog(){
		boolean canceled = false;

		GenericDialog gd = new GenericDialog("Background Subtraction Method");
		int numColumns = 1;
		gd.addRadioButtonGroup("Method :", bsMethods, bsMethods.length, numColumns, defaultMethodBS);

		gd.showDialog();

		// Should I move this to the return statement
		bsMethod = gd.getNextRadioButton();
		defaultMethodBS = bsMethod;

		if (gd.wasCanceled()) 
			canceled = true;

		if (canceled)
			return;
	}

	/**
	 * shows dialog to adjust parameters for the RANSAC background subtraction
	 * */
	protected void interactiveRansacBSDialog() {
		final Frame frame = new Frame("Adjust RANSAC Background Subtraction Values");
		frame.setSize(260, 200);

		/* Instantiation */
		final GridBagLayout layout = new GridBagLayout();
		final GridBagConstraints gbc = new GridBagConstraints();

		int scrollbarBSInitialPosition = computeScrollbarPositionFromValue(bsRansacInitInlierRatio, bsInlierRatioMin,
				bsInlierRatioMax, scrollbarSize);
		
		final Scrollbar bsInlierRatioScrollbar = new Scrollbar(Scrollbar.HORIZONTAL, scrollbarBSInitialPosition, 10, 0,
				10 + scrollbarSize);
		this.bsInlierRatio = bsRansacInitInlierRatio;

		final float log1001 = (float) Math.log10(scrollbarSize + 1);
		scrollbarBSInitialPosition = 1001
				- (int) Math.pow(10, (bsMaxErrorMax - bsRansacInitMaxError) / (bsMaxErrorMax - bsMaxErrorMin) * log1001);

		final Scrollbar maxErrorScrollbar = new Scrollbar(Scrollbar.HORIZONTAL, scrollbarBSInitialPosition, 10, 0,
				10 + scrollbarSize);
		this.bsMaxError = bsRansacInitMaxError;

		final Label bsInlierRatioText = new Label(
				"Inlier Ratio = " + String.format(java.util.Locale.US, "%.2f", this.bsInlierRatio), Label.CENTER);
		final Label bsMaxErrorText = new Label("Max Error = " + String.format(java.util.Locale.US, "%.4f", this.bsMaxError),
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

		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1;
		frame.add(bsInlierRatioText, gbc);

		++gbc.gridy;
		gbc.insets = new Insets(inTop, inLeft, inBottom, inRight);
		frame.add(bsInlierRatioScrollbar, gbc);
		
		++gbc.gridy;
		frame.add(bsMaxErrorText, gbc);

		++gbc.gridy;
		gbc.insets = new Insets(inTop, inLeft, inBottom, inRight);
		frame.add(maxErrorScrollbar, gbc);

		++gbc.gridy;
		gbc.insets = new Insets(5, 50, 0, 50);
		frame.add(button, gbc);

		++gbc.gridy;
		gbc.insets = new Insets(0, 50, 0, 50);
		frame.add(cancel, gbc);

		// /* Configuration */
		bsInlierRatioScrollbar.addAdjustmentListener(new GeneralListener(bsInlierRatioText, bsInlierRatioMin, bsInlierRatioMax,
				ValueChange.BSINLIERRATIO, new TextField()));
		maxErrorScrollbar.addAdjustmentListener(
				new GeneralListener(bsMaxErrorText, bsMaxErrorMin, bsMaxErrorMax, ValueChange.BSMAXERROR, new TextField()));

		button.addActionListener(new FinishedButtonListener(frame, false));
		cancel.addActionListener(new FinishedButtonListener(frame, true));

		frame.addWindowListener(new FrameListener(frame));

		frame.setVisible(true);
	}

	protected void ransacInteractive() {
		// make sure the size is not 0 (is possible in ImageJ when making the Rectangle, not when changing it ... yeah)
		rectangle.width = Math.max( 1, rectangle.width );
		rectangle.height = Math.max( 1, rectangle.height );

		final ArrayList<long[]> simplifiedPeaks = new ArrayList<>(1);
		int numDimensions = extendedRoi.numDimensions(); // DEBUG: should always be 2 
		// extract peaks for the roi
		HelperFunctions.copyPeaks(peaks, simplifiedPeaks, numDimensions, rectangle);

		// the size of the RANSAC area
		final long[] range = new long[numDimensions];

		range[ 0 ] = range[ 1 ] = 2*supportRadius;

		final Gradient derivative = new GradientPreCompute( extendedRoi );		
		NormalizedGradient ng = null;
		// depending on user choice we use different method
		if (backgroundSubtraction)
			ng = chooseNormalizeGradientMethod(bsMethods, bsMethod, derivative);
				
		// ImageJFunctions.show(new NormalizedGradientRANSAC(derivative).);

		// ImageJFunctions.show(new GradientPreCompute(extendedRoi).preCompute(extendedRoi));			
		final ArrayList<Spot> spots = Spot.extractSpots(extendedRoi, simplifiedPeaks, derivative, ng, range);

		// TODO: where this part should be applied
		// applyBackgroundSubtraction(spots, imgOut);

		Spot.ransac(spots, numIterations, maxError, inlierRatio);
		for (final Spot spot : spots)
			spot.computeAverageCostInliers();
		ransacResults(spots);
	}
	
	// mean, median, ransac
	protected NormalizedGradient chooseNormalizeGradientMethod(String[] methods, String method, Gradient derivative){
		NormalizedGradient ng = null;
		if (method.equals(methods[0])){ // if mean
			ng = new NormalizedGradientAverage( derivative );
		} else if (method.equals(methods[1])){ // median
			ng = new NormalizedGradientMedian( derivative );
		} else if (method.equals(methods[2])){
			// TODO: Add BETTER passing the arguments here
			ng = new NormalizedGradientRANSAC(derivative, bsMaxError, bsInlierRatio);
		} else {
			System.out.println("Wrong background subtraction method. Setting ng to null");
		}
		
		return ng;
	}
	
	protected void applyBackgroundSubtraction(ArrayList<long[]> peaksLocal, RandomAccessibleInterval <FloatType> image, long [] fullImgMax){
		int numDimensions = image.numDimensions();

		for(int j = 0; j < peaksLocal.size(); ++j){
			double [] coefficients = new double [numDimensions + 1]; // z y x 1
			double [] position = new double [numDimensions]; // x y z
			long [] spotMin = new long [numDimensions];
			long [] spotMax = new long [numDimensions]; 

			backgroundSubtractionCorners(peaksLocal.get(j), image, coefficients, spotMin, spotMax, fullImgMax);

			Cursor <FloatType> cursor = Views.interval(image, spotMin, spotMax).localizingCursor();

			while(cursor.hasNext()){
				cursor.fwd();
				cursor.localize(position);				
				double total = coefficients[numDimensions];	
				for (int d = 0; d < numDimensions; ++d){
					total += coefficients[d]*position[numDimensions - d - 1]; 
				}

				// DEBUG: 
				if (j == 0){
					System.out.println("before: " + cursor.get().get());
				}

				// TODO: looks like modifying the initial image is a bad idea 
				cursor.get().set(cursor.get().get() - (float)total);

				// DEBUG:
				if (j == 0){
					System.out.println("after:  " + cursor.get().get());
				}

			}		
		}

		// ImageJFunctions.show(source).setTitle("This one is actually modified with background subtraction");
	}

	protected void backgroundSubtractionCorners(long [] peak, RandomAccessibleInterval<FloatType> img22, double[] coefficients, long[] min, long[] max, long [] fullImgMax){	
		HelperFunctions.getBoundaries(peak, min, max, fullImgMax, supportRadius);

		int numDimensions = peak.length;

		Cursor<FloatType> cursor = Views.interval(img22, min, max).localizingCursor();
		long numPoints = 0;
		// this is some kind of magical number calculation 
		// there is a chance to make it nD but it involves some math 	
		if (numDimensions == 2){	
			// add boundaries (+1 comes from the spot implementation)
			for (int j = 0; j < numDimensions; ++j){
				numPoints += (max[j] - min[j] - 2 + 1)*(1 << (numDimensions - 1));
			}
			// add corners
			numPoints += (1 << numDimensions); //Math.pow(2, numDimensions);
		}
		else{
			if (numDimensions == 3){
				for(int j =0; j < numDimensions; j ++){
					for(int i = 0; i < numDimensions; i++){
						if (i != j){
							// add facets
							numPoints += (max[j] - min[j] - 2 + 1)*(max[i] - min[i] - 2 + 1)*(1 << (numDimensions - 2)); 
						}
					}
					// add edges
					numPoints += (max[j] - min[j] - 2 + 1)*(1 << (numDimensions - 1));
				}			
				// add corners
				numPoints += (1 << numDimensions); //Math.pow(2, numDimensions);
			}
			else
				System.out.println("numDimensions should be 2 or 3, higher dimensionality is not supported.");
		}

		double [][] A = new double[(int)numPoints][numDimensions + 1];
		double [] b = new double[(int)numPoints];

		int rowCount = 0;		
		while(cursor.hasNext()){
			cursor.fwd();
			double[] pos = new double[numDimensions];
			cursor.localize(pos);
			// check that this is the boundary pixel
			boolean boundary = false;
			for (int d =0; d < numDimensions;++d ){
				if ((long)pos[d] == min[d] || (long)pos[d] == max[d]){
					boundary = true;
					break;
				}
			}
			// process only boundary pixels for plane fitting
			if (boundary){				
				for (int d = 0; d < numDimensions; ++d){			
					A[rowCount][numDimensions - d - 1] = pos[d];
				}
				A[rowCount][numDimensions] = 1;
				// check this one
				b[rowCount] = cursor.get().get();

				rowCount++;
			}
		}

		RealMatrix mA = new Array2DRowRealMatrix(A, false);
		RealVector mb = new ArrayRealVector(b, false);
		DecompositionSolver solver = new SingularValueDecomposition(mA).getSolver();
		RealVector mX =  solver.solve(mb);

		// FIXME: This part is done outside of the function for now
		// subtract the values this part 
		// return the result
		// TODO: why proper copying is not working here ?! 
		for (int i  = 0; i < coefficients.length; i++)
			coefficients[i] = mX.toArray()[i];
	}

	// TODO: at this point only uses corner values
	// TODO: extend to using the boundary values too
	// TODO: BACK UP / OUT-DATED
	protected void backgroundSubtraction(Spot spot, RandomAccessibleInterval<FloatType> img22, double[] coefficients, long[] min, long[] max){
		int numDimensions = spot.numDimensions();

		// define the boundaries for the of the current spot 
		for (int d = 0; d < numDimensions; ++d){
			min[d] = Long.MAX_VALUE;
			max[d] = Long.MIN_VALUE;
		}

		for (PointMatch pm : spot.candidates){
			double [] coordinates = pm.getP1().getL();
			// TODO: hope it is safe to convert double to long here 
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
		RandomAccess<FloatType> ra = img22.randomAccess();
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

	protected void ransacAutomatic(){
		final ArrayList<long[]> simplifiedPeaks = new ArrayList<>(1);
		
		int numDimensions = extendedRoi.numDimensions();
		HelperFunctions.copyPeaks(peaks, simplifiedPeaks, numDimensions, rectangle);
		
		final long[] range = new long[numDimensions];
		for (int d = 0; d <numDimensions; ++d)
			range[d] = 2*supportRadius;

		final Gradient derivative = new GradientPreCompute(extendedRoi);
		
		NormalizedGradient ng = null;
		// depending on user choice we use different method
		if (backgroundSubtraction)
			ng = chooseNormalizeGradientMethod(bsMethods, bsMethod, derivative);
		
		final ArrayList<Spot> spots = Spot.extractSpots(extendedRoi, simplifiedPeaks, derivative, ng, range);

		Spot.ransac(spots, numIterations, maxError, inlierRatio);
		for (final Spot spot : spots)
			spot.computeAverageCostInliers();

		ransacResultTable(spots);
	}

	// APPROVED:
	/**
	 * shows the results (circles) of the detection. 
	 * */
	protected void ransacResults(final ArrayList<Spot> spots) {
		// reset the image
		for (final FloatType t : Views.iterable(ransacPreview))
			t.setZero();

		Spot.drawRANSACArea(spots, ransacPreview);

		// TODO: create a separate function for this part
		double displayMaxError = 0;		
		for ( final Spot spot : spots ){
			if ( spot.inliers.size() == 0 )
				continue;
			for ( final PointFunctionMatch pm : spot.inliers ){
				if (displayMaxError < pm.getDistance())
					displayMaxError = pm.getDistance();
			}
		}

		// (displayMaxError/4) instead of displayMaxError to have better contrast
		impRansacError.setDisplayRange(0, displayMaxError/4);
		impRansacError.updateAndDraw();

		// show circles in the RANSAC image 
		Overlay ransacErrorOverlay = impRansacError.getOverlay();
		if (ransacErrorOverlay != null)
			ransacErrorOverlay.clear();
		drawDetectedSpots(spots, impRansacError); 

		// show circles in the initial image
		drawDetectedSpots(spots, imagePlus);
	}

	/**
	 * adds new spots to the overlay. IMPORTANT: the overlay is not overwritten since 
	 * it might be useful for initial Difference-of-Gaussians detection
	 * */
	protected void drawDetectedSpots(final ArrayList<Spot> spots, ImagePlus imagePlus) {
		// extract peaks to show
		// we will overlay them with RANSAC result
		Overlay overlay = imagePlus.getOverlay();

		if (overlay == null) {
			// System.out.println("If this message pops up probably something went wrong.");
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

	/**
	 * shows 2 dialogs!
	 * */
	protected void automaticDialog(){
		// TODO: add caching for variables
		boolean canceled = false;

		GenericDialog gd = new GenericDialog("Set Stack Parameters");

		gd.addNumericField("Sigma:", this.sigmaInit, 2);
		gd.addNumericField("Threshold:", this.thresholdInit, 4);
		gd.addNumericField("Support_Region_Radius:", this.ransacInitSupportRadius, 0);
		gd.addNumericField("Inlier_Ratio:", this.ransacInitInlierRatio, 2);
		gd.addNumericField("Max_Error:", this.ransacInitMaxError, 2);

		gd.showDialog();
		if (gd.wasCanceled()) 
			canceled = true;

		sigma = (float)gd.getNextNumber();
		threshold = (float)gd.getNextNumber();
		supportRadius = (int)Math.round(gd.getNextNumber());
		inlierRatio = (float)gd.getNextNumber();
		maxError = (float)gd.getNextNumber();	
		
		// wrong values in the fields
		if (sigma == Double.NaN || threshold == Double.NaN ||  supportRadius == Double.NaN || inlierRatio == Double.NaN || maxError == Double.NaN )
			canceled = true;
		
		if (canceled)
			return;
		
		runRansacAutomatic();
	}

	// unified call for nD cases 
	protected void runRansacAutomatic(){
		extendedRoi = ImageJFunctions.wrap(imagePlus); // returns the whole image either 2D or 3D

		// long [] min = new long [extendedRoi.numDimensions()]; 
		// long [] max = new long [extendedRoi.numDimensions()]; 

		// for (int d = 0; d < extendedRoi.numDimensions(); ++d){
		// 	min[d] = extendedRoi.min(d);
		// 	max[d] = extendedRoi.max(d);
		// }

		this.sigma2 = computeSigma2(this.sigma, sensitivity);
		// IMP: in the 3D case the blobs will have lower contrast as a function of sigma(z) therefore we have to adjust the threshold;
		// to fix the problem we use an extra factor =0.5 which will decrease the threshold value; this might help in some cases but z-extrasmoothing
		// is image depended

		final float tFactor = extendedRoi.numDimensions() == 3 ? 0.5f : 1.0f;	
		final DogDetection<FloatType> dog2 = new DogDetection<>(extendedRoi, calibration, this.sigma, this.sigma2 , DogDetection.ExtremaType.MINIMA,  tFactor*threshold / 4, false);
		peaks = dog2.getSubpixelPeaks();

		if (extendedRoi.numDimensions() == 2 || extendedRoi.numDimensions() == 3 )
			ransacAutomatic();
		else
			System.out.println("Wrong dimensionality. Currently supported 2D/3D!");

	}

	// APPROVED:
	/**
	 * Instantiates the panel for adjusting the RANSAC parameters
	 */
	protected void interactiveRansacDialog() {
		final Frame frame = new Frame("Adjust RANSAC Values");
		frame.setSize(260, 200);

		/* Instantiation */
		final GridBagLayout layout = new GridBagLayout();
		final GridBagConstraints gbc= new GridBagConstraints();

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

		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 0.50;
		gbc.gridwidth = 1;
		frame.add(supportRegionText, gbc);

		gbc.gridx = 1;
		gbc.weightx = 0.50;
		gbc.gridwidth = 1;
		gbc.insets = new Insets(inTop, inLeft, inBottom, inRight);
		frame.add(SupportRegionTextField, gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridwidth = 2;
		gbc.insets = new Insets(5, inLeft, inBottom, inRight);
		frame.add(supportRegionScrollbar, gbc);

		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.gridwidth = 2;
		frame.add(inlierRatioText, gbc);

		gbc.gridx = 0;
		gbc.gridy = 3;
		gbc.gridwidth = 2;
		gbc.insets = new Insets(inTop, inLeft, inBottom, inRight);
		frame.add(inlierRatioScrollbar, gbc);

		gbc.gridx = 0;
		gbc.gridy = 4;
		gbc.gridwidth = 2;
		frame.add(maxErrorText, gbc);

		gbc.gridx = 0;
		gbc.gridy = 5;
		gbc.gridwidth = 2;
		gbc.insets = new Insets(inTop, inLeft, inBottom, inRight);
		frame.add(maxErrorScrollbar, gbc);

		++gbc.gridy;
		gbc.insets = new Insets(5, 50, 0, 50);
		frame.add(button, gbc);

		++gbc.gridy;
		gbc.insets = new Insets(0, 50, 0, 50);
		frame.add(cancel, gbc);

		// /* Configuration */
		supportRegionScrollbar.addAdjustmentListener(new GeneralListener(supportRegionText, supportRadiusMin,
				supportRadiusMax, ValueChange.SUPPORTRADIUS, SupportRegionTextField));
		inlierRatioScrollbar.addAdjustmentListener(new GeneralListener(inlierRatioText, inlierRatioMin, inlierRatioMax,
				ValueChange.INLIERRATIO, new TextField()));
		maxErrorScrollbar.addAdjustmentListener(
				new GeneralListener(maxErrorText, maxErrorMin, maxErrorMax, ValueChange.MAXERROR, new TextField()));

		SupportRegionTextField.addActionListener(new TextFieldListener(supportRegionText, supportRadiusMin,
				supportRadiusMax, ValueChange.SUPPORTRADIUS, SupportRegionTextField, supportRegionScrollbar));

		button.addActionListener(new FinishedButtonListener(frame, false));
		cancel.addActionListener(new FinishedButtonListener(frame, true));

		frame.addWindowListener(new FrameListener(frame));

		frame.setVisible(true);
	}

	// APPROVED: delete comments
	/**
	 * Instantiates the panel for adjusting the parameters
	 */
	protected void interactiveDialog() {
		final Frame frame = new Frame("Adjust Difference-of-Gaussian Values");
		frame.setSize(360, 170);

		/* Instantiation */
		final GridBagLayout layout = new GridBagLayout();
		final GridBagConstraints c = new GridBagConstraints();

		int scrollbarInitialPosition = computeScrollbarPositionFromValue(sigmaInit, sigmaMin, sigmaMax, scrollbarSize);
		final Scrollbar sigma1Bar = new Scrollbar(Scrollbar.HORIZONTAL, scrollbarInitialPosition, 10, 0,
				10 + scrollbarSize);
		this.sigma = sigmaInit;

		final float log1001 = (float) Math.log10(scrollbarSize + 1);
		scrollbarInitialPosition = (int) Math
				.round(1001 - Math.pow(10, (thresholdMax - thresholdInit) / (thresholdMax - thresholdMin) * log1001));
		final Scrollbar thresholdBar = new Scrollbar(Scrollbar.HORIZONTAL, scrollbarInitialPosition, 10, 0,
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
		frame.add(sigma1Bar, c);

		++c.gridy;
		frame.add(thresholdText, c);

		++c.gridy;
		frame.add(thresholdBar, c);

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
		sigma1Bar.addAdjustmentListener(new SigmaListener(sigmaText1, sigmaMin, sigmaMax, scrollbarSize, sigma1Bar));
		thresholdBar.addAdjustmentListener(new ThresholdListener(thresholdText, thresholdMin, thresholdMax));
		button.addActionListener(new FinishedButtonListener(frame, false));
		cancel.addActionListener(new FinishedButtonListener(frame, true));
		frame.addWindowListener(new FrameListener(frame));

		frame.setVisible(true);
	}

	// APPROVED: 
	public float computeSigma2(final float sigma1, final int stepsPerOctave) {
		final float k = (float) Math.pow( 2f, 1f / stepsPerOctave );
		return sigma1 * k;
	}

	// TODO: fix the check: "==" must not be used with floats
	// APPROVED:
	protected boolean isRoiChanged(final ValueChange change, final Rectangle rect, boolean roiChanged){
		boolean res = false;
		res = (roiChanged || extendedRoi == null || change == ValueChange.SLICE ||rect.getMinX() != rectangle.getMinX()
				|| rect.getMaxX() != rectangle.getMaxX() || rect.getMinY() != rectangle.getMinY()
				|| rect.getMaxY() != rectangle.getMaxY());
		return res;
	}

	/**
	 * copy data from one image to another 
	 * */
	protected void createOuputImg(RandomAccessibleInterval<FloatType> iImg, RandomAccessibleInterval<FloatType> oImg){
		Cursor<FloatType> cursor = Views.iterable(iImg).localizingCursor();
		RandomAccess<FloatType> ra = oImg.randomAccess(); //Views.translate(oImg, new long[]{rectangle.x - supportRadius, rectangle.y - supportRadius}).randomAccess();

		long [] pos = new long [iImg.numDimensions()];
		while(cursor.hasNext()){
			cursor.fwd();
			cursor.localize(pos);
			ra.setPosition(pos);
			ra.get().set(cursor.get().get());	
		}
	}


	public static int[] long2int(long [] a){
		int [] res = new int [a.length];
		for (int k = 0; k < a.length; k++)
			res[k] = (int)a[k];
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
		// set up roi 
		boolean roiChanged = false;
		Roi roi = imagePlus.getRoi();

		if (roi == null || roi.getType() != Roi.RECTANGLE) {
			// Rectangle dummyRectangle = new Rectangle(imp.getWidth() / 4, imp.getHeight() / 4, imp.getWidth() / 2,
			//		imp.getHeight() / 2);
			imagePlus.setRoi(rectangle);
			impRansacError.setRoi(rectangle);
			roi = imagePlus.getRoi();
			roiChanged = true;
		}

		// Do I need this one or it is just the copy of the same thing?
		// sourceRectangle or rectangle
		final Rectangle roiBounds = roi.getBounds(); 

		// change the img2 size if the roi or the support radius size was changed
		if (isRoiChanged(change, roiBounds, roiChanged) || change == ValueChange.SUPPORTRADIUS) {
			rectangle = roiBounds;

			// make sure the size is not 0 (is possible in ImageJ when making the Rectangle, not when changing it ... yeah)
			rectangle.width = Math.max( 1, rectangle.width );
			rectangle.height = Math.max( 1, rectangle.height );

			// one direction solution 
			impRansacError.setRoi(rectangle);

			long [] min = new long []{rectangle.x - supportRadius, rectangle.y - supportRadius};
			long [] max = new long []{rectangle.width + rectangle.x + supportRadius - 1, rectangle.height + rectangle.y + supportRadius - 1};

			if (slice.numDimensions() == 3){
				extendedRoi = Views.interval(Views.extendMirrorSingle( Views.hyperSlice(slice, 2, imagePlus.getCurrentSlice())), min, max);
			}
			else{
				if(slice.numDimensions() == 2){
					extendedRoi = Views.interval(Views.extendMirrorSingle(slice), min, max);
				}
				else
					System.out.println("updatePreview: This dimensionality is not supported");
			}
			roiChanged = true;
		}

		// compute the Difference Of Gaussian if necessary
		if (roiChanged || peaks == null || change == ValueChange.SIGMA || change == ValueChange.SLICE
				|| change == ValueChange.ALL || change == ValueChange.SUPPORTRADIUS || change == ValueChange.THRESHOLD || change == ValueChange.MAXERROR 
				|| change == ValueChange.INLIERRATIO) {

			// refill output image in case anything was changed
			long [] dimensions = new long [extendedRoi.numDimensions()];	
			extendedRoi.dimensions(dimensions);
			
			dogDetection(extendedRoi); 
		}

		showPeaks(imagePlus, rectangle);
		imagePlus.updateAndDraw();

		ransacInteractive();
		isComputing = false;
	}

	// APPROVED:
	/**
	 * this function is used for Difference-of-Gaussian calculation in the 
	 * interactive case. No calibration adjustment is needed.
	 * (threshold/4) - because some peaks might be skipped.
	 * */
	protected void dogDetection(RandomAccessibleInterval <FloatType> image){
		final DogDetection<FloatType> dog2 = new DogDetection<>(image, calibration, this.sigma, this.sigma2 , DogDetection.ExtremaType.MINIMA,  threshold/4, false);
		peaks = dog2.getSubpixelPeaks(); 
	}

	// extract peaks to show
	// TODO: Check changes: but should be fine now
	protected void showPeaks(ImagePlus imp, Rectangle rectangle) {
		Overlay o = imp.getOverlay();

		if (o == null) {
			o = new Overlay();
			imp.setOverlay(o);
		}

		o.clear();
		for (final RefinedPeak<Point> peak : peaks) {

			final float x = peak.getFloatPosition(0);
			final float y = peak.getFloatPosition(1);

			// TODO: This check criteria is totally wrong!!!
			if (HelperFunctions.isInside(peak, rectangle) && peak.getValue() > threshold){ // I guess the peak.getValue function returns the value in scale-space

				final OvalRoi or = new OvalRoi(Util.round(x - sigma),
						Util.round(y - sigma), Util.round(sigma + sigma2),
						Util.round(sigma + sigma2));

				or.setStrokeColor(Color.RED);
				o.add(or);
			}
		}
	}

	// TODO: REMOVE THIS? 
	// item listener to choose the type of the method to use
	protected class CheckboxListener implements ItemListener {
		final String[] items;
		final String item;

		public CheckboxListener(final String[] items, final String item){
			this.items = items;
			this.item = item;
		}

		@Override
		public void itemStateChanged(final ItemEvent event) {
			// the last item in the list is the one with extra parameters
			if (item.equals(items[items.length - 1])){

			}

		}
	}


	// APPROVED:

	/**
	 * Tests whether the ROI was changed and will recompute the preview
	 * 
	 * @author Stephan Preibisch
	 */
	protected class RoiListener implements MouseListener {

		final ImagePlus source, target;

		public RoiListener(final ImagePlus s, final ImagePlus t){
			this.source = s;
			this.target = t;
		}

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
			final Roi roi = source.getRoi();

			// roi is wrong, clear the screen 
			if (roi == null || roi.getType() != Roi.RECTANGLE){
				if (source.getOverlay() != null){
					source.getOverlay().clear();
				}
				if (target.getRoi() != null)
					target.deleteRoi();
				if (target.getOverlay() != null){
					target.getOverlay().clear();
				}				

				// target.updateAndDraw();
				// return;
			}
			else{
				// TODO: might put the update part for the roi here instead of the updatePreview
				while (isComputing)
					SimpleMultiThreading.threadWait(10);

				updatePreview(ValueChange.ROI);
			}

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
			close(parent, sliceObserver, imagePlus, roiListener);
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
			close(parent, sliceObserver, imagePlus, roiListener);
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
			sigma2 = computeSigma2(sigma, sensitivity);

			// TODO: this might never be the case
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

			if (valueAdjust == ValueChange.SUPPORTRADIUS) {
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
			updatePreview(ValueChange.SUPPORTRADIUS);
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
			if (valueAdjust == ValueChange.SUPPORTRADIUS) {
				supportRadius = (int) value;
				labelText = "Support Region Radius:"; // = " + supportRegion ;
				textField.setText(Integer.toString(supportRadius));
			} else if (valueAdjust == ValueChange.INLIERRATIO) {
				inlierRatio = value;
				// this is ugly fix of the problem when inlier's ratio is 1.0
				if (inlierRatio >= 0.999)
					inlierRatio = 0.99999f;
				labelText = "Inlier Ratio = " + String.format(java.util.Locale.US, "%.2f", inlierRatio);
			} else if (valueAdjust == ValueChange.MAXERROR) { // MAXERROR
				final float log1001 = (float) Math.log10(1001);
				value = min + ((log1001 - (float) Math.log10(1001 - event.getValue())) / log1001) * (max - min);
				maxError = value;
				labelText = "Max Error = " + String.format(java.util.Locale.US, "%.4f", maxError);
			} else if (valueAdjust == ValueChange.BSMAXERROR) { // BACKGROUND MAXERROR
				final float log1001 = (float) Math.log10(1001);
				value = min + ((log1001 - (float) Math.log10(1001 - event.getValue())) / log1001) * (max - min);
				bsMaxError = value;
				labelText = "Max Error = " + String.format(java.util.Locale.US, "%.4f", bsMaxError);
			} else if (valueAdjust == ValueChange.BSINLIERRATIO){ // BACKGROUND INLIER RATIO
				bsInlierRatio = value;
				// this is ugly fix of the problem when inlier's ratio is 1.0
				if (bsInlierRatio >= 0.999)
					bsInlierRatio = 0.99999f;
				labelText = "Inlier Ratio = " + String.format(java.util.Locale.US, "%.2f", bsInlierRatio);
			} else {
				System.out.println("Attached GeneralListener to the wrong scrollbar");
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
		// path = path.concat("test_background.tif");
		System.out.println(path);

		ImagePlus imp = new Opener().openImage(path);

		if (imp == null)
			System.out.println("image was not loaded");
		else{
			imp.show();
		}

		// 	imp.setSlice(20);


		// imp.setRoi(imp.getWidth() / 4, imp.getHeight() / 4, imp.getWidth() / 2, imp.getHeight() / 2);

		new InteractiveRadialSymmetry().run(null);

		System.out.println("DOGE!");
	}
}
