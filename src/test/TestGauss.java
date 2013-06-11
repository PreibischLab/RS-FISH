package test;

import ij.ImageJ;

import java.util.ArrayList;
import java.util.Random;


import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.legacy.scalespace.DifferenceOfGaussian;
import net.imglib2.algorithm.legacy.scalespace.DifferenceOfGaussianPeak;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.multithreading.SimpleMultiThreading;
import net.imglib2.outofbounds.OutOfBoundsMirrorFactory;
import net.imglib2.outofbounds.OutOfBoundsMirrorFactory.Boundary;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

import fit.OrientedPoint;
import fit.PointFunctionMatch;
import fit.Spot;
import fit.SymmetryCenter3d;
import gradient.Derivative;
import gradient.DerivativeOnDemand;
import gradient.DerivativePreCompute;
import gradientdescent.GradientDescent;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;
import localmaxima.LocalMaxima;
import localmaxima.LocalMaximaAll;
import localmaxima.LocalMaximaDoG;
import localmaxima.LocalMaximaNeighborhood;
import localmaxima.LocalMaximaSmoothNeighborhood;
import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.Point;
import mpicbg.models.PointMatch;

public class TestGauss 
{	
	public static void main( String[] args ) throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{
		// size around the detection to use
		// we detect at 0.5, 0.5, 0.5 - so we need an even size
		final int[] range = new int[]{ 10, 10, 10 };
		
		final Img< FloatType > image = new ArrayImgFactory< FloatType >().create( new int[]{ 256, 256, 256 }, new FloatType() );
		final ArrayList< double[] > points = new ArrayList<double[]>();
		final Random rnd = new Random( 534545 );
		final double[] sigma = new double[]{ 2, 2, 2 };
		
		/*
		for ( int i = 0; i < 1500; ++i )
		{
			final double[] location = new double[]{ rnd.nextDouble() * 256, rnd.nextDouble() * 256, rnd.nextDouble() * 256 };
			addGaussian( image, location, sigma );
			points.add( location );
		}
		*/

		addGaussian( image, new double[]{ 10.6, 10, 10 }, new double[]{ 2, 2, 2 } );
		addGaussian( image, new double[]{ 100.3, 100.1, 100.8 }, new double[]{ 2, 2, 2 } );
		addGaussian( image, new double[]{ 102.8, 104.0, 100.8 }, new double[]{ 2, 2, 2 } );
		
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
		final Derivative derivative;
		
		//derivative = new DerivativeOnDemand( image );
		derivative = new DerivativePreCompute( image );
		
		final ArrayList< Spot > spots = Spot.extractSpots( image, peaks, derivative, range );
		
		//GradientDescent.testGradientDescent( spots, new boolean[]{ false, false, true } );
		//SimpleMultiThreading.threadHaltUnClean();
		
		// ransac on all spots
		Spot.ransac( spots, 100, 0.15, 20.0/100.0 );
		
		// print localizations
		for ( final Spot spot : spots )
		{
			spot.computeAverageCostInliers();
			
			//if ( spot.numRemoved != spot.candidates.size() )
				System.out.println( spot );
		}

		SimpleMultiThreading.threadHaltUnClean();
		
		int foundCorrect = 0;
		double avgDist = 0;
		
		final double[] center = new double[ 3 ];
		for ( final Spot spot : spots )
		{
			if ( spot.inliers.size() < 2 )
				continue;
			
			spot.center.fit( spot.inliers );
			spot.getCenter( center );
			
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

		avgDist /= (double)foundCorrect;
		
		System.out.println( "found " + spots.size() );
		System.out.println( "found correct " + foundCorrect + " with avg error of " + avgDist );

		
		final Img< FloatType > draw = image.factory().create( image, image.firstElement() );
		Spot.drawRANSACArea( spots, draw );
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
	 * @param amount - how many times sigma
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

	final public static void addGaussian( final Img< FloatType > image, final double[] location, final double[] sigma )
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
				value *= Math.exp( -(x * x) / two_sq_sigma[ d ] );
			}
			
			cursor.get().set( cursor.get().get() + (float)value );
		}
	}

}
