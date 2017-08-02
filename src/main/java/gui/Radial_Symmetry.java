package gui;

import java.io.File;
import java.util.ArrayList;

import compute.RadialSymmetry;
import fit.Spot;
import gauss.GaussianMaskFit;
import gui.imagej.GenericDialogGUIParams;
import gui.interactive.HelperFunctions;
import gui.interactive.InteractiveRadialSymmetry;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.io.Opener;
import ij.plugin.PlugIn;
import imglib2.RealTypeNormalization;
import imglib2.TypeTransformingRandomAccessibleInterval;
import mpicbg.spim.io.IOFunctions;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.multithreading.SimpleMultiThreading;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import org.apache.commons.math3.analysis.integration.gauss.HermiteRuleFactory;

import parameters.GUIParams;
import parameters.RadialSymmetryParameters;
import test.TestGauss3d;

public class Radial_Symmetry implements PlugIn
{
	// used to save previous values of the fields
	public static String[] paramChoice = new String[] { "Manual", "Interactive" };
	public static int defaultImg = 0;
	public static int defaultParam = 0;
	public static boolean defaultGauss = false;
	public static boolean defaultRANSAC = false;
	
	// steps per octave
	public static int defaultSensitivity = 4;

	// TODO: used to choose the image
	ImagePlus imp;
	int parameterType;
	boolean gaussFit;
	boolean RANSAC;

	// defines the resolution in x y z dimensions
	double [] calibration;

	@Override
	public void run( String arg )
	{	

		boolean wasCanceled = initialDialog();
		
		if ( !wasCanceled  ) // if user didn't cancel
		{
			if ( imp.getNChannels() > 1 )
			{
				// TODO: REMOVE: This part is fixed 
				// IJ.log("Multichannel images are not supported yet ...");
				// return;
			}

			long [] dimensions = new long [imp.getNDimensions()];
			for (int d = 0; d < imp.getNDimensions(); ++d){
				dimensions[d] = imp.getDimensions()[d];
			}
			
			// make some dirty code as it is not defined at compile time, but for all subsequent code it is
			double[] minmax = HelperFunctions.computeMinMax( (Img)ImageJFunctions.wrapReal(imp) );
			
			float min = (float)minmax[ 0 ];
			float max = (float)minmax[ 1 ];
						
			// set all defaults + set RANSAC 
			final GUIParams params = new GUIParams(RANSAC);

			if ( parameterType == 0 ) // Manual
			{
				// set the parameters in the manual mode
				GenericDialogGUIParams gdGUIParams = new GenericDialogGUIParams( params );
				wasCanceled = gdGUIParams.automaticDialog();
				
				if (wasCanceled) 
					return;
				
				// calculations are performed further
			}
			else // interactive
			{
				InteractiveRadialSymmetry irs = new InteractiveRadialSymmetry( imp, params, min, max );

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
			
			RandomAccessibleInterval<FloatType> rai;
			if ( ! Double.isNaN(min)  &&  ! Double.isNaN( max)) // if normalizable
				rai = new TypeTransformingRandomAccessibleInterval<>( ImageJFunctions.wrap(imp), new RealTypeNormalization<>( min, max - min ), new FloatType() );
			else // otherwise use 
				rai = ImageJFunctions.wrap(imp);
		
			// RandomAccessibleInterval<FloatType> rai = new TypeTransformingRandomAccessibleInterval<>( ImageJFunctions.wrap(imp), new RealTypeNormalization<>( min, max - min ), new FloatType() );
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
			
			
			ArrayList<Long> timePoint = new ArrayList<>(0);
			

			for (int c = 0; c < imp.getNChannels(); c++) {
				for (int t = 0; t < imp.getNFrames(); t++) {
					// "-1" because of the imp offset 					
					timeFrame = copyImg(rai, c, t, dim, impDim);
					RadialSymmetry rs = new RadialSymmetry(rsm, timeFrame);
					allSpots.addAll(rs.getSpots());
									
					for (int j = allSpots.size(); j < allSpots.size() + rs.getSpots().size(); j++)
						timePoint.add(new Long(t));
										
					// user wants to have the gauss fit here
//					if (gaussFit){
//						// TODO: Additional gauss fit should trigger here? 
//						// since the locations of all peaks are known here
//						// we can get the value for the gauss fit 
//						
//						// TODO: might not work on 4D images
//						for(Spot spot : rs.getSpots()){
//							
//							long [] minSpot = new long[timeFrame.numDimensions()];
//							long [] maxSpot = new long[timeFrame.numDimensions()];
//							double [] sigmaSpot = new double[timeFrame.numDimensions()];
//									
//							// FIXME: check that center is correct
//							double [] location = new double [timeFrame.numDimensions()];
//							GaussianMaskFit.gaussianMaskFit(Views.interval(timeFrame, minSpot, maxSpot), location, sigmaSpot, null);
//							
//						}
//										
////						GaussianMaskFit.gaussianMaskFit( 
////								final RandomAccessibleInterval<FloatType> signalInterval, 
////								final double[] location, 
////								final double[] sigma, 
////								final Img< FloatType > ransacWeight )
//										
//					}					
					
					IOFunctions.println("t: " + t + " " + "c: " + c);
				}
			}
			
			RadialSymmetry.ransacResultTable(allSpots, timePoint);
			
			
			Img<FloatType> ransacPreview = new ArrayImgFactory<FloatType>().create(rai, new FloatType());			
			// Spot.drawRANSACArea(allSpots, ransacPreview);
			
			// Uncomment after done with 2d + time testing 
			// Spot.showInliers(allSpots, ransacPreview, params.getMaxError());
			// ImageJFunctions.show(ransacPreview);
			
			// DEBUG: REMOVE
			// Img<FloatType> resImg = new ArrayImgFactory<FloatType>().create(rai, new FloatType());
			// double [] resSigma = new double[]{params.getSupportRadius(), params.getSupportRadius(), params.getSupportRadius()};
			// showPoints(resImg, allSpots, resSigma);
			
			// ImageJFunctions.show(resImg).setTitle("Do use the dots?");
			
			System.out.println("ONLY ONCE?");
		}
	}
	
	// DEBUG: 
	public static void showPoints(Img <FloatType> image, ArrayList<Spot> spots, double [] sigma){		
		for ( int i = 0; i < spots.size(); ++i )
		{
			// final Spot spot = spots.get(i);		
			// if not discarded
			if (spots.get(i).numRemoved != spots.get(i).candidates.size()){
				final double[] location = new double[]{spots.get(i).getFloatPosition(0), spots.get(i).getFloatPosition(1), spots.get(i).getFloatPosition(2)};
				TestGauss3d.addGaussian( image, location, sigma );
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
										ra.setPosition(new long[]{pos[0], pos[1], time}); // fix 
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
			initialDialog.addCheckbox("Use_RANSAC", defaultRANSAC);
			
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
				this.RANSAC = defaultRANSAC = initialDialog.getNextBoolean();
			}
		}

		return failed;
	}

	public static void main(String[] args){

		File path = new File( "/Users/kkolyva/Desktop/2017-07-28-Klim-Dhana-radial-symmetry/Ravg.tif" ); //"/Users/kkolyva/Dropbox/PhD/2017-07-13-lab-meeting/data/part SEA12_dpy23_wdr52_mdh1_006.nd2 - SEA12_dpy23_wdr52_mdh1_006.nd2 (series 13) - C=3-1.tif"); //singlespot_sigma_1-1-1.tif"); //
		// path = path.concat("test_background.tif");

		if ( !path.exists() )
			throw new RuntimeException( "'" + path.getAbsolutePath() + "' doesn't exist." );

		new ImageJ();
		System.out.println( "Opening '" + path + "'");

		ImagePlus imp = new Opener().openImage( path.getAbsolutePath() );

		if (imp == null)
			throw new RuntimeException( "image was not loaded" );

		imp.show(); 

		imp.setSlice(121);

		new Radial_Symmetry().run( new String() ); 
		System.out.println("Doge!");
	}
}
