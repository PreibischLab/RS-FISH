package gui.anisotropy;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import anisotropy.parameters.AParams;
import background.NormalizedGradient;
import fiji.tool.SliceObserver;
import fitting.Spot;
import gradient.Gradient;
import gradient.GradientPreCompute;
import gui.interactive.HelperFunctions;
import gui.interactive.InteractiveRadialSymmetry;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.io.Opener;
import ij.process.ImageProcessor;
import milkyklim.algorithm.localization.EllipticGaussianOrtho;
import milkyklim.algorithm.localization.GenericPeakFitter;
import milkyklim.algorithm.localization.LevenbergMarquardtSolver;
import milkyklim.algorithm.localization.MLEllipticGaussianEstimator;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.algorithm.localextrema.RefinedPeak;
import net.imglib2.converter.Converters;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.imageplus.ImagePlusImgs;
import net.imglib2.multithreading.SimpleMultiThreading;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import parameters.RadialSymParams;
import radial.symmetry.utils.DoGDetection;

public class AnisotropyCoefficient {

	public static int numIterations = 250; // not a parameter, can be changed through Beanshell

	// TODO: Pass as the parameter (?)
	int sensitivity = RadialSymParams.defaultSensitivity;

	// window that is potentially open
	AnisotropyWindow aWindow; 

	// TODO: Pass them or their values
	SliceObserver sliceObserver;
	SimpleROIListener roiListener;

	// TODO: you probably need one image plus object 
	ImagePlus imagePlus;
	final RandomAccessibleInterval< FloatType > img;
	final RandomAccessible< FloatType > imgExtended;
	final double min, max; // intensity of the imageplus
	final long[] dim;
	final int type;
	Rectangle rectangle;

	final int mode; // defines which method will be used: gaussfit or radial symmetry 

	ArrayList<RefinedPeak<Point>> peaks;

	// TODO: always process only this part of the initial image READ ONLY
	Interval extendedRoi;

	boolean isComputing = false;
	boolean isStarted = false;

	public static enum ValueChange {
		SIGMA, THRESHOLD, SLICE, ROI, ALL
	}

	// stores all the parameters 
	final public AParams params;

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
	public AnisotropyCoefficient(ImagePlus imp, final AParams params, final int mode, final double min, final double max){
		this.imagePlus = imp;
		this.mode = mode;

		dim = new long[] { imp.getWidth(), imp.getHeight() };

		imp.setZ( Math.max( 1, imp.getNSlices() / 2 ) );

		this.params = params; 
		this.min = min; 
		this.max = max;

		if ( Double.isNaN( min ) || Double.isNaN( max ) )
		{
			this.img = Converters.convert( (RandomAccessibleInterval<RealType>)(Object)ImagePlusImgs.from( imp ), (i,o) -> o.set(i.getRealFloat()), new FloatType() );
		}
		else
		{
			final double range = max - min;

			this.img = Converters.convert(
					(RandomAccessibleInterval<RealType>)(Object)ImagePlusImgs.from( imp ),
					(i,o) ->
					{
						o.set( (float)( ( i.getRealFloat() - min ) / range ) );
					},
					new FloatType() );
		}

		this.imgExtended = Views.extendMirrorSingle( img );

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
					Math.min( 100, imagePlus.getWidth() / 2 ),
					Math.min( 100, imagePlus.getHeight() / 2) );

			imagePlus.setRoi( rectangle );
		}

		// show the interactive kit 
		this.aWindow = new AnisotropyWindow( this );
		this.aWindow.getFrame().setVisible( true );

		// add listener to the imageplus slice slider
		sliceObserver = new SliceObserver(imagePlus, new ImagePlusListener( this ));

		roiListener = new SimpleROIListener( this, imagePlus );
		imagePlus.getCanvas().addMouseListener( roiListener );

		// compute first version
		updatePreview(ValueChange.ALL);
		isStarted = true;



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

		float sigma = params.getSigmaDoG(); 
		float threshold = params.getThresholdDoG();
		
		System.out.println(sigma + " " + threshold);

		if (img.numDimensions() != 3)
			System.out.println("Wrong dimensionality of the image");
		else {
			// IMP: in the 3D case the blobs will have lower contrast as a
			// function of sigma(z) therefore we have to adjust the threshold;
			// to fix the problem we use an extra factor = 0.5 which will
			// decrease the threshold value; this might help in some cases but
			// z-extra smoothing is image depended

			// make sure the size is not 0 (is possible in ImageJ when making the Rectangle, not when changing it ... yeah)
			rectangle.width = Math.max( 1, rectangle.width );
			rectangle.height = Math.max( 1, rectangle.height );

			long[] min, max;

			// 'channel', 'slice' and 'frame' are one-based indexes
			min = new long []{
					rectangle.x,
					rectangle.y,
					img.min( 2 ) };
			max = new long []{
					rectangle.width + rectangle.x - 1,
					rectangle.height + rectangle.y - 1,
					img.max( 2 ) };

			extendedRoi = new FinalInterval( min, max);

			dogDetection( this.imgExtended, extendedRoi );

			peaks = HelperFunctions.filterPeaks( peaks, rectangle, params.getThresholdDoG() );

			HelperFunctions.log("Found " + peaks.size() + " peaks in the 3D substack defined by the ROI." );

			if ( mode == 0 ) // gauss fit 
				bestScale = calculateAnisotropyCoefficientGF(img, threshold, sigma); 
			else
				bestScale = calculateAnisotropyCoefficientRS(img, threshold, sigma);

			// bestScale = anisotropyChooseImageDialog();
		}

		HelperFunctions.log("Best scale = " + bestScale);
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
		final List< Point > points = peaks.stream().map( p -> p.getOriginalPeak() ).collect( Collectors.toList() );

		HelperFunctions.log( "Removing background (gauss fit required empty bg)..." );
		RandomAccessibleInterval< FloatType > tmp = ArrayImgs.floats( img.dimension( 0 ), img.dimension( 1 ), img.dimension( 2 ) );
		Gauss3.gauss( 10, imgExtended, tmp );

		RandomAccessibleInterval< FloatType > bg =
				Converters.convert( img, tmp, (i1,i2,o) -> { o.set( Math.max( 0, i1.get() - i2.get() ) ); }, new FloatType() );

		//ImagePlus tmpImp = ImageJFunctions.show( bg );
		//tmpImp.setTitle( "Image used for Gauss fitting");

		HelperFunctions.log( "fitting...." );

		GenericPeakFitter< FloatType, Point > pf = new GenericPeakFitter<>(bg, points,
				new LevenbergMarquardtSolver(), new EllipticGaussianOrtho(),
				new MLEllipticGaussianEstimator(typicalSigmas));
		pf.process();

		final Map< Point, double[] > fits = pf.getResult();

		double [] sigmas = new double[numDimensions];
		for (int d = 0; d < numDimensions; d++)
			sigmas[d] = 0;

		// FIXME: is the order consistent
		for (final Point peak : points)
		{
			double[] params = fits.get( peak );
			System.out.println( Util.printCoordinates( peak ));
			System.out.println( "sigma (fit): " + Util.printCoordinates( params ) );
			for (int j = 0; j < numDimensions; j++){
				sigmas[j] += params[numDimensions + 1 + j];
			}
		}

		// skip division by zero		
		for(int d = 0; d < numDimensions; d++){
			sigmas[d] = (peaks.size() == 0) ? 1 : sigmas[d]/peaks.size();
			sigmas[d] = 1 / (Math.sqrt(2 * sigmas[d]));
		}

		HelperFunctions.log( "sigma (fit): " + Util.printCoordinates( sigmas ) );

		// TODO: here we suppose that the x and y sigmas are the same
		bestScale = sigmas[numDimensions - 1] / sigmas[0]; // x/z

		return 1.0 / bestScale;
	}


	// pass the image with the bead
	// detect it 
	// run with different scalings to see which scaling will produce the best result 
	// is this part working at the moment 
	public double calculateAnisotropyCoefficientRS(RandomAccessibleInterval<FloatType> img, float threshold, float sigma){

		double bestScale = 1.0;
		Gradient derivative = new GradientPreCompute(img);

		final ArrayList<long[]> simplifiedPeaks = new ArrayList<>(1);
		int numDimensions = img.numDimensions();
		// copy all peaks
		// TODO: USE FUNCTION FROM HELPERFUNCTIONS
		for (final RefinedPeak<Point> peak : peaks) {
			if ( Math.abs( peak.getValue()) > threshold) {
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
		final long[] size = new long[numDimensions];

		for (int d = 0; d < numDimensions; ++d)
			size[d] = Math.max( 4, (long)(sigma + 2) );

		// this is a hack
		// range[ 2 ] *= 6;
		// System.out.println( "Range: " + Util.printCoordinates(range));

		// double bestScale = -1;
		long bestTotalInliers = -Long.MAX_VALUE;

		// TODO: SET THE VARAIABLES FOR THE SEARCH SPACE
		for (float idx = 0.25f; idx < 3f; idx += 0.01f){
			final ArrayList<Spot> spots = Spot.extractSpots(img, simplifiedPeaks, derivative, ng, size);

			// scale the z-axis 
			float scale = idx;
			for (int j = 0; j < spots.size(); j++){
				spots.get(j).updateScale(new float []{1, 1, scale});
			}

			// HelperFunctions.log( "num spots: " + spots.size() );

			// TODO: MOVE TO THE DEFAULT PARAMETERS
			// USERS SHOULD NOT ADJUST THIS ONE 
			double maxError = 1.5; // 1.0px error 
			double inlierRatio = 0.2; // at least 20% inliers 
			int minNumLiers = RadialSymParams.defaultMinNumInliers;

			Spot.ransac(spots, numIterations, maxError, inlierRatio, minNumLiers, false, 0.0, 0.0, null, null, null, null, true );
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

			HelperFunctions.log(scale + " inlier pixels=" + totalInliers + " for spots=" + total);
		}

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
			if ( roiListener != null )
				imagePlus.getCanvas().removeMouseListener(roiListener);

			imagePlus.getOverlay().clear();
			imagePlus.updateAndDraw();
		}

		isFinished = true;
	}

	// TODO: fix the check: "==" must not be used with floats
	protected boolean isRoiChanged(final ValueChange change, final Rectangle rect, boolean roiChanged){
		boolean res = false;
		res = (roiChanged || extendedRoi == null || change == ValueChange.SLICE ||rect.getMinX() != rectangle.getMinX()
				|| rect.getMaxX() != rectangle.getMaxX() || rect.getMinY() != rectangle.getMinY()
				|| rect.getMaxY() != rectangle.getMaxY());
		return res;
	}

	/*
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
			roi = imagePlus.getRoi();
			roiChanged = true;
		}

		// Do I need this one or it is just the copy of the same thing?
		// sourceRectangle or rectangle
		final Rectangle roiBounds = roi.getBounds(); 

		// change the img2 size if the roi or the support radius size was changed
		if ( isRoiChanged(change, roiBounds, roiChanged) || change == ValueChange.SIGMA )
		{
			rectangle = roiBounds;

			// make sure the size is not 0 (is possible in ImageJ when making the Rectangle, not when changing it ... yeah)
			rectangle.width = Math.max( 1, rectangle.width );
			rectangle.height = Math.max( 1, rectangle.height );

			// a 2d or 3d view where we'll run DoG on
			RandomAccessibleInterval< FloatType > imgTmp;
			long[] min, max;

			// 3d case

			// 'channel', 'slice' and 'frame' are one-based indexes
			final int currentSlice = imagePlus.getZ() - 1;

			min = new long []{
					rectangle.x,
					rectangle.y,
					Math.max( img.min( 2 ), currentSlice - 1 ) };
			max = new long []{
					rectangle.width + rectangle.x - 1,
					rectangle.height + rectangle.y - 1,
					Math.min( img.max( 2 ), currentSlice + 1 ) };

			extendedRoi = new FinalInterval( min, max);

			roiChanged = true;
		}

		// only recalculate DOG & gradient image if: sigma, slider, ROI
		if (peaks == null || change == ValueChange.SIGMA || change == ValueChange.SLICE || change == ValueChange.ROI || change == ValueChange.ALL )
		{
			dogDetection( this.imgExtended, extendedRoi );
		}

		final double radius = ( params.getSigmaDoG() + HelperFunctions.computeSigma2( params.getSigmaDoG(), sensitivity  ) ) / 2.0;
		final ArrayList< RefinedPeak< Point > > filteredPeaks = HelperFunctions.filterPeaks( peaks, rectangle, params.getThresholdDoG() );

		HelperFunctions.drawRealLocalizable( filteredPeaks, imagePlus, radius, Color.RED, true);

		// TODO Should adjust the parameters? 
		//ransacInteractive( derivative );
		isComputing = false;
	}

	protected void dogDetection( final RandomAccessible <FloatType> image, final Interval interval )
	{
		final double sigma2 = HelperFunctions.computeSigma2( params.getSigmaDoG(), sensitivity );

		double[] calibration = new double[ image.numDimensions() ];
		calibration[ 0 ] = 1.0;
		calibration[ 1 ] = 1.0;
		if ( calibration.length == 3 )
			calibration[ 2 ] = 1.0;

		final DoGDetection<FloatType> dog2 =
				new DoGDetection<>(image, interval, calibration, params.getSigmaDoG(), sigma2 , DoGDetection.ExtremaType.MINIMA, InteractiveRadialSymmetry.thresholdMin, false);
		//final DogDetection<FloatType> dog2 =
				//new DogDetection<>(image, calibration, params.getSigmaDoG(), sigma2 , DogDetection.ExtremaType.MINIMA, InteractiveRadialSymmetry.thresholdMin, false);

		ArrayList<Point> simplePeaks = dog2.getPeaks();
		RandomAccess dog = (Views.extendBorder(dog2.getDogImage())).randomAccess();

		peaks = new ArrayList<>();
		for ( final Point p : simplePeaks )
		{
			dog.setPosition( p );
			peaks.add( new RefinedPeak<Point>( p, p, ((RealType)dog.get()).getRealDouble(), true ) );
		}
		//peaks = dog2.getSubpixelPeaks(); 
	}

	public class SimpleROIListener implements MouseListener {
		final AnisotropyCoefficient parent;
		final ImagePlus source;

		public SimpleROIListener( final AnisotropyCoefficient parent, final ImagePlus s ){
			this.parent = parent;
			this.source = s;
		}

		@Override
		public void mouseClicked(MouseEvent e) {}

		@Override
		public void mouseEntered(MouseEvent e) {}

		@Override
		public void mouseExited(MouseEvent e) {}

		@Override
		public void mousePressed(MouseEvent e) {}

		@Override
		public void mouseReleased(final MouseEvent e) {		
			// here the ROI might have been modified, let's test for that
			final Roi roi = source.getRoi();

			// roi is wrong, clear the screen 
			if (roi == null || roi.getType() != Roi.RECTANGLE){
				source.setRoi( parent.rectangle );
			}

			// TODO: might put the update part for the roi here instead of the updatePreview
			while (parent.isComputing)
				SimpleMultiThreading.threadWait(10);

			parent.updatePreview(ValueChange.ROI);
		}
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

		HelperFunctions.log( "min=" + min );
		HelperFunctions.log( "max=" + max );

		imp.show();

		imp.setSlice(20);


		// imp.setRoi(imp.getWidth() / 4, imp.getHeight() / 4, imp.getWidth() / 2, imp.getHeight() / 2);

		new AnisotropyCoefficient( imp, new AParams(), 0, min, max );

		System.out.println("DOGE!");
	}

}
