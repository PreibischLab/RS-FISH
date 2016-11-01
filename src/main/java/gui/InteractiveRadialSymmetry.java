package gui;

import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Rectangle;
import java.awt.Scrollbar;
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
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import fiji.tool.SliceListener;
import fiji.tool.SliceObserver;
import fit.Spot;
import gradient.Gradient;
import gradient.GradientPreCompute;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.io.Opener;
import ij.plugin.PlugIn;
import ij.plugin.filter.PlugInFilter;
import ij.plugin.frame.Editor;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import localmaxima.LocalMaxima;
import localmaxima.LocalMaximaDoG;
import mpicbg.imglib.algorithm.gauss.GaussianConvolutionReal;
import mpicbg.imglib.algorithm.math.LocalizablePoint;
import mpicbg.imglib.algorithm.scalespace.DifferenceOfGaussianPeak;
import mpicbg.imglib.algorithm.scalespace.DifferenceOfGaussianReal1;
import mpicbg.imglib.algorithm.scalespace.SubpixelLocalization;
import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.multithreading.SimpleMultiThreading;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyMirrorFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyValueFactory;
import mpicbg.imglib.util.Util;
import mpicbg.imglib.wrapper.ImgLib1;
import mpicbg.imglib.wrapper.ImgLib2;
import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.spim.io.IOFunctions;
import mpicbg.spim.registration.ViewStructure;
import mpicbg.spim.registration.detection.DetectionSegmentation;
import net.imglib2.RandomAccess;
import net.imglib2.exception.ImgLibException;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.img.imageplus.FloatImagePlus;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.ImgUtil;
import net.imglib2.view.Views;
import spim.process.fusion.FusionHelper;
import test.TestGauss3d;

public class InteractiveRadialSymmetry implements PlugIn  {

	// ImagePlus  imp = null;
	ImageStack stack = null;
	ImageStack stackOut = null;

	// DoG parameters
	// TODO: set the parameters in the constructor
	static final double[] sigmaDoG = new double[]{ 0.7, 1.2};
	static double thresholdDoG = 0.1;
	// used to crop the peak
	// static final int[] range = new int[]{ 10, 10, 10 };

	// RANSAC parameters
	static int numIterations = 100;
	static float maxError = 0.15f;
	static float inlierRatio = (float) (20.0/100.0); 


	// used to keep this example running
	static final int TRANSLATION = 0;
	static final int AFFINE = 1;
	String     outputDir = null;
	boolean    stackVirtual = false;
	boolean    outputNewStack = false;
	int        transform = TRANSLATION;
	int        pyramidLevel = 1;
	int        maxIter = 200;
	double     tol = 1e-7;
	double     alpha = 0.9;
	boolean    logEnabled = false;
	Editor     logEditor = null;

	// DoG -------------------------------

	final int extraSize = 40;
	final int scrollbarSize = 1000;

	float sigma = 0.5f;
	float sigma2 = 0.5f;
	float threshold = 0.0001f;

	// steps per octave
	public static int standardSensitivity = 4;
	int sensitivity = standardSensitivity;

	float imageSigma = 0.5f;
	float sigmaMin = 0.5f;
	float sigmaMax = 10f;
	int sigmaInit = 300;

	float thresholdMin = 0.0001f;
	float thresholdMax = 1f;
	int thresholdInit = 500;

	double minIntensityImage = Double.NaN;
	double maxIntensityImage = Double.NaN;

	SliceObserver sliceObserver;
	RoiListener roiListener;
	ImagePlus imp;
	int channel = 0;
	Rectangle rectangle;
	Image<mpicbg.imglib.type.numeric.real.FloatType> img;
	FloatImagePlus< net.imglib2.type.numeric.real.FloatType > source;
	ArrayList<DifferenceOfGaussianPeak<mpicbg.imglib.type.numeric.real.FloatType>> peaks;

	Color originalColor = new Color( 0.8f, 0.8f, 0.8f );
	Color inactiveColor = new Color( 0.95f, 0.95f, 0.95f );
	public Rectangle standardRectangle;
	boolean isComputing = false;
	boolean isStarted = false;
	boolean enableSigma2 = false;
	boolean sigma2IsAdjustable = true;

	boolean lookForMinima = false;
	boolean lookForMaxima = true;

	public static enum ValueChange { SIGMA, THRESHOLD, SLICE, ROI, MINMAX, ALL }

	boolean isFinished = false;
	boolean wasCanceled = false;
	public boolean isFinished() { return isFinished; }
	public boolean wasCanceled() { return wasCanceled; }
	public double getInitialSigma() { return sigma; }
	public void setInitialSigma( final float value ) 
	{ 
		sigma = value; 
		sigmaInit = computeScrollbarPositionFromValue( sigma, sigmaMin, sigmaMax, scrollbarSize );
	}
	public double getSigma2() { return sigma2; }
	public double getThreshold() { return threshold; }
	public void setThreshold( final float value ) 
	{ 
		threshold = value;
		final double log1001 = Math.log10( scrollbarSize + 1);
		thresholdInit = (int)Math.round( 1001-Math.pow(10, -(((threshold - thresholdMin)/(thresholdMax-thresholdMin))*log1001) + log1001 ) );
	}
	public boolean getSigma2WasAdjusted() { return enableSigma2; }
	public boolean getLookForMaxima() { return lookForMaxima; }
	public boolean getLookForMinima() { return lookForMinima; }
	public void setLookForMaxima( final boolean lookForMaxima ) { this.lookForMaxima = lookForMaxima; }
	public void setLookForMinima( final boolean lookForMinima ) { this.lookForMinima = lookForMinima; }

	public void setSigmaMax( final float sigmaMax ) { this.sigmaMax = sigmaMax; }
	public void setSigma2isAdjustable( final boolean state ) { sigma2IsAdjustable = state; }

	// for the case that it is needed again, we can save one conversion
	public FloatImagePlus< net.imglib2.type.numeric.real.FloatType > getConvertedImage() { return source; }

	public InteractiveRadialSymmetry( final ImagePlus imp, final int channel ) 
	{ 
		this.imp = imp;
		this.channel = channel;
	}
	public InteractiveRadialSymmetry( final ImagePlus imp ) { this.imp = imp; }
	public InteractiveRadialSymmetry() {}

	public void setMinIntensityImage( final double min ) { this.minIntensityImage = min; }
	public void setMaxIntensityImage( final double max ) { this.maxIntensityImage = max; }

	// ===================================

	// necessary!
	public int setup(String arg, ImagePlus imp){
		return 1;
	}

	@Override
	public void run( String arg )
	{
		if ( imp == null )
			imp = WindowManager.getCurrentImage();

		standardRectangle = new Rectangle( imp.getWidth()/4, imp.getHeight()/4, imp.getWidth()/2, imp.getHeight()/2 );

		if ( imp.getType() == ImagePlus.COLOR_RGB || imp.getType() == ImagePlus.COLOR_256 )
		{			
			IJ.log( "Color images are not supported, please convert to 8, 16 or 32-bit grayscale" );
			return;
		}

		Roi roi = imp.getRoi();

		if ( roi == null )
		{
			//IJ.log( "A rectangular ROI is required to define the area..." );
			imp.setRoi( standardRectangle );
			roi = imp.getRoi();
		}

		if ( roi.getType() != Roi.RECTANGLE )
		{
			IJ.log( "Only rectangular rois are supported..." );
			return;
		}

		// copy the ImagePlus into an ArrayImage<FloatType> for faster access
		source = convertToFloat( imp, channel, 0, minIntensityImage, maxIntensityImage );

		// show the interactive kit
		displaySliders();

		// add listener to the imageplus slice slider
		sliceObserver = new SliceObserver( imp, new ImagePlusListener() );

		// compute first version
		updatePreview( ValueChange.ALL );		
		isStarted = true;

		// check whenever roi is modified to update accordingly
		roiListener = new RoiListener();
		imp.getCanvas().addMouseListener( roiListener );
	}

	boolean showDialog(ImageProcessor ip) {
		return true;
	}

	/**
	 * Normalize and make a copy of the {@link ImagePlus} into an {@link Image}&gt;FloatType&lt; for faster access when copying the slices
	 * 
	 * @param imp - the {@link ImagePlus} input image
	 * @return - the normalized copy [0...1]
	 */
	public static FloatImagePlus< net.imglib2.type.numeric.real.FloatType > convertToFloat( final ImagePlus imp, int channel, int timepoint )
	{
		return convertToFloat( imp, channel, timepoint, Double.NaN, Double.NaN );
	}

	public static FloatImagePlus< net.imglib2.type.numeric.real.FloatType > convertToFloat( final ImagePlus imp, int channel, int timepoint, final double min, final double max )
	{
		// stupid 1-offset of imagej
		channel++;
		timepoint++;

		final int h = imp.getHeight();
		final int w = imp.getWidth();


		//		System.out.print("being paranoid here...");
		//		imp.show("check the content of imp");
		//		imp.updateAndDraw();


		final ArrayList< float[] > img = new ArrayList< float[] >();

		if ( imp.getProcessor() instanceof FloatProcessor )
		{
			for ( int z = 0; z < imp.getNSlices(); ++z )
				img.add( ( (float[])imp.getStack().getProcessor( imp.getStackIndex( channel, z + 1, timepoint ) ).getPixels() ).clone() );
		}
		else if ( imp.getProcessor() instanceof ByteProcessor )
		{
			for ( int z = 0; z < imp.getNSlices(); ++z )
			{
				final byte[] pixels = (byte[])imp.getStack().getProcessor( imp.getStackIndex( channel, z + 1, timepoint ) ).getPixels();
				final float[] pixelsF = new float[ pixels.length ];

				for ( int i = 0; i < pixels.length; ++i )
					pixelsF[ i ] = pixels[ i ] & 0xff;

				img.add( pixelsF );
			}
		}
		else if ( imp.getProcessor() instanceof ShortProcessor )
		{
			for ( int z = 0; z < imp.getNSlices(); ++z )
			{
				final short[] pixels = (short[])imp.getStack().getProcessor( imp.getStackIndex( channel, z + 1, timepoint ) ).getPixels();
				final float[] pixelsF = new float[ pixels.length ];

				for ( int i = 0; i < pixels.length; ++i )
					pixelsF[ i ] = pixels[ i ] & 0xffff;

				img.add( pixelsF );
			}
		}
		else // some color stuff or so 
		{
			for ( int z = 0; z < imp.getNSlices(); ++z )
			{
				final ImageProcessor ip = imp.getStack().getProcessor( imp.getStackIndex( channel, z + 1, timepoint ) );
				final float[] pixelsF = new float[ w * h ];

				int i = 0;

				for ( int y = 0; y < h; ++y )
					for ( int x = 0; x < w; ++x )
						pixelsF[ i++ ] = ip.getPixelValue( x, y );

				img.add( pixelsF );
			}
		}

		final FloatImagePlus< net.imglib2.type.numeric.real.FloatType > i = createImgLib2( img, w, h );

		if ( Double.isNaN( min ) || Double.isNaN( max ) || Double.isInfinite( min ) || Double.isInfinite( max ) || min == max )
			FusionHelper.normalizeImage( i );
		else
			FusionHelper.normalizeImage( i, (float)min, (float)max );

		// DEBUG:
		// ImageJFunctions.show(i).setTitle("i pic");

		return i;
	}

	public static FloatImagePlus< net.imglib2.type.numeric.real.FloatType > createImgLib2( final List< float[] > img, final int w, final int h )
	{
		final ImagePlus imp;

		if ( img.size() > 1 )
		{
			final ImageStack stack = new ImageStack( w, h );
			for ( int z = 0; z < img.size(); ++z )
				stack.addSlice( new FloatProcessor( w, h, img.get( z ) ) );
			imp = new ImagePlus( "ImgLib2 FloatImagePlus (3d)", stack );
		}
		else
		{
			imp = new ImagePlus( "ImgLib2 FloatImagePlus (2d)", new FloatProcessor( w, h, img.get( 0 ) ) );
		}

		return ImagePlusAdapter.wrapFloat( imp );
	}


	// call of the radial symmetry algorithm
	// TODO: everything is supposed to be FloatType only
	// TODO: figure out what has to in the gui
	public static void algorithmCall(Img<FloatType> img) {

		// size around the detection to use
		// we detect at 0.5, 0.5, 0.5 - so we need an even size

		// TODO: insted of makeing new image 
		// use current one with overlapping
		// this sigmaLocal is used to plot the points 
		// maybe this value will be taken from another place
		final double[] sigmaLocal = new double[]{ 2, 2, 2 };
		final LocalMaxima candiateSearch = new LocalMaximaDoG( img, sigmaDoG[0], sigmaDoG[1], thresholdDoG );
		final ArrayList< int[] > peaks = candiateSearch.estimateLocalMaxima();

		final Gradient derivative;
		//derivative = new DerivativeOnDemand( image );
		derivative = new GradientPreCompute( img );
		final int[] range = new int []{10, 10, 10};
		final ArrayList< Spot > spots = Spot.extractSpots( img, peaks, derivative, range );

		// DEBUG: 
		System.out.println("spots size = " + spots.size());
		Spot.ransac( spots, numIterations, maxError, inlierRatio );

		// TODO: make a form for the results
		// instead of ptin outs
		// print localizations
		for ( final Spot spot : spots )
		{
			spot.computeAverageCostInliers();

			//if ( spot.numRemoved != spot.candidates.size() )
			// System.out.println( spot );
		}
		int foundCorrect = 0;
		double avgDist = 0;

		// TODO: figure out what is necessary here

		System.out.println( "found " + spots.size() );

		final Img< FloatType > draw = img.factory().create( img, img.firstElement() );
		Spot.drawRANSACArea( spots, draw );
		ImageJFunctions.show( draw );

		final Img< FloatType > detected = img.factory().create( img, img.firstElement() );
		for ( final Spot spot : spots )
		{
			if ( spot.inliers.size() == 0 )
				continue;

			final double[] location = new double[ img.numDimensions() ];//{ spot.center.getXc(), spot.center.getYc(), spot.center.getZc() };
			spot.center.getSymmetryCenter( location );
			TestGauss3d.addGaussian( detected, location, sigmaLocal );			
		}
		ImageJFunctions.show( detected );
		System.out.println("Completed!");
	}

	protected void runRansac() {
		try{
			// size around the detection to use
			// we detect at 0.5, 0.5, 0.5 - so we need an even size

			// TODO: insted of makeing new image 
			// use current one with overlapping
			// this sigmaLocal is used to plot the points 
			// maybe this value will be taken from another place

			final double[] sigmaLocal = new double[]{ 2, 2, 2 };


			// this part is done in the in updatePreview
			// final LocalMaxima candiateSearch = new LocalMaximaDoG( source, sigmaDoG[0], sigmaDoG[1], thresholdDoG );
			final ArrayList< int[] > simplifiedPeaks = new ArrayList<int[]>(1); // candiateSearch.estimateLocalMaxima();
			
			for ( final DifferenceOfGaussianPeak<mpicbg.imglib.type.numeric.real.FloatType> peak : peaks )
			{
				if ( ( peak.isMax() && lookForMaxima ) || ( peak.isMin() && lookForMinima ) )
				{
					final float x = peak.getPosition( 0 ); 
					final float y = peak.getPosition( 1 );

					// take only peaks that are inside of the image
					if ( Math.abs( peak.getValue().get() ) > threshold &&
							x >= extraSize/2 && y >= extraSize/2 &&
							x < rectangle.width+extraSize/2 && y < rectangle.height+extraSize/2 )
					{
						 						
						simplifiedPeaks.add(new int[]{Util.round( x - sigma ) + rectangle.x - extraSize/2, 
													  Util.round( y - sigma ) + rectangle.y - extraSize/2});
					}
				}
			}
			
			
			
			
			
//			for (final DifferenceOfGaussianPeak<mpicbg.imglib.type.numeric.real.FloatType> peak : peaks){
//				simplifiedPeaks.add(peak.getPosition());
//				for (int d = 0; d < source.numDimensions() - 1; ++d){
//					// System.out.print(peak.getPosition(d) + " ");
//					// System.out.print(simplifiedPeaks.get(simplifiedPeaks.size() - 1)[d] + " ");
//				}
//				// System.out.println();    
//			}
			
			System.out.println("Checkpoint #1!");
			
			
			Rectangle sourceRectangle = new Rectangle( 0, 0, source.getWidth(), source.getHeight());
			mpicbg.imglib.image.display.imagej.ImageJFunctions.show(extractImage(source, sourceRectangle, 0));
			
			// ImageJFunctions.show(ImgLib1.wrapFloatToImgLib2(extractImage(source, sourceRectangle, 0))).setTitle("Greetings, friend");
			
			System.out.println("Checkpoint #2!");

			final Gradient derivative;
			//derivative = new DerivativeOnDemand( source );
			derivative = new GradientPreCompute( ImgLib1.wrapFloatToImgLib2(extractImage(source, sourceRectangle, 0)) );
			System.out.println("Checkpoint #3!");
			
			final int[] range = new int[]{ 10, 10 };
			final ArrayList< Spot > spots = Spot.extractSpots( ImgLib1.wrapFloatToImgLib2(extractImage(source, sourceRectangle, 0)), simplifiedPeaks, derivative, range );

			System.out.println("Checkpoint #4!");


			// DEBUG: 
			System.out.println("spots size = " + spots.size());
			//		try{
			Spot.ransac( spots, numIterations, maxError, inlierRatio );
			
			System.out.println("Checkpoint #5!");

			for ( final Spot spot : spots )
			{
				spot.computeAverageCostInliers();
				//if ( spot.numRemoved != spot.candidates.size() )
				// System.out.println( spot );
			}

			System.out.println( "found " + spots.size() );

			final Img< FloatType > draw = source.factory().create( ImgLib1.wrapFloatToImgLib2(extractImage(source, sourceRectangle, 0)), source.firstElement() );
			
			System.out.println("Checkpoint #6!");
			
			Spot.drawRANSACArea( spots, draw );
			ImageJFunctions.show( draw );

			
			
			final Img< FloatType > detected = source.factory().create( ImgLib1.wrapFloatToImgLib2(extractImage(source, sourceRectangle, 0)), source.firstElement() );
			for ( final Spot spot : spots )
			{
				if ( spot.inliers.size() == 0 )
					continue;

				final double[] location = new double[ source.numDimensions() ];//{ spot.center.getXc(), spot.center.getYc(), spot.center.getZc() };

				spot.center.getSymmetryCenter( location );


				TestGauss3d.addGaussian( detected, location, sigmaLocal );		
			}
			ImageJFunctions.show( detected );
			System.out.println("Completed!");

		}
		catch (Exception e) 
		{
			System.out.println( "e1" );
		}
	}

	/**
	 * Instantiates the panel for adjusting the paramters
	 */
	protected void displaySliders()
	{
		final Frame frame = new Frame("Adjust Difference-of-Gaussian Values");
		frame.setSize( 400, 330 );

		/* Instantiation */
		final GridBagLayout layout = new GridBagLayout();
		final GridBagConstraints c = new GridBagConstraints();

		final Scrollbar sigma1 = new Scrollbar ( Scrollbar.HORIZONTAL, sigmaInit, 10, 0, 10 + scrollbarSize );		
		this.sigma = computeValueFromScrollbarPosition( sigmaInit, sigmaMin, sigmaMax, scrollbarSize); 

		final Scrollbar threshold = new Scrollbar ( Scrollbar.HORIZONTAL, thresholdInit, 10, 0, 10 + scrollbarSize );
		final float log1001 = (float)Math.log10( scrollbarSize + 1);

		this.threshold = thresholdMin + ( (log1001 - (float)Math.log10(1001-thresholdInit))/log1001 ) * (thresholdMax-thresholdMin);

		this.sigma2 = computeSigma2( this.sigma, this.sensitivity );
		final int sigma2init = computeScrollbarPositionFromValue( this.sigma2, sigmaMin, sigmaMax, scrollbarSize ); 
		final Scrollbar sigma2 = new Scrollbar ( Scrollbar.HORIZONTAL, sigma2init, 10, 0, 10 + scrollbarSize );

		final Label sigmaText1 = new Label( "Sigma 1 = " + this.sigma, Label.CENTER );
		final Label sigmaText2 = new Label( "Sigma 2 = " + this.sigma2, Label.CENTER );

		final Label thresholdText = new Label( "Threshold = " + this.threshold, Label.CENTER );
		final Button apply = new Button( "Apply to Stack (will take some time)" );
		final Button button = new Button( "Done" );
		final Button cancel = new Button( "Cancel" );

		final Checkbox sigma2Enable = new Checkbox( "Enable Manual Adjustment of Sigma 2 ", enableSigma2 );
		final Checkbox min = new Checkbox( "Look for Minima (red)", lookForMinima );
		final Checkbox max = new Checkbox( "Look for Maxima (green)", lookForMaxima );

		/* Location */
		frame.setLayout( layout );

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		frame.add ( sigma1, c );

		++c.gridy;
		frame.add( sigmaText1, c );

		++c.gridy;
		frame.add ( sigma2, c );

		++c.gridy;
		frame.add( sigmaText2, c );

		++c.gridy;
		c.insets = new Insets(0,65,0,65);
		frame.add( sigma2Enable, c );

		++c.gridy;
		c.insets = new Insets(10,0,0,0);
		frame.add ( threshold, c );
		c.insets = new Insets(0,0,0,0);

		++c.gridy;
		frame.add( thresholdText, c );

		++c.gridy;
		c.insets = new Insets(0,130,0,75);
		frame.add( min, c );

		++c.gridy;
		c.insets = new Insets(0,125,0,75);
		frame.add( max, c );

		++c.gridy;
		c.insets = new Insets(0,75,0,75);
		frame.add( apply, c );

		++c.gridy;
		c.insets = new Insets(10,150,0,150);
		frame.add( button, c );

		++c.gridy;
		c.insets = new Insets(10,150,0,150);
		frame.add( cancel, c );

		/* Configuration */
		sigma1.addAdjustmentListener( new SigmaListener( sigmaText1, sigmaMin, sigmaMax, scrollbarSize, sigma1, sigma2, sigmaText2 ) );
		sigma2.addAdjustmentListener( new Sigma2Listener( sigmaMin, sigmaMax, scrollbarSize, sigma2, sigmaText2 ) );
		threshold.addAdjustmentListener( new ThresholdListener( thresholdText, thresholdMin, thresholdMax ) );
		button.addActionListener( new FinishedButtonListener( frame, false ) );
		cancel.addActionListener( new FinishedButtonListener( frame, true ) );
		apply.addActionListener( new ApplyButtonListener() );
		min.addItemListener( new MinListener() );
		max.addItemListener( new MaxListener() );
		sigma2Enable.addItemListener( new EnableListener( sigma2, sigmaText2 ) );

		if ( !sigma2IsAdjustable )
			sigma2Enable.setEnabled( false );

		frame.addWindowListener( new FrameListener( frame ) );

		frame.setVisible( true );

		originalColor = sigma2.getBackground();
		sigma2.setBackground( inactiveColor );
		sigmaText1.setFont( sigmaText1.getFont().deriveFont( Font.BOLD ) );
		thresholdText.setFont( thresholdText.getFont().deriveFont( Font.BOLD ) );
	}

	public static float computeSigma2( final float sigma1, final int sensitivity )
	{
		final float k = (float)DetectionSegmentation.computeK( sensitivity );
		final float[] sigma = DetectionSegmentation.computeSigma( k, sigma1 );

		return sigma[ 1 ];
	}

	/**
	 * Updates the Preview with the current parameters (sigma, threshold, roi, slicenumber)
	 * 
	 * @param change - what did change
	 */
	protected void updatePreview( final ValueChange change )
	{		
		// check if Roi changed
		boolean roiChanged = false;
		Roi roi = imp.getRoi();

		if ( roi == null || roi.getType() != Roi.RECTANGLE )
		{
			imp.setRoi( new Rectangle( standardRectangle ) );
			roi = imp.getRoi();
			roiChanged = true;
		}

		final Rectangle rect = roi.getBounds();

		if ( roiChanged || img == null || change == ValueChange.SLICE || 
				rect.getMinX() != rectangle.getMinX() || rect.getMaxX() != rectangle.getMaxX() ||
				rect.getMinY() != rectangle.getMinY() || rect.getMaxY() != rectangle.getMaxY() )
		{
			rectangle = rect;
			// TODO: Here we should adjust the image slice!!!
			// change source, otherwise we always take the initial slice
			img = extractImage( source, rectangle, extraSize );
			roiChanged = true;
		}

		// if we got some mouse click but the ROI did not change we can return
		if ( !roiChanged && change == ValueChange.ROI )
		{
			isComputing = false;
			return;
		}

		// compute the Difference Of Gaussian if necessary
		if ( peaks == null || roiChanged || change == ValueChange.SIGMA || change == ValueChange.SLICE || change == ValueChange.ALL )
		{
			//
			// Compute the Sigmas for the gaussian folding
			//

			final float k, K_MIN1_INV;
			final float[] sigma, sigmaDiff;

			if ( enableSigma2 )
			{				
				sigma = new float[ 2 ];
				sigma[ 0 ] = this.sigma;
				sigma[ 1 ] = this.sigma2;
				k = sigma[ 1 ] / sigma[ 0 ];
				K_MIN1_INV = DetectionSegmentation.computeKWeight( k );
				sigmaDiff = DetectionSegmentation.computeSigmaDiff( sigma, imageSigma );
			}
			else
			{
				k = (float)DetectionSegmentation.computeK( sensitivity );
				K_MIN1_INV = DetectionSegmentation.computeKWeight( k );
				sigma = DetectionSegmentation.computeSigma( k, this.sigma );
				sigmaDiff = DetectionSegmentation.computeSigmaDiff( sigma, imageSigma );
			}

			// the upper boundary
			this.sigma2 = sigma[ 1 ];

			final DifferenceOfGaussianReal1<mpicbg.imglib.type.numeric.real.FloatType> dog = new DifferenceOfGaussianReal1<mpicbg.imglib.type.numeric.real.FloatType>( img, new OutOfBoundsStrategyValueFactory<mpicbg.imglib.type.numeric.real.FloatType>(), sigmaDiff[ 0 ], sigmaDiff[ 1 ], thresholdMin/4, K_MIN1_INV );
			dog.setKeepDoGImage( true );
			dog.process();

			final SubpixelLocalization<mpicbg.imglib.type.numeric.real.FloatType> subpixel = new SubpixelLocalization<mpicbg.imglib.type.numeric.real.FloatType>( dog.getDoGImage(), dog.getPeaks() );
			subpixel.process();

			// peaks contain some values that are out of bounds
			peaks = dog.getPeaks();
			
			// drop unnecessary peaks in ransac
			runRansac();
			
		}

		// DEBUG: DELETE WHEN DONE
//		for (int j = 0; j < peaks.size(); ++j){			
//			int[] pos = peaks.get(j).getPosition().clone();
//			System.out.print(j + ": ");
//			for(int d = 0; d < pos.length; ++d)
//				System.out.print(pos[d] + " ");
//			System.out.println();
//		}
		
		
		// extract peaks to show
		Overlay o = imp.getOverlay();

		if ( o == null )
		{
			o = new Overlay();
			imp.setOverlay( o );
		}

		o.clear();

		

		for ( final DifferenceOfGaussianPeak<mpicbg.imglib.type.numeric.real.FloatType> peak : peaks )
		{
			if ( ( peak.isMax() && lookForMaxima ) || ( peak.isMin() && lookForMinima ) )
			{
				final float x = peak.getPosition( 0 ); 
				final float y = peak.getPosition( 1 );

				// take only peaks that are inside of the image
				if ( Math.abs( peak.getValue().get() ) > threshold &&
						x >= extraSize/2 && y >= extraSize/2 &&
						x < rect.width+extraSize/2 && y < rect.height+extraSize/2 )
				{
					final OvalRoi or = new OvalRoi( Util.round( x - sigma ) + rect.x - extraSize/2, Util.round( y - sigma ) + rect.y - extraSize/2, Util.round( sigma+sigma2 ), Util.round( sigma+sigma2 ) );

					if ( peak.isMax() )
						or.setStrokeColor( Color.green );
					else if ( peak.isMin() )
						or.setStrokeColor( Color.red );

					o.add( or );
				}
			}
		}



		imp.updateAndDraw();

		isComputing = false;
	}

	/**
	 * Extract the current 2d region of interest from the source image
	 * 
	 * @param source - the source image, a {@link Image} which is a copy of the {@link ImagePlus}
	 * @param rectangle - the area of interest
	 * @param extraSize - the extra size around so that detections at the border of the roi are not messed up
	 * @return
	 */
	protected Image<mpicbg.imglib.type.numeric.real.FloatType> extractImage( final FloatImagePlus< net.imglib2.type.numeric.real.FloatType > source, final Rectangle rectangle, final int extraSize )
	{
		final Image<mpicbg.imglib.type.numeric.real.FloatType> img = new ImageFactory<mpicbg.imglib.type.numeric.real.FloatType>( new mpicbg.imglib.type.numeric.real.FloatType(), new ArrayContainerFactory() ).createImage( new int[]{ rectangle.width+extraSize, rectangle.height+extraSize } );

		final int offsetX = rectangle.x - extraSize/2;
		final int offsetY = rectangle.y - extraSize/2;

		final int[] location = new int[ source.numDimensions() ];

		if ( location.length > 2 )
			location[ 2 ] = (imp.getCurrentSlice()-1)/imp.getNChannels();

		final LocalizableCursor<mpicbg.imglib.type.numeric.real.FloatType> cursor = img.createLocalizableCursor();
		final RandomAccess<net.imglib2.type.numeric.real.FloatType> positionable;

		if ( offsetX >= 0 && offsetY >= 0 && 
				offsetX + img.getDimension( 0 ) < source.dimension( 0 ) && 
				offsetY + img.getDimension( 1 ) < source.dimension( 1 ) )
		{
			// it is completely inside so we need no outofbounds for copying
			positionable = source.randomAccess();
		}
		else
		{
			positionable = Views.extendMirrorSingle( source ).randomAccess();
		}

		while ( cursor.hasNext() )
		{
			cursor.fwd();
			cursor.getPosition( location );

			location[ 0 ] += offsetX;
			location[ 1 ] += offsetY;

			positionable.setPosition( location );

			cursor.getType().set( positionable.get().get() );
		}

		return img;
	}

	protected class EnableListener implements ItemListener
	{
		final Scrollbar sigma2;
		final Label sigmaText2;

		public EnableListener( final Scrollbar sigma2, final Label sigmaText2 )
		{
			this.sigmaText2 = sigmaText2;
			this.sigma2 = sigma2;
		}

		@Override
		public void itemStateChanged( final ItemEvent arg0 )
		{
			if ( arg0.getStateChange() == ItemEvent.DESELECTED )
			{
				sigmaText2.setFont( sigmaText2.getFont().deriveFont( Font.PLAIN ) );
				sigma2.setBackground( inactiveColor );
				enableSigma2 = false;
			}
			else if ( arg0.getStateChange() == ItemEvent.SELECTED  )
			{
				sigmaText2.setFont( sigmaText2.getFont().deriveFont( Font.BOLD ) );
				sigma2.setBackground( originalColor );
				enableSigma2 = true;
			}
		}
	}

	protected class MinListener implements ItemListener
	{
		@Override
		public void itemStateChanged( final ItemEvent arg0 )
		{
			boolean oldState = lookForMinima;

			if ( arg0.getStateChange() == ItemEvent.DESELECTED )				
				lookForMinima = false;			
			else if ( arg0.getStateChange() == ItemEvent.SELECTED  )
				lookForMinima = true;

			if ( lookForMinima != oldState )
			{
				while ( isComputing )
					SimpleMultiThreading.threadWait( 10 );

				updatePreview( ValueChange.MINMAX );
			}
		}
	}

	protected class MaxListener implements ItemListener
	{
		@Override
		public void itemStateChanged( final ItemEvent arg0 )
		{
			boolean oldState = lookForMaxima;

			if ( arg0.getStateChange() == ItemEvent.DESELECTED )				
				lookForMaxima = false;			
			else if ( arg0.getStateChange() == ItemEvent.SELECTED  )
				lookForMaxima = true;

			if ( lookForMaxima != oldState )
			{
				while ( isComputing )
					SimpleMultiThreading.threadWait( 10 );

				updatePreview( ValueChange.MINMAX );
			}
		}
	}

	/**
	 * Tests whether the ROI was changed and will recompute the preview 
	 * 
	 * @author Stephan Preibisch
	 */
	protected class RoiListener implements MouseListener
	{
		@Override
		public void mouseClicked(MouseEvent e) {}

		@Override
		public void mouseEntered(MouseEvent e) {}

		@Override
		public void mouseExited(MouseEvent e) {}

		@Override
		public void mousePressed(MouseEvent e) {}

		@Override
		public void mouseReleased( final MouseEvent e )
		{
			// here the ROI might have been modified, let's test for that
			final Roi roi = imp.getRoi();

			if ( roi == null || roi.getType() != Roi.RECTANGLE )
				return;

			while ( isComputing )
				SimpleMultiThreading.threadWait( 10 );

			updatePreview( ValueChange.ROI );				
		}

	}

	protected class ApplyButtonListener implements ActionListener
	{
		@Override
		public void actionPerformed( final ActionEvent arg0 ) 
		{
			ImagePlus imp;

			try
			{
				imp = source.getImagePlus();
			}
			catch (ImgLibException e)
			{
				imp = null;
				e.printStackTrace();
			}

			// convert ImgLib2 image to ImgLib1 image via the imageplus
			final Image< mpicbg.imglib.type.numeric.real.FloatType > source = mpicbg.imglib.image.display.imagej.ImageJFunctions.wrap(imp);

			IOFunctions.println( "Computing DoG ... " );

			// test the parameters on the complete stack
			final ArrayList<DifferenceOfGaussianPeak<mpicbg.imglib.type.numeric.real.FloatType>> peaks = 
					DetectionSegmentation.extractBeadsLaPlaceImgLib( 
							source, 
							new OutOfBoundsStrategyMirrorFactory<mpicbg.imglib.type.numeric.real.FloatType>(), 
							imageSigma, 
							sigma,
							sigma2,
							threshold, 
							threshold/4, 
							lookForMaxima,
							lookForMinima,
							ViewStructure.DEBUG_MAIN );

			IOFunctions.println( "Drawing DoG result ... " );

			// display as extra image
			Image<mpicbg.imglib.type.numeric.real.FloatType> detections = source.createNewImage();
			final LocalizableByDimCursor<mpicbg.imglib.type.numeric.real.FloatType> c = detections.createLocalizableByDimCursor();

			for ( final DifferenceOfGaussianPeak<mpicbg.imglib.type.numeric.real.FloatType> peak : peaks )
			{
				final LocalizablePoint p = new LocalizablePoint( new float[]{ peak.getSubPixelPosition( 0 ), peak.getSubPixelPosition( 1 ), peak.getSubPixelPosition( 2 ) } );

				c.setPosition( p );
				c.getType().set( 1 );
			}

			IOFunctions.println( "Convolving DoG result ... " );

			final GaussianConvolutionReal<mpicbg.imglib.type.numeric.real.FloatType> gauss = new GaussianConvolutionReal<mpicbg.imglib.type.numeric.real.FloatType>( detections, new OutOfBoundsStrategyValueFactory<mpicbg.imglib.type.numeric.real.FloatType>(), 2 );
			gauss.process();

			detections = gauss.getResult();

			IOFunctions.println( "Showing DoG result ... " );

			mpicbg.imglib.image.display.imagej.ImageJFunctions.show(detections) ;

		}
	}

	protected class FinishedButtonListener implements ActionListener
	{
		final Frame parent;
		final boolean cancel;

		public FinishedButtonListener( Frame parent, final boolean cancel )
		{
			this.parent = parent;
			this.cancel = cancel;
		}

		@Override
		public void actionPerformed( final ActionEvent arg0 ) 
		{
			wasCanceled = cancel;
			close( parent, sliceObserver, imp, roiListener );
		}
	}

	protected class FrameListener extends WindowAdapter
	{
		final Frame parent;

		public FrameListener( Frame parent )
		{
			super();
			this.parent = parent;
		}

		@Override
		public void windowClosing (WindowEvent e) 
		{ 
			close( parent, sliceObserver, imp, roiListener );
		}
	}

	protected final void close( final Frame parent, final SliceObserver sliceObserver, final ImagePlus imp, final RoiListener roiListener )
	{
		if ( parent != null )
			parent.dispose();

		if ( sliceObserver != null )
			sliceObserver.unregister();

		if ( imp != null )
		{
			if ( roiListener != null )
				imp.getCanvas().removeMouseListener( roiListener );

			imp.getOverlay().clear();
			imp.updateAndDraw();
		}

		isFinished = true;
	}

	protected class Sigma2Listener implements AdjustmentListener
	{
		final float min, max;
		final int scrollbarSize;

		final Scrollbar sigmaScrollbar2;
		final Label sigma2Label;

		public Sigma2Listener( final float min, final float max, final int scrollbarSize, final Scrollbar sigmaScrollbar2, final Label sigma2Label )
		{
			this.min = min;
			this.max = max;
			this.scrollbarSize = scrollbarSize;

			this.sigmaScrollbar2 = sigmaScrollbar2;
			this.sigma2Label = sigma2Label;
		}

		@Override
		public void adjustmentValueChanged( final AdjustmentEvent event )
		{
			if ( enableSigma2 )
			{
				sigma2 = computeValueFromScrollbarPosition( event.getValue(), min, max, scrollbarSize );

				if ( sigma2 < sigma )
				{
					sigma2 = sigma + 0.001f;
					sigmaScrollbar2.setValue( computeScrollbarPositionFromValue( sigma2, min, max, scrollbarSize ) );
				}

				sigma2Label.setText( "Sigma 2 = " + sigma2 );

				if ( !event.getValueIsAdjusting() )
				{
					while ( isComputing )
					{
						SimpleMultiThreading.threadWait( 10 );
					}
					updatePreview( ValueChange.SIGMA );
				}

			}
			else
			{
				// if no manual adjustment simply reset it
				sigmaScrollbar2.setValue( computeScrollbarPositionFromValue( sigma2, min, max, scrollbarSize ) );
			}
		}		
	}

	protected class SigmaListener implements AdjustmentListener
	{
		final Label label;
		final float min, max;
		final int scrollbarSize;

		final Scrollbar sigmaScrollbar1;
		final Scrollbar sigmaScrollbar2;		
		final Label sigmaText2;

		public SigmaListener( final Label label, final float min, final float max, final int scrollbarSize, final Scrollbar sigmaScrollbar1,  final Scrollbar sigmaScrollbar2, final Label sigmaText2  )
		{
			this.label = label;
			this.min = min;
			this.max = max;
			this.scrollbarSize = scrollbarSize;

			this.sigmaScrollbar1 = sigmaScrollbar1;
			this.sigmaScrollbar2 = sigmaScrollbar2;
			this.sigmaText2 = sigmaText2;
		}

		@Override
		public void adjustmentValueChanged( final AdjustmentEvent event )
		{
			sigma = computeValueFromScrollbarPosition( event.getValue(), min, max, scrollbarSize );			

			if ( !enableSigma2 )
			{
				sigma2 = computeSigma2( sigma, sensitivity );
				sigmaText2.setText( "Sigma 2 = " + sigma2 );			    
				sigmaScrollbar2.setValue( computeScrollbarPositionFromValue( sigma2, min, max, scrollbarSize ) );
			}
			else if ( sigma > sigma2 )
			{
				sigma = sigma2 - 0.001f;
				sigmaScrollbar1.setValue( computeScrollbarPositionFromValue( sigma, min, max, scrollbarSize ) );
			}

			label.setText( "Sigma 1 = " + sigma );
			// TODO: Looks like this one is never executed!
			if ( !event.getValueIsAdjusting() )
			{
				while ( isComputing )
				{
					SimpleMultiThreading.threadWait( 10 );
				}
				updatePreview( ValueChange.SIGMA );
			}
		}		
	}

	protected static float computeValueFromScrollbarPosition( final int scrollbarPosition, final float min, final float max, final int scrollbarSize )
	{
		return min + (scrollbarPosition/(float)scrollbarSize) * (max-min);
	}

	protected static int computeScrollbarPositionFromValue( final float sigma, final float min, final float max, final int scrollbarSize )
	{
		return Util.round( ((sigma - min)/(max-min)) * scrollbarSize );
	}

	protected class ThresholdListener implements AdjustmentListener
	{
		final Label label;
		final float min, max;
		final float log1001 = (float)Math.log10(1001);

		public ThresholdListener( final Label label, final float min, final float max )
		{
			this.label = label;
			this.min = min;
			this.max = max;
		}

		@Override
		public void adjustmentValueChanged( final AdjustmentEvent event )
		{			
			threshold = min + ( (log1001 - (float)Math.log10(1001-event.getValue()))/log1001 ) * (max-min);
			label.setText( "Threshold = " + threshold );

			if ( !isComputing )
			{
				updatePreview( ValueChange.THRESHOLD );
			}
			else if ( !event.getValueIsAdjusting() )
			{
				while ( isComputing )
				{
					SimpleMultiThreading.threadWait( 10 );
				}
				updatePreview( ValueChange.THRESHOLD );
			}
		}		
	}

	protected class ImagePlusListener implements SliceListener
	{
		@Override
		public void sliceChanged(ImagePlus arg0)
		{
			if ( isStarted )
			{
				System.out.println("Slice changed!");
				while ( isComputing )
				{
					SimpleMultiThreading.threadWait( 10 );
				}
				updatePreview( ValueChange.SLICE );
			}
		}		
	}


	public static void testPlugin() throws NotEnoughDataPointsException, IllDefinedDataPointsException{
		String pathMac = "/Users/kkolyva/Desktop/latest_desktop/";
		String pathUbuntu = "/home/milkyklim/Desktop/latest_desktop/";

		String path = pathMac;
		Img<FloatType> img = ImgLib2Util.openAs32Bit(new File(path + "multiple_dots.tif"));

		ImagePlus imp = ImageJFunctions.wrap(img, "tImage");

		// RadialSymmetryGUI tGui = new RadialSymmetryGUI();
		// tGui.setup("no args", imp);
		// tGui.run(imp.getProcessor());

		imp.show();
		// imp.setSlice( 27 );		
		// imp.setRoi( imp.getWidth()/4, imp.getHeight()/4, imp.getWidth()/2, imp.getHeight()/2 );
		// InteractiveDoG iDoG = new InteractiveDoG(); 

		// iDoG.run(null);

		algorithmCall(img);

		GenericDialog gd = new GenericDialog("");
		gd.addTextAreas("Hello", "There", 2, 5);
		gd.showDialog();
	}

	public static void main(String[] args) throws NotEnoughDataPointsException, IllDefinedDataPointsException{
		//testPlugin();

		new ImageJ();

		ImagePlus imp = new Opener().openImage( "/Users/kkolyva/Desktop/latest_desktop/multiple_dots.tif" );

		//ImagePlus imp = new Opener().openImage( "D:/Documents and Settings/Stephan/My Documents/Downloads/1-315--0.08-isotropic-subvolume/1-315--0.08-isotropic-subvolume.tif" );
		imp.show();

		imp.setSlice( 20 );		
		// imp.setRoi( imp.getWidth()/4, imp.getHeight()/4, imp.getWidth()/2, imp.getHeight()/2 );	
		imp.setRoi( 0, 0, imp.getWidth()-1, imp.getHeight() - 1 );	

		new InteractiveRadialSymmetry().run( null ); 

		System.out.println("DOGE!");
	}
}
