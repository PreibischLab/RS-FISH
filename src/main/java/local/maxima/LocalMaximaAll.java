package local.maxima;

import java.util.ArrayList;

import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

/**
 * Radial Symmetry Package
 *
 * This software is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software.  If not, see http://www.gnu.org/licenses/.
 * 
 * @author Stephan Preibisch (stephan.preibisch@gmx.de) and Timothee Lionnet
 */
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
