package gui.anisotropy;

import java.awt.Color;
import java.awt.Rectangle;
import java.io.File;
import java.util.ArrayList;

import anisotropy.parameters.AParams;
import background.NormalizedGradient;
import background.NormalizedGradientAverage;
import background.NormalizedGradientMedian;
import background.NormalizedGradientRANSAC;
import fiji.tool.SliceObserver;
import fit.Spot;
import fit.Center.CenterMethod;
import gradient.Gradient;
import gradient.GradientPreCompute;
import gui.Radial_Symmetry;
import gui.interactive.FixROIListener;
import gui.interactive.HelperFunctions;
import gui.interactive.InteractiveRadialSymmetry.ValueChange;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.io.Opener;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import imglib2.RealTypeNormalization;
import imglib2.TypeTransformingRandomAccessibleInterval;
import net.imglib2.Point;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.dog.DogDetection;
import net.imglib2.algorithm.localextrema.RefinedPeak;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import parameters.GUIParams;

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
	final ImagePlus imagePlus;
	final boolean normalize; // do we normalize intensities?
	final double min, max; // intensity of the imageplus
	final long[] dim;
	final int type;
	Rectangle rectangle;

	ArrayList<RefinedPeak<Point>> peaks;

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

	public AnisitropyCoefficient(ImagePlus imp, final AParams params, final double min, final double max){
		this.imagePlus = imp;

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

		// TODO: do we need this check? Maybe it is enought have a wrapper here?
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

		// TODO:
		rectangle = new Rectangle(0, 0, 100, 100);// always the  full image 


		// show the interactive kit 
		this.aWindow = new AnysotropyWindow( this );
		this.aWindow.getFrame().setVisible( true );

		// add listener to the imageplus slice slider
		sliceObserver = new SliceObserver(imagePlus, new ImagePlusListener( this ));
		// compute first version
		// updatePreview(ValueChange.ALL);
		isStarted = true;
		// check whenever roi is modified to update accordingly
		// imagePlus.getCanvas().addMouseListener( roiListener );
	}

	// pass the image with the bead
	// detect it 
	// run with different scalings to see which scaling will produce the best result 
	// 
	//	public static void calculateAnisotropyCoefficient(){
	//		double scaling = 1; // return value 
	//
	//		// TODO: read these values from the gui
	//		AParams.sigma = 1.5;         // params.getParams().getSigmaDoG();
	//		threshold =  0.0050; // params.getParams().getThresholdDoG();
	//
	//		float sigma2 = HelperFunctions.computeSigma2(sigma, Radial_Symmetry.defaultSensitivity);
	//		
	//		if (img.numDimensions() != 3)
	//			System.out.println("Wrong dimensionality of the image");
	//		else {
	//			// IMP: in the 3D case the blobs will have lower contrast as a
	//			// function of sigma(z) therefore we have to adjust the threshold;
	//			// to fix the problem we use an extra factor = 0.5 which will
	//			// decrease the threshold value; this might help in some cases but
	//			// z-extra smoothing is image depended
	//
	//			long sTime = System.currentTimeMillis();
	//			final float tFactor = img.numDimensions() == 3 ? 0.5f : 1.0f;
	//			final DogDetection<FloatType> dog2 = new DogDetection<>(img, params.getCalibration(), sigma, sigma2,
	//					DogDetection.ExtremaType.MINIMA, tFactor * threshold / 2, false);
	//			peaks = dog2.getSubpixelPeaks();
	//
	//			derivative = new GradientPreCompute(img);
	//
	//
	//			final ArrayList<long[]> simplifiedPeaks = new ArrayList<>(1);
	//			int numDimensions = img.numDimensions();
	//			// copy all peaks
	//			// TODO: USE FUNCTION FROM HELPERFUNCTIONS
	//			for (final RefinedPeak<Point> peak : peaks) {
	//				if (-peak.getValue() > threshold) {
	//					final long[] coordinates = new long[numDimensions];
	//					for (int d = 0; d < peak.numDimensions(); ++d)
	//						coordinates[d] = Util.round(peak.getDoublePosition(d));
	//					simplifiedPeaks.add(coordinates);
	//				}
	//			}
	//
	//			final NormalizedGradient ng; // don't use the gradient normalization for the bead detection 
	//
	//			// the size of the RANSAC area
	//			final long[] range = new long[numDimensions];
	//
	//			for (int d = 0; d < numDimensions; ++d)
	//				range[d] = supportRadius;
	//
	//			// this is a hack
	//			//range[ 2 ] *= 6;
	//			System.out.println( "Range: " + Util.printCoordinates(range));
	//
	//			//					double bestDist = Double.MAX_VALUE;
	//			//					double bestScale = -1;
	//
	//			//					for (float idx = 1.0f; idx < 1.21f; idx += 0.2f){
	//			spots = Spot.extractSpots(img, simplifiedPeaks, derivative, ng, range);
	//
	//			// TODO: HERE OR BEFORE? 
	//			// ADD SCALING HERE
	//			//						float scaled = idx;
	//			//						for (int j = 0; j < spots.size(); j++){
	//			//							spots.get(j).updateScale(new float []{1, 1, scaled});
	//			//						}
	//
	//
	//			IJ.log( "num spots: " + spots.size() );
	//			if (debug) {
	//				System.out.println("Timing: Extract peaks : " + (System.currentTimeMillis() - sTime) / timingScale);
	//				sTime = System.currentTimeMillis();
	//			}
	//
	//			// TODO: IS THIS A PLACE WHERE YOU CAN SKIP RANSAC
	//			if (params.getParams().getRANSAC()){
	//				Spot.ransac(spots, numIterations, maxError, inlierRatio);
	//			}
	//			else{
	//				try{
	//					Spot.fitCandidates(spots);
	//					//IJ.log( "inliers: " + spots.get(0).inliers.size() );
	//				}
	//				catch(Exception e){
	//					System.out.println("EXCEPTION CAUGHT");
	//				}
	//			}
	//
	//			//						double[] knowLocation = new double[]{ 124.52561137015748, 129.88211102199878, 121.78135663923388 };
	//			//						double dist = 0;
	//			//				
	//			//						double avgInliers = 0;
	//			//						long total = 0;
	//			//						for (final Spot spot : spots)
	//			//						{
	//			//							dist = mpicbg.models.Point.distance( new mpicbg.models.Point( knowLocation), new mpicbg.models.Point( spot.getCenter() ) );
	//			//
	//			//							if ( spot.inliers.size() == 0)
	//			//								continue;
	//			//
	//			//							total++;
	//			//							avgInliers += spot.inliers.size();	
	//			//
	//			//							// IJ.log( "removed " + spot.numRemoved );
	//			//						}
	//			//	
	//			//						if ( dist < bestDist )
	//			//						{
	//			//							bestDist = dist;
	//			//							bestScale = idx;
	//			//						}
	//			//
	//			//						IJ.log(scaled + " " + avgInliers/total + " " + avgInliers + " " + total + " d=" + dist );
	//
	//			//					}
	//
	//			//					IJ.log("best:");
	//			//					IJ.log( bestScale + " " + bestDist );
	//
	//
	//			// TODO: Moved to the main function
	//			//			ransacResultTable(spots);
	//			//			if (debug) {
	//			//				System.out.println("Timing : Results peaks : " + (System.currentTimeMillis() - sTime) / timingScale);
	//			//				sTime = System.currentTimeMillis();
	//			//			}
	//
	//		} else
	//			// TODO: if the code is organized correctly this part should be removed 
	//			System.out.println("Wrong dimensionality. Currently supported 2D/3D!");
	//
	//
	//	}

	
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
		final ArrayList< RefinedPeak< Point > > filteredPeaks = HelperFunctions.filterPeaks( peaks, rectangle, params.getThresholdDoG() );

		HelperFunctions.drawRealLocalizable( filteredPeaks, imagePlus, radius, Color.RED, true);

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
		HelperFunctions.copyPeaks(peaks, simplifiedPeaks, numDimensions, rectangle, params.getThresholdDoG() );

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
				
		System.out.println("total RS: " + spots.size());
	
	
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
		peaks = dog2.getSubpixelPeaks(); 
	}
	

	public static void main(String[] args)
	{
		File path = new File( "/media/milkyklim/Samsung_T3/2017-06-26-radial-symmetry-test/Simulated_3D.tif" );
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

		new AnisitropyCoefficient( imp, new AParams(), min, max );

		System.out.println("DOGE!");
	}

}
