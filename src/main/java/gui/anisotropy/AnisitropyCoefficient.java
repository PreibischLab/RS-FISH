package gui.anisotropy;

import java.awt.Color;
import java.awt.Rectangle;
import java.io.File;
import java.util.ArrayList;
import java.util.Map;

import net.imglib2.Point;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.dog.DogDetection;
import net.imglib2.algorithm.localextrema.RefinedPeak;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.multithreading.SimpleMultiThreading;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

import anisotropy.parameters.AParams;
import fiji.tool.SliceObserver;
import fitting.Spot;
import gradient.computation.Gradient;
import gradient.computation.GradientPreCompute;
import gradient.normalized.computation.NormalizedGradient;
import gui.radial.symmetry.interactive.FixROIListener;
import gui.radial.symmetry.interactive.HelperFunctions;
import gui.radial.symmetry.plugin.Radial_Symmetry;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.io.Opener;
import ij.process.ImageProcessor;
import imglib2.type.numeric.real.normalized.RealTypeNormalization;
import imglib2.type.numeric.real.normalized.TypeTransformingRandomAccessibleInterval;
import milkyklim.algorithm.localization.EllipticGaussianOrtho;
import milkyklim.algorithm.localization.GenericPeakFitter;
import milkyklim.algorithm.localization.LevenbergMarquardtSolver;
import milkyklim.algorithm.localization.MLEllipticGaussianEstimator;

public class AnisitropyCoefficient {

	public static int numIterations = 100; // not a parameter, can be changed through Beanshell

	// calibration in xy, usually [1, 1], will be read from ImagePlus upon initialization
	final double[] calibration;

	// TODO: Pass as the parameter (?)
	int sensitivity = Radial_Symmetry.defaultSensitivity;

	// window that is potentially open
	AnysotropyWindow aWindow; 

	// TODO: Pass them or their values
	SliceObserver sliceObserver;
	FixROIListener fixROIListener;

	// TODO: you probably need one image plus object 
	ImagePlus imagePlus;
	final boolean normalize; // do we normalize intensities?
	final double min, max; // intensity of the imageplus
	final long[] dim;
	final int type;
	Rectangle rectangle;

	final String paramType; // defines which method will be used: gaussfit or radial symmetry 

	ArrayList< Point > peaks;
	ArrayList< RefinedPeak< Point> > refPeaks;

	// TODO: always process only this part of the initial image READ ONLY
	RandomAccessibleInterval<FloatType> extendedRoi;
	// the pre-computed gradient
	Gradient derivative;

	boolean isComputing = false;
	boolean isStarted = false;

	public static enum ValueChange {
		SIGMA, THRESHOLD, SLICE, ROI, ALL
	}

	// stores all the parameters 
	final AParams params;

	// min/max value
	final float sigmaMin = 0.5f;
	final float sigmaMax = 10f;
	final float thresholdMin = 0.0001f;
	final float thresholdMax = 1f;

	final int scrollbarSize = 1000;
	// ----------------------------------------

	boolean isFinished = false;
	boolean wasCanceled = false;	

	public boolean isFinished() {
		return isFinished;
	}

	public boolean wasCanceled() {
		return wasCanceled;
	}

	// paramType defines if we use RS or Gaussian Fit
	public AnisitropyCoefficient(ImagePlus imp, final AParams params, final String paramType, final double min, final double max){
		this.imagePlus = imp;
		this.paramType = paramType;

		dim = new long[imp.getNDimensions()];
		for (int d = 0; d< imp.getNDimensions(); ++d)
			this.dim[d] = imp.getDimensions()[d];

		this.params = params; 
		this.min = min; 
		this.max = max;

		if ( Double.isNaN( min ) || Double.isNaN( max ) )
			this.normalize = false;
		else
			this.normalize = true;

		// TODO: do we need this check? Maybe it is enough have a wrapper here?
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

		this.calibration = HelperFunctions.initCalibration(imp, imp.getNDimensions());

		rectangle = new Rectangle(0, 0, (int)dim[0], (int)dim[1]);// always the full image 

		// show the interactive kit 
		this.aWindow = new AnysotropyWindow( this );
		this.aWindow.getFrame().setVisible( true );

		// add listener to the imageplus slice slider
		sliceObserver = new SliceObserver(imagePlus, new ImagePlusListener( this ));
		// compute first version
		updatePreview(ValueChange.ALL);
		isStarted = true;
		// TODO: don't need roi becasue the full image is used
		// check whenever roi is modified to update accordingly
		// imagePlus.getCanvas().addMouseListener( roiListener );


		// triggers on the dispose method 
		do {
			SimpleMultiThreading.threadWait(100);
		} while (!this.isFinished());

		if (this.wasCanceled())
			return;

		// calculateAnisotropyCoefficient();
	}

	// trigger the coefficient calculation
	public double calculateAnisotropyCoefficient(){
		double bestScale = 1.0;

		// System.out.println("BEEP");

		// TODO Add this parameters to the gui?
		float sigma = params.getSigmaDoG(); 
		float threshold = params.getThresholdDoG();
		
		System.out.println(sigma + " " + threshold);

		RandomAccessibleInterval<FloatType> img;
		if (!Double.isNaN(min) && !Double.isNaN(max)) // if normalizable
			img = new TypeTransformingRandomAccessibleInterval<>(ImageJFunctions.wrap(imagePlus),
					new RealTypeNormalization<>(min, max - min), new FloatType());
		else // otherwise use
			img = ImageJFunctions.wrap(imagePlus);

		float sigma2 = HelperFunctions.computeSigma2(sigma, Radial_Symmetry.defaultSensitivity);

		if (img.numDimensions() != 3)
			System.out.println("Wrong dimensionality of the image");
		else {
			// IMP: in the 3D case the blobs will have lower contrast as a
			// function of sigma(z) therefore we have to adjust the threshold;
			// to fix the problem we use an extra factor = 0.5 which will
			// decrease the threshold value; this might help in some cases but
			// z-extra smoothing is image depended

			final float tFactor = img.numDimensions() == 3 ? 0.5f : 1.0f;
			final DogDetection<FloatType> dog2 = new DogDetection<>(img, calibration, sigma, sigma2,
					DogDetection.ExtremaType.MINIMA, tFactor * threshold / 2, false);
			peaks = dog2.getPeaks();
			refPeaks = dog2.getSubpixelPeaks();

			// ugly but this is probably the way at the moment because the 
			// peakfitter is only working with localizable at the moment 
			// this is also the thresholding of the peaks that must be done, in general! 
			refPeaks = HelperFunctions.filterPeaks(refPeaks, threshold);	
			peaks = HelperFunctions.resavePeaks(refPeaks, threshold, img.numDimensions());

			// FIXME: debug 
			// System.out.println("total refined peaks after filtering: " + refPeaks.size());
			System.out.println("total _______ peaks after filtering: " + peaks.size());
			
			
			// FIXME: debug 
			for (Point peak : peaks){
				for (int d = 0; d < peak.numDimensions(); d++)
					System.out.print(peak.getIntPosition(d) + " ");
				System.out.println();
			}

			// TODO: FIXME: Do we have to filter peaks here? 
			// Otherwise we use all peaks that we detected! 

			if (paramType.equals("Gauss Fit")) // gauss fit 
				bestScale = calculateAnisotropyCoefficientGF(img, threshold, sigma); 
			else
				bestScale = calculateAnisotropyCoefficientRS(img, threshold, sigma);

			// bestScale = anisotropyChooseImageDialog();
		}
		System.out.println("Best scale = " + bestScale);
		return bestScale;
	}

	// use gauss fit to detect the anisotropy coefficent of the 3D images
	public double calculateAnisotropyCoefficientGF(RandomAccessibleInterval<FloatType> img, float threshold, float sigma){
		// this one will be returned
		double bestScale = 1; 
		int numDimensions = img.numDimensions();
		// this one is coming from gui
		double [] typicalSigmas = new double[numDimensions];
		for (int d = 0; d < numDimensions; d++)
			typicalSigmas[d] = sigma;

		// TODO: implement the background subtraction here, otherwise peakfitter will give the wrong result 
		
		GenericPeakFitter< FloatType, Point > pf = new GenericPeakFitter<>(img, peaks,
				new LevenbergMarquardtSolver(), new EllipticGaussianOrtho(),
				new MLEllipticGaussianEstimator(typicalSigmas));
		pf.process();

		final Map< Point, double[] > fits = pf.getResult();

		double [] sigmas = new double[numDimensions];
		for (int d = 0; d < numDimensions; d++)
			sigmas[d] = 0;

		// FIXME: is the order consistent
		for (final Point peak : peaks)
		{
			double[] params = fits.get( peak );
			for (int j = 0; j < numDimensions; j++){
				sigmas[j] += params[numDimensions + 1 + j];
			}
		}

		// skip division by zero		
		for(int d = 0; d < numDimensions; d++)
			sigmas[d] = (peaks.size() == 0) ? 1 : sigmas[d]/peaks.size();
		for(int d = 0; d < numDimensions; d++){
			sigmas[d] = 1 / (Math.sqrt(2 * sigmas[d]));
		}

		// TODO: here we suppose that the x and y sigmas are the same
		bestScale = sigmas[numDimensions - 1] / sigmas[0]; // x/z

		return bestScale;
	}


	// pass the image with the bead
	// detect it 
	// run with different scalings to see which scaling will produce the best result 
	// is this part working at the moment 
	public double calculateAnisotropyCoefficientRS(RandomAccessibleInterval<FloatType> img, float threshold, float sigma){

		double bestScale = 1.0;
		derivative = new GradientPreCompute(img);

		final ArrayList<long[]> simplifiedPeaks = new ArrayList<>(1);
		int numDimensions = img.numDimensions();
		// copy all peaks
		// TODO: USE FUNCTION FROM HELPERFUNCTIONS
		for (final RefinedPeak<Point> peak : refPeaks) {
			if (-peak.getValue() > threshold) {
				final long[] coordinates = new long[numDimensions];
				for (int d = 0; d < peak.numDimensions(); ++d)
					coordinates[d] = Util.round(peak.getDoublePosition(d));
				simplifiedPeaks.add(coordinates);
			}
		}

		final NormalizedGradient ng = null; // don't use the gradient normalization for the bead detection 

		// TODO: should the user set it?
		// or it is just enough to use the difference of gaussian value -> sigma + 1? 
		// the size of the RANSAC area
		final long[] range = new long[numDimensions];

		for (int d = 0; d < numDimensions; ++d)
			range[d] = (long)(sigma + 2);

		// this is a hack
		// range[ 2 ] *= 6;
		// System.out.println( "Range: " + Util.printCoordinates(range));

		// double bestScale = -1;
		long bestTotalInliers = -Long.MAX_VALUE;

		// TODO: SET THE VARAIABLES FOR THE SEARCH SPACE
		for (float idx = 1.0f; idx < 1.6f; idx += 0.01f){
			final ArrayList<Spot> spots = Spot.extractSpots(img, simplifiedPeaks, derivative, ng, range);

			// scale the z-axis 
			float scale = idx;
			for (int j = 0; j < spots.size(); j++){
				spots.get(j).updateScale(new float []{1, 1, scale});
			}

			// IJ.log( "num spots: " + spots.size() );

			// TODO: MOVE TO THE DEFAULT PARAMETERS
			// USERS SHOULD NOT ADJUST THIS ONE 
			double maxError = 1.0; // 1.0px error 
			double inlierRatio = 0.6; // at least 60% inliers 

			Spot.ransac(spots, numIterations, maxError, inlierRatio);
			try{
				Spot.fitCandidates(spots);
			}
			catch(Exception e){
				System.out.println("EXCEPTION CAUGHT");
			}				
			// double[] knowLocation = new double[]{ 124.52561137015748, 129.88211102199878, 121.78135663923388 };
			// double dist = 0;

			long totalInliers = 0;
			long total = 0;

			for (final Spot spot : spots)
			{
				if ( spot.inliers.size() == 0)
					continue;

				total++;
				totalInliers += spot.inliers.size();	
			}

			if (totalInliers > bestTotalInliers){
				bestScale = scale;
				bestTotalInliers = totalInliers;
			} 

			IJ.log(scale + " " + (1.0)*totalInliers/total + " " + totalInliers + " " + total);

		}

		IJ.log("best: " + bestScale);


		return bestScale;
	}


	// APPROVED:
	protected final void dispose()
	{
		if ( aWindow.getFrame() != null)
			aWindow.getFrame().dispose();

		if (sliceObserver != null)
			sliceObserver.unregister();

		if ( imagePlus != null) {
			imagePlus.getOverlay().clear();
			imagePlus.updateAndDraw();
		}

		isFinished = true;
	}

	/*
	 * Updates the Preview with the current parameters (sigma, threshold, roi,
	 * slice number + RANSAC parameters)
	 * 
	 * @param change
	 *            - what did change
	 */
	protected void updatePreview(final ValueChange change) {
		// TODO : verify
		long offset = (long)(params.getSigmaDoG() + 1);

		long [] min = new long []{rectangle.x - offset, rectangle.y - offset};
		long [] max = new long []{rectangle.width + rectangle.x + offset - 1, rectangle.height + rectangle.y + offset - 1};

		// get the currently selected slice
		final RandomAccessibleInterval< FloatType > imgTmp;
		if ( normalize )
			imgTmp = new TypeTransformingRandomAccessibleInterval<>( HelperFunctions.toImg( imagePlus, dim, type ), new RealTypeNormalization<>( this.min, this.max - this.min ), new FloatType() );
		else
			imgTmp = HelperFunctions.toImg( imagePlus, dim, type );

		extendedRoi = Views.interval( Views.extendMirrorSingle( imgTmp ), min, max);

		// only recalculate DOG & gradient image if: sigma, slider
		if (peaks == null || change == ValueChange.SIGMA || change == ValueChange.SLICE || change == ValueChange.ALL )
		{
			dogDetection( extendedRoi );
			derivative = new GradientPreCompute( extendedRoi );
		}

		final double radius = ( params.getSigmaDoG() + HelperFunctions.computeSigma2( params.getSigmaDoG(), sensitivity  ) ) / 2.0;
		final ArrayList< RefinedPeak< Point > > filteredPeaks = HelperFunctions.filterPeaks( refPeaks, rectangle, params.getThresholdDoG() );

		HelperFunctions.drawRealLocalizable( filteredPeaks, imagePlus, radius, Color.RED, true);

		// TODO Should adjust the parameters? 
		ransacInteractive( derivative );
		isComputing = false;
	}

	protected void ransacInteractive( final Gradient derivative ) {
		// TODO: I think this problem with the rectangle was previously fixed
		// make sure the size is not 0 (is possible in ImageJ when making the Rectangle, not when changing it ... yeah)
		rectangle.width = Math.max( 1, rectangle.width );
		rectangle.height = Math.max( 1, rectangle.height );

		final ArrayList<long[]> simplifiedPeaks = new ArrayList<>(1);
		int numDimensions = extendedRoi.numDimensions(); // DEBUG: should always be 2 
		// extract peaks for the roi
		HelperFunctions.copyPeaks(refPeaks, simplifiedPeaks, numDimensions, rectangle, params.getThresholdDoG() );

		// the size of the RANSAC area
		final long[] range = new long[numDimensions];

		range[ 0 ] = range[ 1 ] = (long)(2*params.getSigmaDoG() + 1);

		// ImageJFunctions.show(new GradientPreCompute(extendedRoi).preCompute(extendedRoi));
		final NormalizedGradient ng = null;
		final ArrayList<Spot> spots = Spot.extractSpots(extendedRoi, simplifiedPeaks, derivative, ng, range);

		// TODO: CORRECT PLACE TO TURN ON/OFF RANSAC		
		if (true){ // (params.getRANSAC()){
			Spot.ransac(spots, numIterations, params.getMaxError(), params.getInlierRatio());
			for (final Spot spot : spots)
				spot.computeAverageCostInliers();
		}

		// System.out.println("total RS: " + spots.size());


	}

	// APPROVED:
	/*
	 * this function is used for Difference-of-Gaussian calculation in the 
	 * interactive case. No calibration adjustment is needed.
	 * (threshold/2) - because some peaks might be skipped - always compute all spots, select later
	 * */
	protected void dogDetection( final RandomAccessibleInterval <FloatType> image )
	{
		final double sigma2 = HelperFunctions.computeSigma2( params.getSigmaDoG(), sensitivity );
		final DogDetection<FloatType> dog2 = new DogDetection<>(image, calibration, params.getSigmaDoG(), sigma2 , DogDetection.ExtremaType.MINIMA,  params.getThresholdDoG()/2, false);
		peaks = dog2.getPeaks();
		refPeaks = dog2.getSubpixelPeaks(); 
	}

	public static void main(String[] args)
	{
		File path = new File( "/media/milkyklim/Samsung_T3/2017-08-18-radial-symmetry-test/gauss3d-1,2,3.tif" );
		// path = path.concat("test_background.tif");

		if ( !path.exists() )
			throw new RuntimeException( "'" + path.getAbsolutePath() + "' doesn't exist." );

		new ImageJ();
		System.out.println( "Opening '" + path + "'");

		ImagePlus imp = new Opener().openImage( path.getAbsolutePath() );

		if (imp == null)
			throw new RuntimeException( "image was not loaded" );

		float min = Float.MAX_VALUE;
		float max = -Float.MAX_VALUE;

		for ( int z = 1; z <= imp.getStack().getSize(); ++z )
		{
			final ImageProcessor ip = imp.getStack().getProcessor( z );

			for ( int i = 0; i < ip.getPixelCount(); ++i )
			{
				final float v = ip.getf( i );
				min = Math.min( min, v );
				max = Math.max( max, v );
			}
		}

		IJ.log( "min=" + min );
		IJ.log( "max=" + max );

		imp.show();

		imp.setSlice(20);


		// imp.setRoi(imp.getWidth() / 4, imp.getHeight() / 4, imp.getWidth() / 2, imp.getHeight() / 2);

		new AnisitropyCoefficient( imp, new AParams(), "Gauss fit", min, max );

		System.out.println("DOGE!");
	}

}
