package gui;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import benchmark.TextFileAccess;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.iterator.LocalizingZeroMinIntervalIterator;
import net.imglib2.util.Util;

public class Block implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 9068252106055534926L;

	final long[] min, max;
	final int id;

	public Block( final int id, final long[] min, final long[] max )
	{
		this.min = min;
		this.max = max;
		this.id = id;
	}

	public long[] min() { return min; }
	public long[] max() { return max; }
	public int id() { return id; }
	public FinalInterval createInterval()
	{
		return new FinalInterval( min, max );
	}

	public static ArrayList< Block > splitIntoBlocks( final Interval interval, final int[] blockSize )
	{
		if ( blockSize.length != interval.numDimensions() )
			throw new RuntimeException( "Mismatch between interval dimension and blockSize length." );

		final int[] numBlocks = new int[ blockSize.length ];

		final List< List< Long > > mins = new ArrayList<>();
		final List< List< Long > > maxs = new ArrayList<>();

		for ( int d = 0; d < blockSize.length; ++d )
		{
			List< Long > min = new ArrayList<>();
			List< Long > max = new ArrayList<>();

			long bs = blockSize[ d ];
			long pos = interval.min( d );

			while ( pos < interval.max( d ) - 1 )
			{
				min.add( pos );
				max.add( pos + Math.min( bs - 1, interval.max( d ) - pos ) );

				pos += bs - 2; // one overlap, starts at the max - 2 since the most outer pixels are not evaluated with DoG 
				++numBlocks[ d ];
			}

			mins.add( min );
			maxs.add( max );
		}

		final ArrayList< Block > blocks = new ArrayList<>();
		final LocalizingZeroMinIntervalIterator cursor = new LocalizingZeroMinIntervalIterator( numBlocks );
		int id = 0;

		while ( cursor.hasNext() )
		{
			cursor.fwd();

			final long[] min = new long[ blockSize.length ];
			final long[] max = new long[ blockSize.length ];

			for ( int d = 0; d < blockSize.length; ++d )
			{
				min[ d ] = mins.get( d ).get( cursor.getIntPosition( d ) );
				max[ d ] = maxs.get( d ).get( cursor.getIntPosition( d ) );
			}

			blocks.add( new Block(id++, min, max) );
		}

		return blocks;
	}

	public static void writeCSV( final List<double[]> points, final String file )
	{
		PrintWriter out = TextFileAccess.openFileWrite( file );

		if ( points.get( 0 ).length == 4 )
			out.println("x,y,z,t,c,intensity");
		else
			out.println("x,y,t,c,intensity");

		for (double[] spot : points) {
			for (int d = 0; d < spot.length - 1; ++d)
				out.print( String.format(java.util.Locale.US, "%.4f", spot[ d ] ) + "," );

			out.print( "1,1," );

			out.println(String.format(java.util.Locale.US, "%.4f", spot[ spot.length - 1 ] ) );
		}

		System.out.println(points.size() + " spots written to " + file );
		out.close();
	}

	public static void main( String[] args )
	{
		ArrayList< Block > blocks = splitIntoBlocks( new FinalInterval( new long[] { 19, -5 }, new long[] { 1000, 100 } ), new int[] { 100, 100 } );

		for ( final Block b : blocks )
			System.out.println( Util.printInterval( b.createInterval() ) );
	}
}
