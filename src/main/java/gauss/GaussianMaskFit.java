package gauss;

import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.multithreading.SimpleMultiThreading;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;
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
public class GaussianMaskFit 
{
	public static void gaussianMaskFit( 
			final RandomAccessibleInterval<FloatType> signalInterval, 
			final double[] location, 
			final double[] sigma, 
			final Img< FloatType > ransacWeight )
	{
		final int n = signalInterval.numDimensions();
		
		// pre-compute 2*sigma^2
		final double[] two_sq_sigma = new double[ n ];
		for ( int d = 0; d < n; ++d )
			two_sq_sigma[ d ] = 2 * sigma[ d ] * sigma[ d ];

		// make the interval we fit on iterable
		final IterableInterval< FloatType > signalIterable = Views.iterable( signalInterval );
		
		// create the mask image
		final Img< FloatType > gaussianMask = new ArrayImgFactory< FloatType >().create( signalInterval, signalIterable.firstElement() );
		
		// set the mask image to the same location as the interval we fit on and make it iterable
		final long[] translation = new long[ n ];
		for ( int d = 0; d < n; ++d )
			translation[ d ] = signalInterval.min( d );
		
		final RandomAccessibleInterval<FloatType> translatedMask = Views.translate( gaussianMask, translation );
		final IterableInterval<FloatType> translatedIterableMask = Views.iterable( translatedMask );
		
		// remove background in the input
		removeBackground( signalIterable );
		
		double N = 0;
		int i = 0;
		
		do
		{
			setGaussian( translatedIterableMask, location, two_sq_sigma );
			
			// compute the sums
			final Cursor< FloatType > cMask = gaussianMask.cursor();
			final Cursor< FloatType > cImg = signalIterable.localizingCursor();
			final Cursor< FloatType > cWeight;
			
			if ( ransacWeight == null )
				cWeight = null;
			else
				cWeight = ransacWeight.cursor();
			
			double sumLocSN[] = new double[ n ]; // int_{all_px} d * S[ d ] * N[ d ]
			double sumSN = 0; // int_{all_px} S[ d ] * N[ d ]
			double sumSS = 0; // int_{all_px} S[ d ] * S[ d ]
			
			while ( cMask.hasNext() )
			{
				cMask.fwd();
				cImg.fwd();
				
				final double signal = cImg.get().getRealDouble();
				final double mask = cMask.get().getRealDouble();
				final double weight;
				
				if ( cWeight != null )
					weight = cWeight.next().getRealDouble();
				else
					weight = 8;
				
				final double signalmask = signal * mask * weight;
				
				sumSN += signalmask;
				sumSS += signal * signal * weight;
				
				for ( int d = 0; d < n; ++d )
				{
					final double l = cImg.getLongPosition( d );
					sumLocSN[ d ] += l * signalmask;
				}
				
			}
			
			for ( int d = 0; d < n; ++d )
				location[ d ] = sumLocSN[ d ] / sumSN;
			
			N = sumSN / sumSS;
			
			System.out.println( i + ": " + Util.printCoordinates( location )  + " N: " + N );
			
			++i;
			
			// ImageJFunctions.show( gaussianMask );
			// ImageJFunctions.show( signalInterval );
			
			// SimpleMultiThreading.threadHaltUnClean();
		}
		while ( i < 100 );
		
		ImageJFunctions.show( signalInterval );
		ImageJFunctions.show( gaussianMask );
		SimpleMultiThreading.threadHaltUnClean();
	}
	
	public static void removeBackground( final IterableInterval< FloatType > iterable )
	{
		double i = 0;
		
		for ( final FloatType t : iterable )
			i += t.getRealDouble();
		
		i /= (double)iterable.size();
		
		for ( final FloatType t : iterable )
			t.setReal( t.get() - i );		
	}
	
	final public static void setGaussian( final IterableInterval< FloatType > image, final double[] location, final double[] two_sq_sigma )
	{
		final int numDimensions = image.numDimensions();
		
		final Cursor< FloatType > cursor = image.localizingCursor();
		
		while ( cursor.hasNext() )
		{
			cursor.fwd();
			
			double value = 1;
			
			for ( int d = 0; d < numDimensions; ++d )
			{
				final double x = location[ d ] - cursor.getIntPosition( d );
				value *= Math.exp( -(x * x) / two_sq_sigma[ d ] );
			}
			
			cursor.get().setReal( value );
		}
	}
	
}
