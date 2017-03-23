package gui;

import java.io.File;

import gui.imagej.GenericDialogGUIParams;
import gui.interactive.InteractiveRadialSymmetry;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.io.Opener;
import ij.plugin.PlugIn;
import net.imglib2.multithreading.SimpleMultiThreading;
import parameters.GUIParams;
import parameters.RadialSymmetryParameters;

public class Radial_Symmetry implements PlugIn
{
	// used to save previous values of the fields
	public static String[] paramChoice = new String[] { "Manual", "Interactive" };
	public static int defaultImg = 0;
	public static int defaultParam = 1;
	public static boolean defaultGauss = false;

	// TODO: used to choose the image
	ImagePlus imp;
	int parameterType;
	boolean gaussFit;

	// defines the resolution in x y z dimensions
	double [] calibration;
	
	@Override
	public void run( String arg )
	{	
		
		// Pipeline 
		/*
		 * get the 1 method 2 image 3 fitting
		 * if advanced
		 *   trigger the advanced gui window
		 *   restore the defaults
		 * 	 save the defaults 
		 * else 
		 *   trigger the interactive window
		 *   restore the defaults 
		 *   listen to the values
		 *     recompute the results 
		 *   save the values
		 * trigger the computations for the whole image  	
		 * */
		
		if ( !initialDialog() ) // if user didn't cancel
		{
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
 
			if ( imp.getNChannels() > 1 )
			{
				IJ.log("Multichannel images are not supported yet ...");
				return;
			}

			// set all defaults + initialize the parameters with default values
			final GUIParams params = new GUIParams();
			
			// TODO: call new GenericDialogGUIParams( params );
			// to choose 
			// extra parameters are 

			
			if ( parameterType == 0 ) // Manual
			{
				// imagej stuff
				GenericDialogGUIParams gdGUIParams = new GenericDialogGUIParams( params );
				gdGUIParams.automaticDialog();
				
				// automaticDialog();
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
			
			// TODO: run the processing on the whole image if the user clicked okay 
			
		
			// might have imagej-specific parameters (what to do with channels?)

			// compute on the whole dataset with params
//			for ( c = 0; c < numChannels; ++c )
//				for ( t = 0; t < numTimePoints; ++t )
//				{
//					rai = wrapImagePlus( t, c );
//					new RadialSymmetry< FloatType >( allParams, rai );
//				}
		}


	}

	// TODO: POLISH
	/**
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

	/*
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

	*/
	
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
