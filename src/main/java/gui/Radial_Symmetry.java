/*-
 * #%L
 * A plugin for radial symmetry localization on smFISH (and other) images.
 * %%
 * Copyright (C) 2016 - 2025 RS-FISH developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */
package gui;

import java.awt.Font;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import compute.RadialSymmetry;
import fiji.util.gui.GenericDialogPlus;
import fitting.Spot;
import gui.interactive.HelperFunctions;
import gui.interactive.InteractiveRadialSymmetry;
import gui.vizualization.Visualization;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.PointRoi;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.img.imageplus.ImagePlusImgs;
import net.imglib2.multithreading.SimpleMultiThreading;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import parameters.RadialSymParams;
import result.output.ShowResult;

public class Radial_Symmetry implements PlugIn
{
	public static ImagePlus lastImp = null;
	public static ResultsTable lastRt = null;

	public static boolean defaultUseMultithreading = false;
	public static int[] defaultBlockSize = new int[] { 128, 128, 16 };
	public static int defaultNumThreads = Math.max( 1, Prefs.getThreads() );
	
	@Override
	public void run(String arg) {

		// get list of open image stacks
		final int[] idList = WindowManager.getIDList();

		if ( idList == null || idList.length == 0 )
		{
			IJ.error( "You need at least one open image." );
			return;
		}

		// map all id's to image title for those who are 3d stacks
		final String[] imgList =
				Arrays.stream( idList ).
						mapToObj( id -> WindowManager.getImage( id ).getTitle() ).
							toArray( String[]::new );

		if ( RadialSymParams.defaultImg >= imgList.length )
			RadialSymParams.defaultImg = 0;

		GenericDialog gd1 = new GenericDialog( "Radial Symmetry (2d/3d)" );

		gd1.addChoice( "Image", imgList, imgList[ RadialSymParams.defaultImg ] );
		gd1.addChoice( "Mode", RadialSymParams.modeChoice, RadialSymParams.modeChoice[ RadialSymParams.defaultMode ] );
		gd1.addNumericField( "Anisotropy coefficient", RadialSymParams.defaultAnisotropy, 4, 15, "s(z)/s(xy)" );

		gd1.addMessage( "<html>*Use the \"Anisotropy Coefficient Plugin\"<br/>to calculate the anisotropy coefficient<br/> or leave 1.00 for a hopefully reasonable result.", new Font( "Default", Font.ITALIC, 10 ) );

		gd1.addMessage( "Computation:", new Font( "Default", Font.BOLD, 13 ) );
		gd1.addChoice( "Robust_fitting", RadialSymParams.ransacChoice, RadialSymParams.ransacChoice[ RadialSymParams.defaultRANSACChoice ] );
		gd1.addCheckbox( "Compute_min/max intensity from image", RadialSymParams.defaultAutoMinMax );
		gd1.addCheckbox( "Use_anisotropy coefficient for DoG", RadialSymParams.defaultUseAnisotropyForDoG );
		gd1.addChoice( "Spot_intensity", RadialSymParams.intensityMethods, RadialSymParams.intensityMethods[ RadialSymParams.defaultIntensityMethod ] );
		//gd1.addCheckbox( "Refine_spot_location with Gaussian fit on inliers", RadialSymParams.defaultGaussFitLocation );

		gd1.addMessage( "Visualization:", new Font( "Default", Font.BOLD, 13 ) );
		gd1.addCheckbox( "Add detections to ROI-Manager", RadialSymParams.defaultAddToROIManager );
		//gd1.addCheckbox( "Visualize_Inliers (RANSAC)", RadialSymParams.defaultVisualizeInliers );

		gd1.showDialog();
		if ( gd1.wasCanceled() )
			return;

		// set the parameters from the defaults
		final RadialSymParams params = new RadialSymParams();

		// don't do it by name as often multiple images have the same name
		ImagePlus imp = lastImp = WindowManager.getImage( idList[ RadialSymParams.defaultImg = gd1.getNextChoiceIndex() ] );
		int mode = RadialSymParams.defaultMode = gd1.getNextChoiceIndex();
		params.anisotropyCoefficient = RadialSymParams.defaultAnisotropy = gd1.getNextNumber();
		params.ransacSelection = RadialSymParams.defaultRANSACChoice = gd1.getNextChoiceIndex();

		params.autoMinMax = RadialSymParams.defaultAutoMinMax = gd1.getNextBoolean();
		params.useAnisotropyForDoG = RadialSymParams.defaultUseAnisotropyForDoG = gd1.getNextBoolean();
		params.intensityMethod = RadialSymParams.defaultIntensityMethod = gd1.getNextChoiceIndex();
		//params.gaussFitLocation = RadialSymParams.defaultGaussFitLocation = gd2.getNextBoolean();

		params.addToROIManager = RadialSymParams.defaultAddToROIManager = gd1.getNextBoolean();
		//boolean visInliers = RadialSymParams.defaultVisualizeInliers = gd1.getNextBoolean();

		if (imp.getNChannels() > 1)
		{
			// TODO: show a Choice with all channels, make the user choose one and replace the imp with a single channel
			// TOOD: overlay, BDV, might not work downstream
			HelperFunctions.log( "Multichannel image detected. Please select 'Image > Color > Split Channels' and run RS-FISH on each channel separately (they very likely need different parameters).");
			return;
		}

		if ( params.RANSAC().ordinal() != 2 ) // NOT Multiconsensus RANSAC
		{
			params.minNumInliers = 0;
		}

		final RandomAccessibleInterval<RealType> img = (RandomAccessibleInterval)ImagePlusImgs.from( imp );

		// dirty cast that can't be avoided :(
		if ( params.autoMinMax )
		{
			double[] minmax = HelperFunctions.computeMinMax(img);

			params.min = (float) minmax[0];
			params.max = (float) minmax[1];
			params.autoMinMax = false; // have done it already
		}
		else
		{
			GenericDialogPlus gd2 = new GenericDialogPlus( "Image Min/Max" );
			gd2.addNumericField( "Image_min", RadialSymParams.defaultMin );
			gd2.addNumericField( "Image_max", RadialSymParams.defaultMax );

			gd2.showDialog();
			if (gd2.wasCanceled() )
				return;

			params.min = RadialSymParams.defaultMin = gd2.getNextNumber();
			params.max = RadialSymParams.defaultMax = gd2.getNextNumber();
		}

		// TODO: REMOVE
		//mode = 1;
		//RadialSymParams.defaultSigma = 2.0f;
		//RadialSymParams.defaultThreshold =  0.02f;
		//params.setAnisotropyCoefficient( 0.675 );

		final boolean useMultithreading;
		final int numThreads;
		final int[] blockSize3d;

		if ( mode == 1) {// advanced

			GenericDialogPlus gd2 = new GenericDialogPlus( "Advanced Options" );

			gd2.addNumericField( "Sigma (DoG)", RadialSymParams.defaultSigma, 5, 15, "" );
			gd2.addNumericField( "Threshold (DoG)", RadialSymParams.defaultThreshold, 5, 15, "" );
			gd2.addNumericField( "Support region radius (RANSAC)", RadialSymParams.defaultSupportRadius, 0 );
			gd2.addNumericField( "Min_inlier_ratio (RANSAC)", RadialSymParams.defaultInlierRatio, 2 );
			gd2.addNumericField( "Max_error (RANSAC)", RadialSymParams.defaultMaxError, 2 );
			gd2.addNumericField( "Spot_intensity_threshold", RadialSymParams.defaultIntensityThreshold, 2 );

			gd2.addMessage( "" );
			gd2.addChoice( "Background subtraction", RadialSymParams.bsMethods, RadialSymParams.bsMethods[ RadialSymParams.defaultBsMethodChoice ]);
			gd2.addNumericField( "Background_subtraction_max_error", RadialSymParams.defaultBsMaxError, 2 );
			gd2.addNumericField( "Background_subtraction_min_inlier_ratio", RadialSymParams.defaultBsInlierRatio, 2 );

			gd2.addMessage( "" );
			gd2.addFileField( "Results_file", RadialSymParams.defaultResultsFilePath );

			gd2.addMessage( "" );
			gd2.addMessage( "Multi-Threading:", new Font( "Default", Font.BOLD, 13 ) );
			gd2.addMessage( "(Warning: if using RANSAC, results might slightly change\nfrom run to run due to inherent randomness)", new Font( "Default", Font.ITALIC, 11 ) );
			gd2.addCheckbox( "Use_multithreading", defaultUseMultithreading );
			gd2.addNumericField( "Num_threads", defaultNumThreads, 0 );
			gd2.addNumericField( "Block_size_X", defaultBlockSize[ 0 ], 0 );
			gd2.addNumericField( "Block_size_Y", defaultBlockSize[ 1 ], 0 );
			gd2.addNumericField( "Block_size_Z", defaultBlockSize[ 2 ], 0 );

			gd2.showDialog();
			if ( gd2.wasCanceled() )
				return;

			params.sigma = RadialSymParams.defaultSigma = (float)gd2.getNextNumber();
			params.threshold = RadialSymParams.defaultThreshold = (float)gd2.getNextNumber();
			params.supportRadius = RadialSymParams.defaultSupportRadius = Math.round( (float)gd2.getNextNumber() );
			params.inlierRatio = RadialSymParams.defaultInlierRatio = (float)gd2.getNextNumber();
			params.maxError = RadialSymParams.defaultMaxError = (float)gd2.getNextNumber();
			params.intensityThreshold = RadialSymParams.defaultIntensityThreshold = gd2.getNextNumber();

			params.bsMethod = RadialSymParams.defaultBsMethodChoice = gd2.getNextChoiceIndex();
			params.bsMaxError = RadialSymParams.defaultBsMaxError = (float)gd2.getNextNumber();
			params.bsInlierRatio = RadialSymParams.defaultBsInlierRatio = (float)gd2.getNextNumber();
			params.resultsFilePath = RadialSymParams.defaultResultsFilePath = gd2.getNextString().trim();
			useMultithreading = defaultUseMultithreading = gd2.getNextBoolean();
			numThreads = defaultNumThreads = (int)Math.round( gd2.getNextNumber() );
			blockSize3d = new int[ defaultBlockSize.length ];
			blockSize3d[ 0 ] = defaultBlockSize[ 0 ] = (int)Math.round( gd2.getNextNumber() );
			blockSize3d[ 1 ] = defaultBlockSize[ 1 ] = (int)Math.round( gd2.getNextNumber() );
			blockSize3d[ 2 ] = defaultBlockSize[ 2 ] = (int)Math.round( gd2.getNextNumber() );
		}
		else // interactive
		{
			useMultithreading = false;
			numThreads = -1;
			blockSize3d = null;

			HelperFunctions.log( "img min intensity=" + params.min + ", max intensity=" + params.max );

			InteractiveRadialSymmetry irs = new InteractiveRadialSymmetry(imp, params, params.min, params.max);
			do {
				// TODO: change to something that is not deprecated
				SimpleMultiThreading.threadWait(100);
			} while (!irs.isFinished());

			if (irs.wasCanceled())
				return;

			// update defaults with selections from the interactive GUI
			params.setDefaultValuesFromInteractive();
		}

		if ( params.RANSAC().ordinal() == 2 ) // Multiconsensus RANSAC
		{
			GenericDialogPlus gd2 = new GenericDialogPlus( "Multiconsensus RANSAC Options" );
			gd2.addNumericField( "Min_number_of_inliers", RadialSymParams.defaultMinNumInliers, 0 );
			gd2.addNumericField( "Initial #inlier threshold for new spot (avg - n*stdev) n=", RadialSymParams.defaultNTimesStDev1, 2 );
			gd2.addNumericField( "Final #inlier threshold for new spot (avg - n*stdev) n=", RadialSymParams.defaultNTimesStDev2, 2 );

			gd2.showDialog();
			if ( gd2.wasCanceled() )
				return;

			params.minNumInliers = RadialSymParams.defaultMinNumInliers = (int)Math.round( gd2.getNextNumber() );
			params.nTimesStDev1 = RadialSymParams.defaultNTimesStDev1 = gd2.getNextNumber();
			params.nTimesStDev2 = RadialSymParams.defaultNTimesStDev2 = gd2.getNextNumber();
		}

		int[] impDim = imp.getDimensions(); // x y c z t

		if ( !useMultithreading )
		{
			final long time = System.currentTimeMillis();

			runRSFISH(
					Views.extendMirrorSingle( img ),
					new FinalInterval( img ),
					params,
					mode,
					imp,
					impDim );

			if ( mode == 1 )
				HelperFunctions.log( "Compute time = " + (System.currentTimeMillis() - time ) + " msec." );
		}
		else
		{
			params.printParams();

			final long time = System.currentTimeMillis();

			final int[] blockSize;
			if ( img.numDimensions() == 3 )
				blockSize = blockSize3d;
			else
				blockSize = new int[] { blockSize3d[ 0 ], blockSize3d[ 1 ] };

			for ( int d = 0; d < blockSize.length; ++d )
			{
				if ( blockSize[ d ] < 3 )
				{
					HelperFunctions.log( "ERROR: Blocksize is too small, needs to be at least 3 -- even though this would be very inefficient.");
					return;
				}

				if ( blockSize[ d ] < 16 )
				{
					HelperFunctions.log( "WARNING: You chose a small blockSize[" + d +"]=" + blockSize[ d ]);
				}
			}

			// only 2 pixel overlap necessary to find local max/min to start - we then anyways load the full underlying image for each block
			final ArrayList< Block > blocks = Block.splitIntoBlocks( new FinalInterval( img ), blockSize );

			HelperFunctions.log( "Using multithreading ... num threads = " + numThreads + ", num blocks = " + blocks.size() + ", block size = " + Util.printCoordinates( blockSize )  );

			params.numThreads = 1; // DoG numThreads is now 1
			HelperFunctions.headless = true;

			final List< double[] > allPoints = new ArrayList<>();
			final List< Callable< List< double[] > > > tasks = new ArrayList<>();
			final AtomicInteger nextBlock = new AtomicInteger();

			for ( int threadNum = 0; threadNum < numThreads; ++threadNum )
			{
				tasks.add( () ->
				{
					final List< double[] > points = new ArrayList<>();

					for ( int b = nextBlock.getAndIncrement(); b < blocks.size(); b = nextBlock.getAndIncrement() )
					{
						final Block block = blocks.get( b );

						final ArrayList<double[]> pointsLocal = Radial_Symmetry.runRSFISH(
								(RandomAccessible)(Object)Views.extendMirrorSingle( img ),
								new FinalInterval( img ),
								block.createInterval(),
								params );

						HelperFunctions.log( "block " + block.id() + " found " + pointsLocal.size() + " spots.");

						points.addAll( pointsLocal );
					}

					return points;
				});
			}

			final ExecutorService service = Executors.newFixedThreadPool( numThreads );

			try
			{
				final List< Future< List< double[] > > > futures = service.invokeAll( tasks );
				for ( final Future< List< double[] > > future : futures )
					allPoints.addAll( future.get() );
			}
			catch ( final InterruptedException | ExecutionException e )
			{
				e.printStackTrace();
				throw new RuntimeException( e );
			}

			service.shutdown();

			HelperFunctions.headless = false;

			HelperFunctions.log( "Compute time = " + (System.currentTimeMillis() - time ) + " msec." );

			HelperFunctions.log( "Found " + allPoints.size() + " spots in total." );

			lastRt = ShowResult.ransacResultTable(allPoints);
			lastRt.show( "smFISH localizations" );

			if ( params.resultsFilePath != null && params.resultsFilePath.length() > 0 )
			{
				HelperFunctions.log( "Writing result to " + params.resultsFilePath );
				Block.writeCSV( allPoints, params.resultsFilePath );
			}
		}
	}

	public static < T extends RealType< T > > ArrayList<double[]> runRSFISH(
			final RandomAccessible< T > img,
			final Interval globalInterval, // we need to know where to cut off gradients at image borders
			final Interval computeInterval,
			final RadialSymParams params )
	{
		if ( img.numDimensions() < 2 || img.numDimensions() > 3 )
			throw new RuntimeException( "Only dimensionality of 2 or 3 is supported right now, here we have: " + img.numDimensions() );

		final int[] impDim = new int[ 5 ]; // x y c z t

		impDim[ 0 ] = (int)computeInterval.dimension( 0 );
		impDim[ 1 ] = (int)computeInterval.dimension( 1 );
		impDim[ 2 ] = 1;
		impDim[ 3 ] = computeInterval.numDimensions() > 2 ? (int)computeInterval.dimension( 2 ) : 1;
		impDim[ 4 ] = 1;

		return runRSFISH( img, globalInterval, computeInterval, params, 1, null, impDim);
	}

	public static < T extends RealType< T > > ArrayList<double[]> runRSFISH(
			final RandomAccessible< T > img,
			final Interval interval,
			final RadialSymParams params,
			final int mode,
			final ImagePlus imp,
			final int[] impDim )
	{
		return runRSFISH(img, interval, interval, params, mode, imp, impDim);
	}

	public static < T extends RealType< T > > ArrayList<double[]> runRSFISH(
			final RandomAccessible< T > img,
			final Interval globalInterval, // we need to know where to cut off gradients at image borders
			final Interval computeInterval,
			final RadialSymParams params,
			final int mode,
			final ImagePlus imp,
			final int[] impDim )
	{
		params.printParams( false );

		if ( params.autoMinMax )
		{
			double[] minmax = HelperFunctions.computeMinMax( Views.interval( img, computeInterval ) );

			if (Double.isNaN( params.min ) )
				params.min = (float) minmax[0];

			if (Double.isNaN( params.max ) )
				params.max = (float) minmax[1];
		}

		HelperFunctions.log( "img min intensity=" + params.min + ", max intensity=" + params.max );

		ArrayList<Spot> allSpots = new ArrayList<>(0);
		// stores number of detected spots per time point
		ArrayList<Long> timePoint = new ArrayList<>(0);
		// stores number of detected spots per channel
		ArrayList<Long> channelPoint = new ArrayList<>(0);

		// un-normalized image for intensity measurement
		final RandomAccessible<FloatType> input = Converters.convert(
				img,
				(a, b) -> b.setReal(a.getRealFloat()),
				new FloatType());

		// normalized image for detection
		final double range = params.max - params.min;

		final RandomAccessible<FloatType> rai = Converters.convert(
				img,
				(a, b) -> b.setReal( ( a.getRealFloat() - params.min ) / range ),
				new FloatType());

		RadialSymmetry.process(input, rai, globalInterval, computeInterval, params, impDim, allSpots, timePoint, channelPoint);

		ResultsTable rt = null;

		if ( mode == 0 ) { // interactive
			imp.deleteRoi();

			// shows the histogram and sets the intensity threshold
			params.intensityThreshold = RadialSymParams.defaultIntensityThreshold = 
					Visualization.visuallyDefineThreshold(
							imp, allSpots, timePoint,
							params.getSigmaDoG(), params.getAnisotropyCoefficient());

			rt = lastRt = ShowResult.ransacResultTable(allSpots, timePoint, channelPoint, params.intensityThreshold );
		}
		else if ( mode == 1 ) { // advanced
			// write the result to the csv file
			if ( HelperFunctions.headless && params.resultsFilePath.length() > 0 )
				ShowResult.ransacResultCsv(allSpots, timePoint, channelPoint, params.intensityThreshold, params.resultsFilePath );

			if ( !HelperFunctions.headless )
				rt = lastRt = ShowResult.ransacResultTable(allSpots, timePoint, channelPoint, params.intensityThreshold );
		}
		else
		{
			throw new RuntimeException("Wrong parameters' mode");
		}

		HelperFunctions.log("intensityThreshold     : " + params.intensityThreshold);

		if ( !HelperFunctions.headless )
		{
			rt.show( "smFISH localizations");

			if ( params.resultsFilePath.length() > 0 )
			{
				System.out.println("Writing CSV: " + params.resultsFilePath);
				rt.save(params.resultsFilePath);
			}
			else
			{
				System.out.println("No CSV output given.");
			}
		}

		if ( params.addToROIManager )
		{
			HelperFunctions.log( "Adding spots to ROI Manager" );
			RoiManager roim = RoiManager.getRoiManager();
			imp.setActivated();

			for ( final Spot spot : allSpots )
			{
				if (spot.getIntensity() >= params.intensityThreshold) {
					PointRoi p = new PointRoi( spot.getDoublePosition( 0 ), spot.getDoublePosition( 1 ) );
					imp.setSliceWithoutUpdate( 1 + (int)Math.round( spot.getDoublePosition( 2 ) ) );
					p.setPosition( 1 + (int)Math.round( spot.getDoublePosition( 2 ) ) );
					imp.unlock();
					roim.addRoi( p );
				}
			}
		}

		return ShowResult.points(allSpots, params.intensityThreshold );
	}

	public static void main(String[] args) {
		//net.imagej.ImageJ ij = new net.imagej.ImageJ();
		//ij.launch( "/Users/spreibi/Downloads/N2_267-1.tif" );
		//ij.launch( "/Users/spreibi/Downloads/C0-N2_352_cropped_1240.tif" );
		//ij.launch( "/home/kharrington/Data/Radial_Symmetry/N2_352-1.tif" );

		//ij.launch( "/Users/spreibi/Documents/BIMSB/Publications/radialsymmetry/Poiss_30spots_bg_200_0_I_10000_0_img0.tif");
		//ij.launch( "/Users/spreibi/Documents/BIMSB/Publications/radialsymmetry/Poiss_30spots_bg_200_1_I_300_0_img0.tif");
		//ij.launch( "/Users/spreibi/Documents/BIMSB/Publications/radialsymmetry/Poiss_300spots_bg_200_0_I_10000_0_img0.tif" );

		//ij.command().run(Radial_Symmetry.class, true);

		new ImageJ();
		//ImagePlus imp = new ImagePlus("/Users/spreibi/Documents/BIMSB/Publications/radialsymmetry/Poiss_30spots_bg_200_1_I_300_0_img0.tif" );
		//ImagePlus imp = new ImagePlus( "/Users/spreibi/Downloads/C0-N2_352_cropped_1240.tif" );
		ImagePlus imp = new ImagePlus( "/Users/preibischs/Documents/BIMSB/Publications/radialsymmetry/N2_702_cropped_1620 (high SNR)_ch0.tif");

		imp.show();
		imp.setSlice( imp.getStackSize() / 2 );

		new Radial_Symmetry().run( null );
	}
}
