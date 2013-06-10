package localmaxima;

import java.util.ArrayList;

import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

public class LocalMaximaAll extends LocalMaxima
{
	public LocalMaximaAll( final RandomAccessibleInterval<FloatType> source )
	{
		super( source );
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
