package gui.interactive;

import java.awt.Color;
import java.awt.Rectangle;
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
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.io.Opener;
import ij.measure.ResultsTable;
import ij.process.FloatProcessor;
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
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

public class InteractiveRadialSymmetry
{
	// fake 2d-calibration
	final double[] calibration = new double[]{ 1, 1 };

	// RANSAC parameters
	// current value
	int numIterations = 100;
	float maxError = 3.0f;
	float inlierRatio = (float) (75.0 / 100.0);
	int supportRadius = 5;

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

	// TODO: after moving to imglib2 REMOVE
	// steps per octave
	public static int standardSensitivity = 4;
	int sensitivity = standardSensitivity;

	// TODO: keep observers
	SliceObserver sliceObserver;
	ROIListener roiListener;
	FixROIListener fixROIListener;

	// TODO: you probably need one image plus object 
	final ImagePlus imagePlus;
	final long[] dim;
	final int type;
	Rectangle rectangle;

	ArrayList<RefinedPeak<Point>> peaks;

	// TODO: always process only this part of the initial image READ ONLY
	RandomAccessibleInterval<FloatType> extendedRoi;
	// the pre-computed gradient
	Gradient derivative;

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

	public boolean isFinished() {
		return isFinished;
	}

	public boolean wasCanceled() {
		return wasCanceled;
	}

	/**
	 * Single-channel imageplus, 2d or 3d or 4d
	 * 
	 * TODO: 4d!
	 * 
	 * @param imp
	 */
	public InteractiveRadialSymmetry( final ImagePlus imp )
	{
		this.imagePlus = imp;

		// which type of imageplus image is it?
		final Object pixels = imp.getProcessor().getPixels();
		if ( pixels instanceof byte[] )
			this.type = 0;
		else if ( pixels instanceof short[] )
			this.type = 1;
		else if ( pixels instanceof float[] )
			this.type = 2;
		else
			throw new RuntimeException( "Pixels of this type are not supported: " + pixels.getClass().getSimpleName() );

		this.dim = new long[]{ imp.getWidth(), imp.getHeight() };

		final Roi roi = imagePlus.getRoi();

		if ( roi != null && roi.getType() == Roi.RECTANGLE  )
		{
			rectangle = roi.getBounds();
		}
		else
		{
			// initial rectangle
			rectangle = new Rectangle(
					imagePlus.getWidth() / 4,
					imagePlus.getHeight() / 4,
					imagePlus.getWidth() / 2,
					imagePlus.getHeight() / 2);

			// IJ.log( "A rectangular ROI is required to define the area..." );
			imagePlus.setRoi( rectangle );
		}

		// initialize variables for interactive preview
		// called before updatePreview() !
		ransacPreviewInit( imagePlus );

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


	/**
	 * Initialize preview variables for RANSAC
	 */
	// TODO: might be not necessary
	protected void ransacPreviewInit( final ImagePlus imp )
	{
		int width = imp.getWidth();
		int height = imp.getHeight();

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

	protected void ransacInteractive( final Gradient derivative ) {
		// make sure the size is not 0 (is possible in ImageJ when making the Rectangle, not when changing it ... yeah)
		rectangle.width = Math.max( 1, rectangle.width );
		rectangle.height = Math.max( 1, rectangle.height );

		final ArrayList<long[]> simplifiedPeaks = new ArrayList<>(1);
		int numDimensions = extendedRoi.numDimensions(); // DEBUG: should always be 2 
		// extract peaks for the roi
		HelperFunctions.copyPeaks(peaks, simplifiedPeaks, numDimensions, rectangle, threshold );

		// the size of the RANSAC area
		final long[] range = new long[numDimensions];

		range[ 0 ] = range[ 1 ] = 2*supportRadius;

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

			final OvalRoi or = new OvalRoi(
					spot.center.getSymmetryCenter( 0 ) - sigma,
					spot.center.getSymmetryCenter( 1 ) - sigma,
					Util.round(sigma + sigma2),
					Util.round(sigma + sigma2));

			or.setStrokeColor(Color.ORANGE);
			overlay.add(or);
		}
		imagePlus.updateAndDraw();
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

			// get the currently selected slice
			final Img< FloatType > imgTmp = HelperFunctions.toImg( imagePlus, dim, type );

			extendedRoi = Views.interval( Views.extendMirrorSingle( imgTmp ), min, max);

			roiChanged = true;
		}

		// only recalculate DOG & gradient image if: sigma, roi (also through support region), slider
		if (roiChanged || peaks == null || change == ValueChange.SIGMA || change == ValueChange.SLICE || change == ValueChange.ALL )
		{
			dogDetection( extendedRoi );
			derivative = new GradientPreCompute( extendedRoi );
		}

		showPeaks( imagePlus, rectangle, threshold );
		imagePlus.updateAndDraw();

		ransacInteractive( derivative );
		isComputing = false;
	}

	// APPROVED:
	/**
	 * this function is used for Difference-of-Gaussian calculation in the 
	 * interactive case. No calibration adjustment is needed.
	 * (thresholdMin/2) - because some peaks might be skipped - always compute all spots, select later
	 * */
	protected void dogDetection(RandomAccessibleInterval <FloatType> image){
		final DogDetection<FloatType> dog2 = new DogDetection<>(image, calibration, this.sigma, this.sigma2 , DogDetection.ExtremaType.MINIMA,  this.thresholdMin/2, false);
		peaks = dog2.getSubpixelPeaks(); 
	}

	// extract peaks to show
	// TODO: Check changes: but should be fine now
	protected void showPeaks( final ImagePlus imp, final Rectangle rectangle, final double threshold )
	{
		Overlay o = imp.getOverlay();

		if (o == null) {
			o = new Overlay();
			imp.setOverlay(o);
		}

		o.clear();
		for (final RefinedPeak<Point> peak : peaks) {

			final float x = peak.getFloatPosition(0);
			final float y = peak.getFloatPosition(1);

			// TODO: This check criteria is totally wrong!!! - should be fixed
			if (HelperFunctions.isInside( peak, rectangle ) && (-peak.getValue() > threshold)){ // I guess the peak.getValue function returns the value in scale-space

				final OvalRoi or = new OvalRoi(Util.round(x - sigma),
						Util.round(y - sigma), Util.round(sigma + sigma2),
						Util.round(sigma + sigma2));

				or.setStrokeColor(Color.RED);
				o.add(or);
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

		imp.setSlice(20);


		// imp.setRoi(imp.getWidth() / 4, imp.getHeight() / 4, imp.getWidth() / 2, imp.getHeight() / 2);

		new InteractiveRadialSymmetry( imp );

		System.out.println("DOGE!");
	}
}
