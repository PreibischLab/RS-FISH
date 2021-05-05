package gui;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import cmd.VisualizePointsBDV;
import fiji.util.gui.GenericDialogPlus;
import fitting.Spot;
import gui.csv.overlay.CsvOverlay;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.img.imageplus.ImagePlusImgs;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Pair;
import net.imglib2.util.Util;
import net.imglib2.util.ValuePair;
import parameters.RadialSymParams;

public class CsvOverlay_Bdv implements PlugIn {

	public static String[] options = new String[] {
			"Display processed image and points in ResultTable",
			"Display a currently open image and points in ResultTable",
			"Display a currently open image and load points from CSV",
			"Display a N5 dataset (virtually) and load points from CSV" };

	public static int defaultOption = 0;

	public static double defaultSigma = 1.0;
	public static String defaultN5Path = "";
	public static String defaultN5Dataset = "";
	public static String defaultPath = "";

	@Override
	public void run(String arg)
	{
		final GenericDialog gd = new GenericDialog( "Display overlaid points with BDV" );

		gd.addChoice( "Options", options, options[ defaultOption ] );
		gd.addSlider( "Sigma (points)", 0.5, 5, defaultSigma );

		gd.showDialog();

		if ( gd.wasCanceled() )
			return;

		final int choice = defaultOption = gd.getNextChoiceIndex();
		final double sigma = defaultSigma = gd.getNextNumber();

		BdvStackSource<?> bdv = null;

		if ( choice == 0 )
		{
			// 1: use a currently open image and the detections currently shown in the Resultstable

			if ( Radial_Symmetry.lastImp == null )
			{
				IJ.log( "No image that RS-FISH was run on is known." );
				return;
			}

			if ( Radial_Symmetry.lastRt == null )
			{
				IJ.log( "No ResultTable that RS-FISH was run on is known." );
				return;
			}

			RandomAccessibleInterval img = ImagePlusImgs.from( Radial_Symmetry.lastImp );
			bdv = BdvFunctions.show( img, Radial_Symmetry.lastImp.getTitle(), new BdvOptions().numRenderingThreads( Runtime.getRuntime().availableProcessors() ) );

			bdv = BdvFunctions.show( VisualizePointsBDV.renderPoints( getPoints( Radial_Symmetry.lastRt ), sigma ), Intervals.createMinMax( 0, 0, 0, 1, 1, 1), "detections", new BdvOptions().numRenderingThreads( Runtime.getRuntime().availableProcessors() ).addTo( bdv ) );
			bdv.setColor(new ARGBType( ARGBType.rgba(0, 255, 0, 0) ) );
			bdv.setDisplayRange( 0, 256 );
		}
		else if ( choice == 1 )
		{
			// 2: use a currently open image and load a CSV
			Pair<ImagePlus, String> v = getImagePlus( false );

			RandomAccessibleInterval img = ImagePlusImgs.from( v.getA() );
			bdv = BdvFunctions.show( img, v.getA().getTitle(), new BdvOptions().numRenderingThreads( Runtime.getRuntime().availableProcessors() ) );

			bdv = BdvFunctions.show( VisualizePointsBDV.renderPoints( getPoints( Radial_Symmetry.lastRt ), sigma ), Intervals.createMinMax( 0, 0, 0, 1, 1, 1), "detections", new BdvOptions().numRenderingThreads( Runtime.getRuntime().availableProcessors() ).addTo( bdv ) );
			bdv.setColor(new ARGBType( ARGBType.rgba(0, 255, 0, 0) ) );
			bdv.setDisplayRange( 0, 256 );
		}
		else if ( choice == 2 )
		{
			// 3:Display a currently open image and load points from CSV
			Pair<ImagePlus, String> v = getImagePlus( true );

			RandomAccessibleInterval img = ImagePlusImgs.from( v.getA() );
			bdv = BdvFunctions.show( img, v.getA().getTitle(), new BdvOptions().numRenderingThreads( Runtime.getRuntime().availableProcessors() ) );

			final ArrayList<RealPoint> peaks = CsvOverlay.readAndSortPositionsFromCsv( new File( v.getB() ) );
			bdv = BdvFunctions.show( VisualizePointsBDV.renderPoints( peaks, sigma ), Intervals.createMinMax( 0, 0, 0, 1, 1, 1), "detections", new BdvOptions().numRenderingThreads( Runtime.getRuntime().availableProcessors() ).addTo( bdv ) );
			bdv.setColor(new ARGBType( ARGBType.rgba(0, 255, 0, 0) ) );
			bdv.setDisplayRange( 0, 256 );
		}
		else
		{
			// 4: load (virtually) from disc and load a CSV
			final GenericDialogPlus gd2 = new GenericDialogPlus( "Select N5 dataset and CSV" );

			gd2.addDirectoryOrFileField( "N5_folder", defaultN5Path, 70 );
			gd2.addDirectoryOrFileField( "N5_dataset", defaultN5Dataset, 70 );
			gd2.addDirectoryOrFileField( "Select CSV", defaultPath, 70 );
			gd2.addMessage("");
			gd2.addMessage("NOTE: THIS REQUIRES THE N5 UPDATE SITE TO BE ACTIVE.");

			gd2.showDialog();

			if (gd2.wasCanceled() )
				return;

			final String image = defaultN5Path = gd2.getNextString();
			final String dataset = defaultN5Dataset = gd2.getNextString();
			final String csv = defaultPath = gd2.getNextString();

			IJ.log( "Loading N5: " + image + ":/" + dataset );

			try
			{
				bdv = BdvFunctions.show( VisualizePointsBDV.openMultiRes( image, dataset, null ), new BdvOptions().numRenderingThreads( Runtime.getRuntime().availableProcessors() ) );
			}
			catch (Exception e )
			{
				IJ.log( "Could not open as multi-resolution N5 (if you want to do so please do not specify s0, s1, ... but only the parent folder. " );
				IJ.log( e.getMessage() );
				//e.printStackTrace();
			}

			if ( bdv == null )
			{
				System.out.println( "Opening: " + new File( image, dataset ) );
				RandomAccessibleInterval img = null;
				try
				{
					img = VisualizePointsBDV.open( image, dataset );
				}
				catch (IOException e) {
					IJ.log( "Failed to open N5: " + e.getMessage() );
					//e.printStackTrace();
					return;
				}

				bdv = BdvFunctions.show( img, new File( image ).getName(), new BdvOptions().numRenderingThreads( Runtime.getRuntime().availableProcessors() ) );
			}

			final ArrayList<RealPoint> peaks = CsvOverlay.readAndSortPositionsFromCsv( new File( csv ) );
			bdv = BdvFunctions.show( VisualizePointsBDV.renderPoints( peaks, sigma ), Intervals.createMinMax( 0, 0, 0, 1, 1, 1), "detections", new BdvOptions().numRenderingThreads( Runtime.getRuntime().availableProcessors() ).addTo( bdv ) );
			bdv.setColor(new ARGBType( ARGBType.rgba(0, 255, 0, 0) ) );
			bdv.setDisplayRange( 0, 256 );
		}
	}

	public static ArrayList< RealPoint > getPoints( final ResultsTable rt )
	{
		String[] headers = rt.getHeadings();
		final int numDimensions;

		if ( headers[ 2 ].equals( "z" ) )
			numDimensions = 3;
		else
			numDimensions = 2;

		final ArrayList< RealPoint > list = new ArrayList<>();

		final int numRows = rt.getColumn(0).length;
		for ( int i = 0; i < numRows; ++i )
		{
			if ( numDimensions == 2 )
				list.add( new RealPoint( Double.parseDouble( rt.getStringValue( 0, i ) ), Double.parseDouble( rt.getStringValue( 1, i ) ) ) );
			else
				list.add( new RealPoint( Double.parseDouble( rt.getStringValue( 0, i ) ), Double.parseDouble( rt.getStringValue( 1, i ) ), Double.parseDouble( rt.getStringValue( 2, i ) ) ) );
		}

		return list;
	}

	public static Pair<ImagePlus, String> getImagePlus( final boolean csv )
	{
		// get list of open image stacks
		final int[] idList = WindowManager.getIDList();

		if ( idList == null || idList.length == 0 )
		{
			IJ.error( "You need at least one open image." );
			return null;
		}

		// map all id's to image title for those who are 3d stacks
		final String[] imgList =
				Arrays.stream( idList ).
						mapToObj( id -> WindowManager.getImage( id ).getTitle() ).
							toArray( String[]::new );

		if ( RadialSymParams.defaultImg >= imgList.length )
			RadialSymParams.defaultImg = 0;

		GenericDialogPlus gd = (csv ? new GenericDialogPlus( "Select open image and CSV(s)" ) : new GenericDialogPlus( "Select image" ));

		gd.addChoice( "Image", imgList, imgList[ RadialSymParams.defaultImg ] );
		if ( csv )
			gd.addDirectoryOrFileField( "Select CSV", defaultPath, 70 );

		gd.showDialog();

		if (gd.wasCanceled() )
			return null;

		ImagePlus imp = WindowManager.getImage( idList[ RadialSymParams.defaultImg = gd.getNextChoiceIndex() ] );

		if ( csv )
		{
			String csvFile = defaultPath = gd.getNextString();
			return new ValuePair<>( imp, csvFile );
		}
		else
		{
			return new ValuePair<>( imp, null );
		}
	}

	public static void main( String[] args )
	{
		new ImageJ();

		new CsvOverlay_Bdv().run( null );
	}
}
