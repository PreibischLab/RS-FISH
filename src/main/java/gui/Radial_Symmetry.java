package gui;

import java.io.File;
import java.util.ArrayList;

import compute.RadialSymmetry;
import fit.Spot;
import gui.imagej.GenericDialogGUIParams;
import gui.interactive.HelperFunctions;
import gui.interactive.InteractiveRadialSymmetry;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.io.Opener;
import ij.measure.Calibration;
import ij.plugin.PlugIn;
import imglib2.RealTypeNormalization;
import imglib2.TypeTransformingRandomAccessibleInterval;
import net.imglib2.Cursor;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.localextrema.RefinedPeak;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.multithreading.SimpleMultiThreading;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import parameters.GUIParams;
import parameters.RadialSymmetryParameters;

public class Radial_Symmetry implements PlugIn
{
	// used to save previous values of the fields
	public static String[] paramChoice = new String[] { "Manual", "Interactive" };
	public static int defaultImg = 0;
	public static int defaultParam = 1;
	public static boolean defaultGauss = false;

	// steps per octave
	public static int defaultSensitivity = 4;

	// TODO: used to choose the image
	ImagePlus imp;
	int parameterType;
	boolean gaussFit;

	// defines the resolution in x y z dimensions
	double [] calibration;

	@Override
	public void run( String arg )
	{	

		if ( !initialDialog() ) // if user didn't cancel
		{
			if ( imp.getNChannels() > 1 )
			{
				IJ.log("Multichannel images are not supported yet ...");
				return;
			}

			// set all defaults + initialize the parameters with default values
			final GUIParams params = new GUIParams();

			if ( parameterType == 0 ) // Manual
			{
				// set the parameters in the manual mode
				GenericDialogGUIParams gdGUIParams = new GenericDialogGUIParams( params );
				gdGUIParams.automaticDialog();
				// calculations are performed further
			}
			else // interactive
			{
				InteractiveRadialSymmetry irs = new InteractiveRadialSymmetry( imp, params );

				do
				{
					SimpleMultiThreading.threadWait( 100 );
				}
				while ( !irs.isFinished() );

				if ( irs.wasCanceled() )
					return;
			}

			// back up the parameter values to the default variables
			params.setDefaultValues(); 
			calibration = HelperFunctions.initCalibration(imp, imp.getNDimensions()); // new double[]{1, 1, 1};

			RadialSymmetryParameters rsm = new RadialSymmetryParameters(params, calibration);
			
			// which type of imageplus image is it?
			int type = -1;
			final Object pixels = imp.getProcessor().getPixels();
			if ( pixels instanceof byte[] )
				type = 0;
			else if ( pixels instanceof short[] )
				type = 1;
			else if ( pixels instanceof float[] )
				type = 2;
			else
				throw new RuntimeException( "Pixels of this type are not supported: " + pixels.getClass().getSimpleName() );

			// Normalize the full image
			double[] minmax = HelperFunctions.getMinMax(ImageJFunctions.wrap(imp));
			double min = minmax[0];
			double max = minmax[1];						
			RandomAccessibleInterval<FloatType> rai = new TypeTransformingRandomAccessibleInterval<>( ImageJFunctions.wrap(imp), new RealTypeNormalization<>( min, max - min ), new FloatType() );
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

			for (int c = 1; c <= imp.getNChannels(); c++) {
				for (int t = 1; t <= imp.getNFrames(); t++) {
					// "-1" because of the imp offset 
					// ImageJFunctions.show(rai).setTitle("rai");
					
					timeFrame = copyImg(rai, c - 1, t - 1, dim, impDim);
					// ImageJFunctions.show(timeFrame);
					// if (true)break;
					// TODO: process img
					RadialSymmetry rs = new RadialSymmetry(rsm, timeFrame);
					allSpots.addAll(rs.getSpots());
				}
			}

		}
	}

	public static RandomAccessibleInterval<FloatType> copyImg(RandomAccessibleInterval<FloatType> rai, long channel, long time, long[] dim, int[] impDim) {
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
			if (impDim[2] != 1 && impDim[3] != 1 && impDim[4] != 1){ // full 5D stack
				ra.setPosition(new long[]{pos[0], pos[1], channel, pos[2], time});	
			}
			else{
				if(impDim[2] != 1 && impDim[3] != 1){ // channels + z
					ra.setPosition(new long[]{pos[0], pos[1], channel, pos[2]});
				}
				else 
					if(impDim[2] != 1 && impDim[4] != 1){ // channels + time
						ra.setPosition(new long[]{pos[0], pos[1], channel, time});
					}
					else 
						if (impDim[3] != 1 && impDim[4] != 1){ // z + time
							ra.setPosition(new long[]{pos[0], pos[1], pos[2], time});
						}
						else 
							if (impDim[2] != 1){ // c 
								ra.setPosition(new long[]{pos[0], pos[1], pos[2]});
							}
							else
								if (impDim[3] != 1){ // z
									ra.setPosition(new long[]{pos[0], pos[1], pos[2]});
								}
								else
									if (impDim[4] != 1){ // t 
										ra.setPosition(new long[]{pos[0], pos[1], pos[2]});
									}
									else // 2D image										 
											ra.setPosition(new long[]{pos[0], pos[1]});
			}
				
			cursor.get().setReal(ra.get().getRealFloat());
		}

		return img;
	}

	// TODO: POLISH
	/*
	 * shows the initial GUI dialog 
	 * user has to choose 
	 * an image 
	 * a processing method -- advanced/interactive
	 * */
	protected boolean initialDialog(){
		boolean failed = false;
		// check that the are images
		final int[] imgIdList = WindowManager.getIDList();
		if (imgIdList == null || imgIdList.length < 1)
		{
			IJ.error("You need at least one open image.");
			failed = true;
		}
		else
		{
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
			initialDialog.showDialog();

			if ( initialDialog.wasCanceled() )
			{
				failed = true;
			}
			else
			{
				// Save current index and current choice here 
				int tmp = defaultImg = initialDialog.getNextChoiceIndex();
				this.imp = WindowManager.getImage( imgIdList[ tmp ] );
				this.parameterType = defaultParam = initialDialog.getNextChoiceIndex();
				this.gaussFit = defaultGauss = initialDialog.getNextBoolean();
			}
		}

		return failed;
	}

	public static void main(String[] args){

		File path = new File( "src/main/resources/multiple_dots.tif" );
		// path = path.concat("test_background.tif");

		if ( !path.exists() )
			throw new RuntimeException( "'" + path.getAbsolutePath() + "' doesn't exist." );

		new ImageJ();
		System.out.println( "Opening '" + path + "'");

		ImagePlus imp = new Opener().openImage( path.getAbsolutePath() );

		if (imp == null)
			throw new RuntimeException( "image was not loaded" );

		imp.show();

		imp.setSlice(20);

		new Radial_Symmetry().run( new String() ); 
		System.out.println("Doge!");
	}
}
