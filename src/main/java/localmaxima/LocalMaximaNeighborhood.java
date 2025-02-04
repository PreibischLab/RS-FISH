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
package localmaxima;

import java.util.ArrayList;

import net.imglib2.Cursor;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.neighborhood.Neighborhood;
import net.imglib2.algorithm.neighborhood.RectangleShape;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
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
public class LocalMaximaNeighborhood extends LocalMaxima
{
	final double threshold;
	
	public LocalMaximaNeighborhood( final RandomAccessibleInterval<FloatType> source )
	{
		this( source, -Double.MAX_VALUE );
	}

	public LocalMaximaNeighborhood( final RandomAccessibleInterval<FloatType> source, final double threshold )
	{
		super( source );

		this.threshold = threshold;;
	}
	
	@Override
	public ArrayList<int[]> estimateLocalMaxima()
	{
		final ArrayList< int[] > peakList = new ArrayList<int[]>();
		
		// define an interval that is one pixel smaller on each side in each dimension,
        // so that the search in the 8-neighborhood (3x3x3...x3) never goes outside
        // of the defined interval
        final Interval interval = Intervals.expand( source, -1 );
 
        // create a view on the source with this interval
        final RandomAccessibleInterval< FloatType > area = Views.interval( source, interval );
        
        // create a cursor on the smaller interval
		final Cursor< FloatType > center = Views.iterable( area ).cursor();
		
		// instantiate a RectangleShape to access rectangular local neighborhoods
        // of radius 1 (that is 3x3x...x3 neighborhoods), skipping the center pixel
        // (this corresponds to an 8-neighborhood in 2d or 26-neighborhood in 3d, ...)
		final RectangleShape shape = new RectangleShape( 1, true );
		
		 // iterate over the set of neighborhoods in the image
        for ( final Neighborhood< FloatType > localNeighborhood : shape.neighborhoods( area ) )
        {
            // what is the value that we investigate?
            // (the center cursor runs over the image in the same iteration order as neighborhood)
            final float centerValue = center.next().get();
 
            // only if it has a certain value
            if ( centerValue < threshold )
            	continue;
            
            // keep this boolean true as long as no other value in the local neighborhood
            // is larger or equal
            boolean isMax = true;
 
            // check if all pixels in the local neighborhood that are smaller
            for ( final FloatType value : localNeighborhood )
            {
            	if ( value.get() >= centerValue )
                {
            		isMax = false;
                    break;
                }
            }
 
            if ( isMax )
            {
                final int[] tmp = new int[ numDimensions ];
                
                for ( int d = 0; d < numDimensions; ++d )
                	tmp[ d ] = center.getIntPosition( d );
                
                peakList.add( tmp );
            }
        }
        
		return peakList;
	}
}
