package cmd;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;

import com.opencsv.CSVReader;

import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import util.st.render.Render;

public class VisualizePointsMerfish implements Callable<Void> {

	@Option(names = {"-i", "--N5Path"}, required = true, description = "N5 container path, e.g. -i /BlendMosaic/region_0/data.n5")
	private String image = null;

	@Option(names = {"-d", "--N5dataset"}, required = true, description = "the dataset(s) within the N5, e.g. -d dapi -d polyY")
	private List<String> datasets = null;

	@Option(names = {"-g", "--gene"}, required = true, description = "the gene(s) to be displayed, e.g. -g Col25a1 -d Sema4d")
	private List<String> genes = null;

	@Option(names = {"-c", "--csvFile"}, required = true, description = "the csv file as exported by the Merfish software (e.g. ExportBarcodes/region_0/barcodes.csv)")
	private String csvFile = null;

	@Option(names = {"-gc", "--genesCsvFile"}, required = true, description = "the csv file that contains the gene names as defined by the Merfish software (e.g. codebook_0_M22a.csv)")
	private String genesCsvFile = null;

	@Option(names = {"-tc", "--transformCsvFile"}, required = true, description = "the csv file that contains the transformation matrix defined by Merfish software (e.g. BlendMosaic/region_0/micron_to_mosaic_pixel_transform.csv)")
	private String transformCsvFile = null;

	@Option(names = {"-s", "--sigma"}, required = false, description = "the sigma for each rendered point (default: 1.0)")
	private double sigma = 2.0;

	@Override
	public Void call() throws Exception {

		CSVReader reader;
		String[] nextLine;

		// load gene name CSV
		System.out.println( "all genes:" );

		final HashMap<Integer, String> idToGene = new HashMap<>();
		final HashMap<String, Integer> geneToId = new HashMap<>();

		reader = new CSVReader(new FileReader(genesCsvFile));
		nextLine = reader.readNext(); // skip header
		for ( int j = 0; (nextLine = reader.readNext()) != null; ++j )
		{
			idToGene.put( j, nextLine[ 0 ] );
			geneToId.put( nextLine[ 0 ], j );
			System.out.println( nextLine[ 0 ] + " >> " + j );
		}
		reader.close();

		// load transformation matrix
		final AffineTransform2D transform = new AffineTransform2D();
		if ( transformCsvFile != null )
		{
			reader = new CSVReader(new FileReader(transformCsvFile));
			for ( int r = 0; r < 2; ++r )
			{
				nextLine = reader.readNext();
				for ( int c = 0; c < 3; ++c )
					transform.set( Double.parseDouble( nextLine[ c ] ), r, c );
			}
			reader.close();
		}
		System.out.println( "\n2d affine transform: " + transform );

		// genes to load
		final HashMap< Integer, List<RealPoint> > peaks = new HashMap<>();

		System.out.println( "\nshowing genes:" );
		for ( final String gene : genes )
		{
			final Integer id = geneToId.get( gene );

			if ( id == null )
			{
				throw new Exception( "gene '" + gene + "' not available in this dataset. stopping.");
			}

			peaks.put( id, new ArrayList<>() );
		}



		// load coordinates
		final double[] xy = new double[ 2 ];

		reader = new CSVReader(new FileReader(csvFile)); 
		nextLine = reader.readNext(); // skip header ,barcode_id,global_x,global_y,global_z,x,y,fov
		while ( (nextLine = reader.readNext()) != null )
		{
			final int id = Integer.parseInt( nextLine[ 1 ] );

			if ( peaks.containsKey( id ) )
			{
				xy[ 0 ] = Double.parseDouble( nextLine[ 2 ]);
				xy[ 1 ] = Double.parseDouble( nextLine[ 3 ]);
				transform.apply( xy, xy );

				peaks.get( id ).add(
						new RealPoint(
								xy[ 0 ],
								xy[ 1 ],
								Double.parseDouble( nextLine[ 4 ])
								));
			}
		}

		for( final Entry<Integer, List<RealPoint>> entry : peaks.entrySet() )
			System.out.println( idToGene.get( entry.getKey() ) + ", id=" + entry.getKey() + ", numPoints=" + entry.getValue().size() );

		BdvStackSource<?> bdv = null;

		// load images
		for ( int i = 0; i < datasets.size(); ++i )
		{
			final String dataset = datasets.get( i );

			System.out.println( "Opening: " + new File( image, dataset ) );

			try
			{
				bdv = BdvFunctions.show( VisualizePointsBDV.openMultiRes( image, dataset ), new BdvOptions().numRenderingThreads( Runtime.getRuntime().availableProcessors() ).addTo( bdv ) );
			}
			catch (Exception e )
			{
				System.out.println( "Could not open as multi-resolution N5 (if you want to do so please do not specify s0, s1, ... but only the parent folder. " );
			}
	
			if ( bdv == null )
			{
				RandomAccessibleInterval img = VisualizePointsBDV.open( image, dataset );
	
				final double[] calArray = VisualizePointsBDV.getCalibration( null, img.numDimensions() );
				System.out.println( "Calibration: " + Util.printCoordinates( calArray ) );
	
				bdv = BdvFunctions.show( img, new File( image ).getName(), new BdvOptions().numRenderingThreads( Runtime.getRuntime().availableProcessors() ).sourceTransform( calArray ).addTo( bdv ) );
			}
		}

		System.out.println( "initializing point drawing ... " );

		// random gene coloring
		Random rnd = new Random( 343 );

		BdvOptions options = new BdvOptions().numRenderingThreads( Runtime.getRuntime().availableProcessors() );

		for ( final int id : peaks.keySet() )
		{
			System.out.println( "creating rra" );
			final RealRandomAccessible<DoubleType> rra = VisualizePointsBDV.renderPoints( peaks.get( id ), sigma );

			System.out.println( "displaying" );
			bdv = BdvFunctions.show( rra, Intervals.createMinMax( 0, 0, 0, 1, 1, 1), idToGene.get( id ), options.addTo( bdv ) );
	
			if ( peaks.keySet().size() == 1 )
				bdv.setColor(new ARGBType( ARGBType.rgba(0, 255, 0, 0) ) );
			else
				bdv.setColor( Render.randomColor( rnd ) );
	
			bdv.setDisplayRange( 0, 256 );
		}

		reader.close();

		System.out.println( "done." );

		return null;
	}

	public static final void main(final String... args) {
		new CommandLine( new VisualizePointsMerfish() ).execute( args );
	}
}
