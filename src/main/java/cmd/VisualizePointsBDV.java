package cmd;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;

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
import net.imglib2.multithreading.SimpleMultiThreading;
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

	// input file
	@Option(names = {"-i", "--image"}, required = true, description = "input image or N5 container path, e.g. -i /home/smFish.tif or /home/smFish.n5")
	private String image = null;

	@Option(names = {"-d", "--dataset"}, required = false, description = "if you selected an N5 path, you need to define the dataset within the N5, e.g. -d 'embryo_5_ch0/c0'")
	private String dataset = null;

	@Option(names = {"-c", "--csvFile"}, required = true, description = "the csv file stored from RS-FISH'")
	private String csvFile = null;

	@Option(names = {"-s", "--sigma"}, required = false, description = "the sigma for each rendered point (default: 1.0)")
	private double sigma = 1.0;

	public static Source<?> openMultiRes(
			final String n5Path,
			final String n5Group ) throws IOException {

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

		final RandomAccessibleIntervalMipmapSource<?> mipmapSource =
				new RandomAccessibleIntervalMipmapSource<>(
						mipmaps,
						 (NumericType)Views.iterable( mipmaps[ 0 ] ).firstElement(),
						scales,
						new FinalVoxelDimensions("px", new double[] { 1, 1, 1 } ),
						new AffineTransform3D(),
						group);

		//final BdvOptions bdvOptions = Bdv.options()./*screenScales(new double[] {1, 0.5}).*/numRenderingThreads(Math.max(3, Runtime.getRuntime().availableProcessors() / 5)).addTo( bdv );
		//final BdvOptions bdvOptions = Bdv.options().numRenderingThreads(Math.max(3, Runtime.getRuntime().availableProcessors() / 5));

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

	protected static RandomAccessibleInterval open( String image, String dataset ) throws IOException
	{
		RandomAccessibleInterval img;

		if ( RadialSymmetry.isN5( image ) )
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

		csvFile = "/Users/spreibi/Documents/BIMSB/Publications/radialsymmetry/EASI-FISH/R7_LHA5/c0/merged_points_c0.txt";
		image = "/Volumes/multifish/Yuhan/LHA5/stitch/R7_LHA5/export.n5/";
		dataset = "c0";
		sigma = 2.0;

		BdvStackSource<?> bdv;
		BdvOptions options = new BdvOptions().numRenderingThreads( Runtime.getRuntime().availableProcessors() );

		if ( RadialSymmetry.isN5( image ) )
			bdv = BdvFunctions.show( openMultiRes( image, dataset ), options );
		else
			bdv = BdvFunctions.show( open( image, dataset ), new File( image ).getName(), options );

		bdv.setDisplayRange( 0, 512 );

		if ( ! new File( csvFile ).exists() )
			throw new RuntimeException( "csvFile does not exist: " + csvFile );

		ArrayList<RealPoint> peaks = CsvOverlay.readAndSortPositionsFromCsv( new File( csvFile) );

		for ( final RealPoint p : peaks )
		{
			p.setPosition( p.getDoublePosition( 0 ) * 4.347449623547185, 0 );
			p.setPosition( p.getDoublePosition( 1 ) * 4.347449623547185, 1 );
			p.setPosition( p.getDoublePosition( 2 ) * 2.378892926071238, 2 );
		}

		double min[] = new double[ peaks.iterator().next().numDimensions() ];
		double max[] = new double[ peaks.iterator().next().numDimensions() ];

		for (int d = 0; d < min.length; ++d )
		{
			min[ d ] = Double.MAX_VALUE;
			max[ d ] = -Double.MAX_VALUE;
		}

		for ( final RealPoint p : peaks )
		{
			for (int d = 0; d < min.length; ++d )
			{
				min[ d ] = Math.min( min[ d ], p.getDoublePosition( d ) );
				max[ d ] = Math.max( max[ d ], p.getDoublePosition( d ) );
			}
		}

		System.out.println( "spots min=" + Util.printCoordinates( min ) + ", max=" + Util.printCoordinates( max ) );
		System.out.println( "initializing point drawing ... " );

		bdv = BdvFunctions.show( renderPoints( peaks, sigma ), Intervals.createMinMax( 0, 0, 0, 1, 1, 1), "detections", options.addTo( bdv ) );
		bdv.setColor(new ARGBType( ARGBType.rgba(255, 0, 0, 0) ) );
		bdv.setDisplayRange( 0, 256 );

		System.out.println( "done" );

		return null;
	}

	public static final void main(final String... args) {
		new CommandLine( new VisualizePointsBDV() ).execute( args );
	}
}
