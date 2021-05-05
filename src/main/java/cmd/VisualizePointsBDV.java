package cmd;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.janelia.saalfeldlab.n5.N5FSReader;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;

import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.util.RandomAccessibleIntervalMipmapSource;
import bdv.util.volatiles.SharedQueue;
import bdv.util.volatiles.VolatileViews;
import bdv.viewer.Source;
import gui.csv.overlay.CsvOverlay;
import ij.ImagePlus;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.RealPointSampleList;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.imageplus.ImagePlusImgs;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import util.st.filter.GaussianFilterFactory;
import util.st.filter.GaussianFilterFactory.WeightType;
import util.st.render.Render;

public class VisualizePointsBDV implements Callable<Void> {

	// TODO: as plugin
	@Option(names = {"-i", "--image"}, required = true, description = "input image or N5 container path, e.g. -i /home/smFish.tif or /home/smFish.n5")
	private String image = null;

	@Option(names = {"-d", "--dataset"}, required = false, description = "if you selected an N5 path, you need to define the dataset within the N5, e.g. -d 'embryo_5_ch0/c0'")
	private String dataset = null;

	@Option(names = {"-c", "--csvFile"}, required = true, description = "the csv file stored from RS-FISH'")
	private String csvFile = null;

	@Option(names = {"-s", "--sigma"}, required = false, description = "the sigma for each rendered point (default: 1.0)")
	private double sigma = 1.0;

	@Option(names = {"--calibration"}, required = false, description = "calibration for the image as comma-separated list of doubles (default: 1.0,1.0,...)")
	private String calibration = null;

	@Option(names = {"--noPointScaling"}, required = false, description = "use this option to apply calibration only to the image, but not to the points")
	private boolean noPointScaling = false;

	public static Source<?> openMultiRes(
			final String n5Path,
			final String n5Group,
			final List< Double > calibration ) throws IOException {

		final N5Reader n5 = new N5FSReader(n5Path);
		final String group = n5Group;

		final SharedQueue queue = new SharedQueue(Math.min(4, Math.max(1, Runtime.getRuntime().availableProcessors() / 2)));

		final int numScales = n5.list(group).length;

		final RandomAccessibleInterval[] mipmaps = new RandomAccessibleInterval[numScales];
		final double[][] scales = new double[numScales][];

		for (int s = 0; s < numScales; ++s) {

			final int scale = 1 << s;

			final RandomAccessibleInterval<UnsignedByteType> source = N5Utils.openVolatile(n5, group + "/s" + s);

			mipmaps[s] = source;
			if ( n5.listAttributes( group + "/s" + s ).containsKey( "downsamplingFactors" ) )
				scales[ s ] = n5.getAttribute(group + "/s" + s, "downsamplingFactors", double[].class );
			else
				scales[s] = new double[]{scale, scale, scale};

			System.out.println( "Scale /s" + s + ": " + Util.printCoordinates( scales[ s ] ) + ", dim=" + Util.printCoordinates( Intervals.dimensionsAsLongArray( mipmaps[ s ] ) ) );
		}

		final double[] cal = new double[ mipmaps[ 0 ].numDimensions() ];

		for ( int d = 0; d < cal.length; ++d )
		{
			if ( calibration == null || calibration.size() == 0 )
				cal[ d ] = 1.0;
			else
				cal[ d ] = calibration.get( d );
		}

		System.out.println( Util.printCoordinates( cal ));

		final AffineTransform3D t = new AffineTransform3D();
		t.scale( cal[ 0 ], cal[ 1 ], cal.length >= 3 ? cal[ 2 ] : 1.0 );

		final RandomAccessibleIntervalMipmapSource<?> mipmapSource =
				new RandomAccessibleIntervalMipmapSource<>(
						mipmaps,
						 (NumericType)Views.iterable( mipmaps[ 0 ] ).firstElement(),
						scales,
						new FinalVoxelDimensions("px", cal ),
						t,
						group);

		final Source<?> volatileMipmapSource = mipmapSource.asVolatile(queue);

		return volatileMipmapSource;
		//bdv = Show.mipmapSource(volatileMipmapSource, bdv, bdvOptions);
		//bdv.getBdvHandle().getViewerPanel().getTopLevelAncestor().setSize(1280 - 32, 720 - 48 - 16);
		//return bdv;
	}

	public static RealRandomAccessible< DoubleType > renderPoints( final Collection< ? extends RealLocalizable > points, final double sigma )
	{
		RealPointSampleList< UnsignedByteType > list = new RealPointSampleList<UnsignedByteType>( points.iterator().next().numDimensions() );

		for ( final RealLocalizable p : points )
			list.add( new RealPoint( p ), new UnsignedByteType( 255 ) );

		return Render.render( list, new GaussianFilterFactory<>( new DoubleType(), sigma, WeightType.NONE ) );
	}

	public static RandomAccessibleInterval open( String image, String dataset ) throws IOException
	{
		RandomAccessibleInterval img;

		if ( dataset != null && RadialSymmetry.isN5( image ) )
		{
			if ( dataset == null || dataset.length() < 1 )
				throw new RuntimeException( "no dataset for the N5 container defined, please use -d 'dataset'." );

			final N5Reader n5 = new N5FSReader( image );
			img = VolatileViews.wrapAsVolatile( (RandomAccessibleInterval)N5Utils.openVolatile( n5, dataset ) );
		}
		else
		{
			img = ImagePlusImgs.from( new ImagePlus( image ) );
		}

		return img;
	}

	@Override
	public Void call() throws Exception {

		//csvFile = "/Users/spreibi/Documents/BIMSB/Publications/radialsymmetry/EASI-FISH/R7_LHA5/c0/merged_points_c0.txt";
		//image = "/Volumes/multifish/Yuhan/LHA5/stitch/R7_LHA5/export.n5/";
		//dataset = "c0";
		//sigma = 2.0;
		//calibration = "0.23,0.23,0.42";
		//noPointScaling = true;

		// -i '/Users/spreibi/Documents/BIMSB/Publications/radialsymmetry/test.n5'
		// -d 'N2-702-ch0/c0'
		// N2_702_cropped_1620 (high SNR)_ch0.tif

		
		List< Double > cal = null;

		if ( calibration != null )
		{
			cal = Arrays.asList( calibration.split( "," ) ).stream().map( s -> Double.parseDouble( s ) ).collect( Collectors.toList() );
			sigma *= cal.get( 0 );
		}

		BdvStackSource<?> bdv = null;

		if ( dataset != null && RadialSymmetry.isN5( image ) )
		{
			try
			{
				bdv = BdvFunctions.show( openMultiRes( image, dataset, cal ), new BdvOptions().numRenderingThreads( Runtime.getRuntime().availableProcessors() ) );
			}
			catch (Exception e )
			{
				System.out.println( "Could not open as multi-resolution N5 (if you want to do so please do not specify s0, s1, ... but only the parent folder. " );
			}
		}

		if ( bdv == null )
		{
			System.out.println( "Opening: " + ( dataset == null ? new File( image ) : new File( image, dataset ) ) );
			RandomAccessibleInterval img = open( image, dataset );

			final double[] calArray = getCalibration( cal, img.numDimensions() );
			System.out.println( "Calibration: " + Util.printCoordinates( calArray ) );

			bdv = BdvFunctions.show( img, new File( image ).getName(), new BdvOptions().numRenderingThreads( Runtime.getRuntime().availableProcessors() ).sourceTransform( calArray ) );
		}

		if ( ! new File( csvFile ).exists() )
			throw new RuntimeException( "csvFile does not exist: " + csvFile );

		final ArrayList<RealPoint> peaks = CsvOverlay.readAndSortPositionsFromCsv( new File( csvFile ) );

		System.out.println( "initializing point drawing ... " );

		BdvOptions options = new BdvOptions().numRenderingThreads( Runtime.getRuntime().availableProcessors() );

		if ( noPointScaling )
			options = options.sourceTransform( Util.getArrayFromValue( 1.0, peaks.iterator().next().numDimensions() ) );
		else
			options = options.sourceTransform( getCalibration( cal, peaks.iterator().next().numDimensions() ) );

		bdv = BdvFunctions.show( renderPoints( peaks, sigma ), Intervals.createMinMax( 0, 0, 0, 1, 1, 1), "detections", options.addTo( bdv ) );
		bdv.setColor(new ARGBType( ARGBType.rgba(0, 255, 0, 0) ) );
		bdv.setDisplayRange( 0, 256 );

		System.out.println( "done" );

		return null;
	}

	public static double[] getCalibration( final List< Double > cal, final int numDimensions )
	{
		final double[] calArray = new double[ numDimensions ];

		for ( int d = 0; d < calArray.length; ++d )
		{
			if ( cal == null || cal.size() == 0 )
				calArray[ d ] = 1.0;
			else
				calArray[ d ] = cal.get( d );
		}

		return calArray;
	}

	public static final void main(final String... args) {
		new CommandLine( new VisualizePointsBDV() ).execute( args );
	}
}
