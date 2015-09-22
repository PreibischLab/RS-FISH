package localmaxima;

import java.util.ArrayList;

import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

public class LocalMaximaAll extends LocalMaxima
{
	final int[] range;
	
	/**
	 * Adds a regular grid of locations
	 * 
	 * @param source - on which data
	 * @param range - every n'th pixel
	 */
	public LocalMaximaAll( final RandomAccessibleInterval<FloatType> source, final int[] range )
	{
		super( source );
		this.range = range;
	}

	/**
	 * Adds all pixels
	 * 
	 * @param source - on which data
	 */
	public LocalMaximaAll( final RandomAccessibleInterval<FloatType> source )
	{
		super( source );
		
		this.range = new int[ source.numDimensions() ];
		
		for ( int d = 0; d < source.numDimensions(); ++d )
			this.range[ d ] = 1;
	}

	@Override
	public ArrayList<int[]> estimateLocalMaxima()
	{
		final ArrayList< int[] > peakList = new ArrayList<int[]>();
		
		final Cursor< FloatType > cursor = Views.iterable( source ).localizingCursor();
		
		while ( cursor.hasNext() )
		{
			cursor.fwd();
			
			final int[] tmp = new int[ numDimensions ];
            
            for ( int d = 0; d < numDimensions; ++d )
            	tmp[ d ] = cursor.getIntPosition( d );
            
            peakList.add( tmp );
		}
		
		return peakList;
	}

}
