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
package cmd;

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
import org.janelia.saalfeldlab.n5.N5FSReader;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import util.st.filter.GaussianFilterFactory;
import util.st.filter.GaussianFilterFactory.WeightType;
import util.st.render.Render;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

public class VisualizePointsBDV implements Callable<Void> {

	// TODO: as plugin
	@Option(names = {"-i", "--image"}, required = true, description = "input image or N5 container path, e.g. -i /home/smFish.tif or /home/smFish.n5")
	private String image = null;

	@Option(names = {"-d", "--dataset"}, required = false, description = "if you selected an N5 path, you need to define the dataset within the N5, e.g. -d 'embryo_5_ch0/c0'")
	private List<String> datasets = null;

	@Option(names = {"-c", "--csvFile"}, required = true, description = "the csv file stored from RS-FISH'")
	private List<String> csvFiles = null;

	@Option(names = {"-s", "--sigma"}, required = false, description = "the sigma for each rendered point (default: 1.0)")
	private double sigma = 1.0;

	@Option(names = {"--calibration"}, required = false, description = "calibration for each image/dataset as comma-separated list of doubles (default: 1.0,1.0,...)")
	private List<String> calibration = null;

	@Option(names = {"--pointScaling"}, required = false, description = "scaling applied to each csv dataset as comma-separated list of doubles (default: 1.0,1.0,...)")
	private List<String> pointScaling = null;

	@Option(names = {"--pointOffset"}, required = false, description = "offset applied to each csv dataset (after scaling) as comma-separated list of doubles (default: 1.0,1.0,...)")
	private List<String> pointOffset = null;

	public static Source<?> openMultiRes(
			final String n5Path,
			final String n5Group ) throws IOException {

		return openMultiRes(n5Path, n5Group, null );
	}

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
			if ( !new File( image ).exists() )
				throw new RuntimeException( "image '" + new File( image ) + "' does not exist." );

			img = ImagePlusImgs.from( new ImagePlus( image ) );
		}

		return img;
	}

	@Override
	public Void call() throws Exception {

		//csvFile = "/Users/spreibi/Documents/BIMSB/Publications/radialsymmetry/EASI-FISH/R7_LHA5/c0/merged_points_c0.txt";
		//image = "/nrs/multifish/Yuhan/LHA5/stitch/R7_LHA5/export.n5/";
		//dataset = "c0";
		//sigma = 2.0;
		//calibration = "0.23,0.23,0.42";
		//noPointScaling = true;

		/*
		-i '/Users/spreibi/Documents/BIMSB/Publications/radialsymmetry/test.n5'
		-d 'N2-702-ch0/c0'
		-c '/Users/spreibi/Documents/BIMSB/Publications/radialsymmetry/test.csv'
		*/

		BdvStackSource<?> bdv = null;

		if ( datasets == null ) // loading a normal image
		{
			datasets = new ArrayList<>();
			datasets.add( null );
		}

		for ( int i = 0; i < datasets.size(); ++i )
		{
			final String dataset = datasets.get( i );

			List< Double > cal = null;

			if ( calibration != null && i < calibration.size() )
				cal = Arrays.asList( calibration.get( i ).split( "," ) ).stream().map( s -> Double.parseDouble( s ) ).collect( Collectors.toList() );

			System.out.println( "Opening: " + ( dataset == null ? new File( image ) : new File( image, dataset ) ) );

			if ( dataset != null && RadialSymmetry.isN5( image ) )
			{
				try
				{
					bdv = BdvFunctions.show( openMultiRes( image, dataset, cal ), new BdvOptions().numRenderingThreads( Runtime.getRuntime().availableProcessors() ).addTo( bdv ) );
				}
				catch (Exception e )
				{
					System.out.println( "Could not open as multi-resolution N5 (if you want to do so please do not specify s0, s1, ... but only the parent folder. " );
				}
			}
	
			if ( bdv == null )
			{
				RandomAccessibleInterval img = open( image, dataset );
	
				final double[] calArray = getCalibration( cal, img.numDimensions() );
				System.out.println( "Calibration: " + Util.printCoordinates( calArray ) );
	
				bdv = BdvFunctions.show( img, new File( image ).getName(), new BdvOptions().numRenderingThreads( Runtime.getRuntime().availableProcessors() ).sourceTransform( calArray ).addTo( bdv ) );
			}
		}

		// random gene coloring
		Random rnd = new Random( 343 );

		for ( int i = 0; i < csvFiles.size(); ++i )
		{
			final String csvFile = csvFiles.get( i );

			System.out.println( "Loading points from: " + csvFile );

			if ( ! new File( csvFile ).exists() )
				throw new RuntimeException( "csvFile does not exist: " + csvFile );

			List< Double > scaling = null;
			List< Double > offset = null;

			if ( pointScaling != null && i < pointScaling.size() )
				scaling = Arrays.asList( pointScaling.get( i ).split( "," ) ).stream().map( s -> Double.parseDouble( s ) ).collect( Collectors.toList() );

			if ( pointOffset != null && i < pointOffset.size() )
				offset = Arrays.asList( pointOffset.get( i ).split( "," ) ).stream().map( s -> Double.parseDouble( s ) ).collect( Collectors.toList() );

			final ArrayList<RealPoint> peaks = CsvOverlay.readAndSortPositionsFromCsv( new File( csvFile ) );

			if ( scaling != null )
			{
				System.out.println( "Scaling all points with " + scaling );

				for ( final RealPoint p : peaks )
				{
					for ( int d = 0; d < scaling.size(); ++d )
					{
						p.setPosition( p.getDoublePosition( d ) * scaling.get( d ), d);
					}
				}
			}

			if ( offset != null )
			{
				System.out.println( "Adding following offset to all points " + offset );

				for ( final RealPoint p : peaks )
				{
					for ( int d = 0; d < scaling.size(); ++d )
					{
						p.setPosition( p.getDoublePosition( d ) + offset.get( d ), d);
					}
				}
			}

			System.out.println( "initializing point drawing ... " );

			BdvOptions options = new BdvOptions().numRenderingThreads( Runtime.getRuntime().availableProcessors() );
	
			bdv = BdvFunctions.show( renderPoints( peaks, sigma ), Intervals.createMinMax( 0, 0, 0, 1, 1, 1), "detections", options.addTo( bdv ) );

			if ( csvFiles.size() == 1 )
				bdv.setColor(new ARGBType( ARGBType.rgba(0, 255, 0, 0) ) );
			else
				bdv.setColor( Render.randomColor( rnd ) );

			bdv.setDisplayRange( 0, 256 );
		}

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
