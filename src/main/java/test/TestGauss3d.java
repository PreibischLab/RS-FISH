/*-
 * #%L
 * A plugin for radial symmetry localization on smFISH (and other) images.
 * %%
 * Copyright (C) 2016 - 2024 RS-FISH developers.
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
package test;

import java.util.ArrayList;
import java.util.Random;

import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

import background.NormalizedGradient;
import background.NormalizedGradientRANSAC;
import fitting.Spot;
import fitting.Center.CenterMethod;
import gradient.Gradient;
import gradient.GradientPreCompute;
import ij.ImageJ;
import localmaxima.LocalMaxima;
import localmaxima.LocalMaximaDoG;
import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.NotEnoughDataPointsException;

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
public class TestGauss3d 
{	
	public static void main( String[] args ) throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{
		// size around the detection to use
		// we detect at 0.5, 0.5, 0.5 - so we need an even size
		final long[] range = new long[]{ 10, 10, 10 };
		
		final Img< FloatType > image = new ArrayImgFactory< FloatType >().create( new int[]{ 256, 256, 256 }, new FloatType() );
		final ArrayList< double[] > points = new ArrayList<double[]>();
		final Random rnd = new Random( 534545 );
		final double[] sigma = new double[]{ 1.5, 1.5, 1.5 };
		
		
		for ( int i = 0; i < 1500; ++i )
		{
			// small adjustment to have a padding around all points
			// (int)(Math.random() * ((Max - Min) + 1)) + Min
			double minValue = 20;
			double maxValue = 235;
			double x = rnd.nextDouble()*( (maxValue - minValue)  + 1 ) + minValue; 
			double y = rnd.nextDouble()*( (maxValue - minValue)  + 1 ) + minValue; 
			double z = rnd.nextDouble()*( (maxValue - minValue)  + 1 ) + minValue; 
					
			final double[] location = new double[]{ x, y, z };
			addGaussian( image, location, sigma );
			points.add( location );
		}
		

		//addGaussian( image, new double[]{ 10.6, 10, 10 }, new double[]{ 2, 2, 2 } );
		//addGaussian( image, new double[]{ 100.3, 100.1, 100.8 }, new double[]{ 2, 2, 2 } );
		//addGaussian( image, new double[]{ 102.8, 104.0, 100.8 }, new double[]{ 2, 2, 2 } );
		
		//addGaussianNoise( image, rnd, 0.1f, true );
		
		new ImageJ();
		ImageJFunctions.show( image );
		
		//SimpleMultiThreading.threadHaltUnClean();
		
		//
		// extract data for all spots
		//
		
		// we need an initial candidate search
		
		final LocalMaxima candiateSearch;
		candiateSearch = new LocalMaximaDoG( image, 0.7, 1.2, 0.1 );
		//candiateSearch = new LocalMaximaNeighborhood( image );
		//candiateSearch = new LocalMaximaSmoothNeighborhood( image, new double[]{ 1, 1, 1 } );
		//candiateSearch = new LocalMaximaAll( image );
		
		final ArrayList< int[] > peaks = candiateSearch.estimateLocalMaxima();
		
		// we need something to compute the derivatives
		final Gradient derivative;
		
		//derivative = new DerivativeOnDemand( image );
		derivative = new GradientPreCompute( image );

		NormalizedGradient ng = null;
		ng = new NormalizedGradientRANSAC( derivative, CenterMethod.MEAN );
		//ng = new NormalizedGradientMedian( derivative );
		//ng = new NormalizedGradientAverage( derivative );

		final ArrayList< Spot > spots = Spot.extractSpots( image, TestGauss2d.int2long( peaks ), derivative, ng, range );
		
//		System.out.println("peaks size = " + peaks.size());
//		System.out.println("spots size = " + spots.size());
		
		//GradientDescent.testGradientDescent( spots, new boolean[]{ false, false, true } );
		//SimpleMultiThreading.threadHaltUnClean();
		
		// ransac on all spots
		Spot.ransac( spots, 100, 0.15, 20.0/100.0, 0, false, 0.0, 0.0, null, null, null, null );
		
		// print localizations
		for ( final Spot spot : spots )
		{
			spot.computeAverageCostInliers();
			
			//if ( spot.numRemoved != spot.candidates.size() )
			// 	System.out.println( spot );
		}

		// SimpleMultiThreading.threadHaltUnClean();
		
		int foundCorrect = 0;
		double avgDist = 0;
		
		final double[] center = new double[ 3 ];
		for ( final Spot spot : spots )
		{
			if ( spot.inliers.size() < 2 )
				continue;
			
			spot.center.fit( spot.inliers );
			spot.localize( center );
			
			int minIndex = -1;
			double minDist = Double.MAX_VALUE;
			
			// find closest original point
			for ( int i = 0; i < points.size(); ++i )
			{
				final double d = distance( points.get( i ), center );
				
				if ( d < minDist )
				{
					minDist = d;
					minIndex = i;
				}
			}
			
			if ( minDist < 0.5 )
			{
				++foundCorrect;
				avgDist += minDist;
				points.remove( minIndex );
			}
		}

//		System.out.println("spots size = " + spots.size());
		
		avgDist /= (double)foundCorrect;
		
		System.out.println( "found " + spots.size() );
		System.out.println( "found correct " + foundCorrect + " with avg error of " + avgDist );

		
		final Img< FloatType > draw = image.factory().create( image, image.firstElement() );
		Spot.drawRANSACArea( spots, draw, true);
		ImageJFunctions.show( draw );
		
		final Img< FloatType > detected = image.factory().create( image, image.firstElement() );
		for ( final Spot spot : spots )
		{
			if ( spot.inliers.size() == 0 )
				continue;
			
			final double[] location = new double[ image.numDimensions() ];//{ spot.center.getXc(), spot.center.getYc(), spot.center.getZc() };
			spot.center.getSymmetryCenter( location );
			addGaussian( detected, location, sigma );			
		}
		ImageJFunctions.show( detected );
	}
	
	/**
	 * Adds additive gaussian noise: i = i + gauss(x, sigma)
	 * 
	 * @param img - to which image
	 * @param rnd - random num generator
	 * @param sigma - sigma
	 * @param onlyPositive - if it can be negative
	 * @return the signal-to-noise ratio (measured)
	 */
	public static double addGaussianNoise( final Img< FloatType > img, final Random rnd, final float sigma, boolean onlyPositive )
	{
		for ( final FloatType f : img )
		{
			float newValue = f.get() + (float)( rnd.nextGaussian() * sigma );
			
			if ( onlyPositive )
				newValue = Math.max( 0, newValue );
			
			f.set( newValue );
		}
		
		return 1;
	}

	final static public double distance( final double[] p1, final double[] p2 )
	{
		double sum = 0.0;
		for ( int i = 0; i < p1.length; ++i )
		{
			final double d = p1[ i ] - p2[ i ];
			sum += d * d;
		}
		return Math.sqrt( sum );
	}

	final public static void addGaussian( final RandomAccessibleInterval< FloatType > image, final double[] location, final double[] sigma, float A, boolean total)
	{
		final int numDimensions = image.numDimensions();
		final int[] size = new int[ numDimensions ];
		
		final long[] min = new long[ numDimensions ];
		final long[] max = new long[ numDimensions ];
		
		final double[] two_sq_sigma = new double[ numDimensions ];
		
		for ( int d = 0; d < numDimensions; ++d )
		{
			size[ d ] = Util.getSuggestedKernelDiameter( sigma[ d ] ) * 2;
			min[ d ] = (int)Math.round( location[ d ] ) - size[ d ]/2;
			max[ d ] = min[ d ] + size[ d ] - 1;
			two_sq_sigma[ d ] = 2 * sigma[ d ] * sigma[ d ];
		}

		final RandomAccessible< FloatType > infinite = Views.extendZero( image );
		final RandomAccessibleInterval< FloatType > interval = Views.interval( infinite, min, max );
		final IterableInterval< FloatType > iterable = Views.iterable( interval );
		final Cursor< FloatType > cursor = iterable.localizingCursor();
		
		while ( cursor.hasNext() )
		{
			cursor.fwd();
			
			double value = 1;
			
			for ( int d = 0; d < numDimensions; ++d )
			{
				final double x = location[ d ] - cursor.getIntPosition( d );
				value *= A*Math.exp( -(x * x) / two_sq_sigma[ d ] );
			}
			
			if (total)
				cursor.get().set( cursor.get().get() + (float)value );
			else
				cursor.get().set( Math.max(cursor.get().get(), (float)value) );
		}
	}

	final public static void addGaussian( final RandomAccessibleInterval< FloatType > image, final double[] location, final double[] sigma, float A){
		addGaussian(image, location, sigma, A, true);
	}
	
	final public static void addGaussian( final RandomAccessibleInterval< FloatType > image, final double[] location, final double[] sigma){
		addGaussian(image, location, sigma, 1, true);
	}
	
	
}
