package gui;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.ArrayList;

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
import fiji.tool.SliceObserver;
import fit.Center.CenterMethod;
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
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

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

	// Frames that are potentially open
	BackgroundRANSACWindow bkWindow;
	DoGWindow dogWindow;
	RANSACWindow ransacWindow;

	// Background Subtraction parameters 
	// current values 
	float bsMaxError = 0.05f;
	float bsInlierRatio = 75.0f / 100.0f;
	int bsNumIterations = 100;
	int bsMethod = 0;

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
	final static String [] bsMethods = new String []{ "No background subtraction", "Mean", "Median", "RANSAC on Mean", "RANSAC on Median" };

	// TODO: after moving to imglib2 REMOVE
	// steps per octave
	public static int standardSensitivity = 4;
	int sensitivity = standardSensitivity;

	// TODO: keep observers
	SliceObserver sliceObserver;
	ROIListener roiListener;
	FixROIListener fixROIListener;

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

	boolean isFinished = false;
	boolean wasCanceled = false;	

	// used to save previous values of the fields
	public static String[] paramChoice = new String[] { "Manual", "Interactive" };
	public static int defaultImg = 0;
	public static int defaultParam = 1;
	public static boolean defaultGauss = false;

	public static boolean defaultBackgroundSubtraction = false;
	public static int defaultMethodBS;

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

		if (!failed)
		{
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
				if (parameterAdjustment.equals(paramChoice[1])){
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
						this.dogWindow = new DoGWindow( this );
						this.dogWindow.getFrame().setVisible( true );

						// show the interactive ransac kit
						this.ransacWindow = new RANSACWindow( this );
						this.ransacWindow.getFrame().setVisible( true );

						// add listener to the imageplus slice slider
						sliceObserver = new SliceObserver(imagePlus, new ImagePlusListener( this ));
						// compute first version
						updatePreview(ValueChange.ALL);
						isStarted = true;
						// check whenever roi is modified to update accordingly
						roiListener = new ROIListener( this, imagePlus, impRansacError );
						imagePlus.getCanvas().addMouseListener( roiListener );
						fixROIListener = new FixROIListener( imagePlus, impRansacError );
						impRansacError.getCanvas().addMouseListener( fixROIListener );
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
	private void fittingDialog(){
		boolean canceled = false;

		GenericDialog gd = new GenericDialog("Background Subtraction Method");
		gd.addChoice( "Method :", bsMethods, bsMethods[ defaultMethodBS ] );

		gd.showDialog();

		// Should I move this to the return statement
		bsMethod = defaultMethodBS = gd.getNextChoiceIndex();

		if (gd.wasCanceled()) 
			canceled = true;

		if (canceled)
			return;
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

		// ImageJFunctions.show(new GradientPreCompute(extendedRoi).preCompute(extendedRoi));
		final NormalizedGradient ng;

		// "No background subtraction", "Mean", "Median", "RANSAC on Mean", "RANSAC on Median"
		if ( bsMethod == 0 )
			ng = null;
		else if ( bsMethod == 1 )
			ng = new NormalizedGradientAverage( derivative );
		else if ( bsMethod == 2 )
			ng = new NormalizedGradientMedian( derivative );
		else if ( bsMethod == 3 )
			ng = new NormalizedGradientRANSAC( derivative, CenterMethod.MEAN, bsMaxError, bsInlierRatio );
		else if ( bsMethod == 4 )
			ng = new NormalizedGradientRANSAC( derivative, CenterMethod.MEDIAN, bsMaxError, bsInlierRatio );
		else
			throw new RuntimeException( "Unknown bsMethod: " + bsMethod );

		final ArrayList<Spot> spots = Spot.extractSpots(extendedRoi, simplifiedPeaks, derivative, ng, range);

		// TODO: where this part should be applied
		// applyBackgroundSubtraction(spots, imgOut);

		Spot.ransac(spots, numIterations, maxError, inlierRatio);
		for (final Spot spot : spots)
			spot.computeAverageCostInliers();
		ransacResults(spots);
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
		SimpleMultiThreading.threadHaltUnClean();
		//throw new RuntimeException( "backgroundsubt. missing" );
		
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

		this.sigma2 = HelperFunctions.computeSigma2(this.sigma, sensitivity);
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

		if ( roi == null || roi.getType() != Roi.RECTANGLE )
		{
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

	protected final void dispose()
	{
		if ( dogWindow.getFrame() != null)
			dogWindow.getFrame().dispose();

		if ( ransacWindow.getFrame() != null)
			ransacWindow.getFrame().dispose();

		if ( bkWindow != null )
			bkWindow.getFrame().dispose();

		if (sliceObserver != null)
			sliceObserver.unregister();

		if ( imagePlus != null) {
			if (roiListener != null)
				imagePlus.getCanvas().removeMouseListener(roiListener);

			imagePlus.getOverlay().clear();
			imagePlus.updateAndDraw();
		}

		if ( impRansacError != null )
			impRansacError.close();

		isFinished = true;
	}

	public static void main(String[] args)
	{
		File path = new File( "src/main/resources/multiple_dots.tif" );
		// path = path.concat("test_background.tif");

		if ( !path.exists() )
			throw new RuntimeException( "'" + path.getAbsolutePath() + "' doesn't exist." );

		new ImageJ();
		System.out.println( "Opening '" + path + "'");

		ImagePlus imp = new Opener().openImage( path.getAbsolutePath() );

		if (imp == null)
			throw new RuntimeException( "image was not loaded" );
		else
			imp.show();

		// 	imp.setSlice(20);


		// imp.setRoi(imp.getWidth() / 4, imp.getHeight() / 4, imp.getWidth() / 2, imp.getHeight() / 2);

		new InteractiveRadialSymmetry().run(null);

		System.out.println("DOGE!");
	}
}
