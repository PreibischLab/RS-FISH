package test;

import ij.ImageJ;

import java.awt.Cursor;
import java.util.ArrayList;
import java.util.Random;

import fit.OrientedPoint;
import fit.PointFunctionMatch;
import fit.Spot;
import fit.SymmetryCenter3d;
import gradient.Gradient3d;
import gradient.GradientDescent;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;
import mpicbg.imglib.algorithm.fft.FourierConvolution;
import mpicbg.imglib.algorithm.scalespace.DifferenceOfGaussian;
import mpicbg.imglib.algorithm.scalespace.DifferenceOfGaussianPeak;
import mpicbg.imglib.algorithm.scalespace.DifferenceOfGaussianReal1;
import mpicbg.imglib.algorithm.scalespace.SubpixelLocalization;
import mpicbg.imglib.container.ContainerFactory;
import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.cursor.special.RegionOfInterestCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.multithreading.SimpleMultiThreading;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyMirrorFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyValueFactory;
import mpicbg.imglib.type.numeric.real.DoubleType;
import mpicbg.imglib.type.numeric.real.FloatType;
import mpicbg.imglib.util.Util;
import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.Point;
import mpicbg.models.PointMatch;

public class TestGauss 
{
	
	public static ArrayList< DifferenceOfGaussianPeak< FloatType > > findLocalMaxima( final Image< FloatType > image )
	{
		DifferenceOfGaussianReal1< FloatType > dog = new DifferenceOfGaussianReal1<FloatType>( 
				image, new OutOfBoundsStrategyMirrorFactory<FloatType>(), 0.7, 1.2, 0.1f, 1 );
		dog.setKeepDoGImage( true );
		dog.process();
		final ArrayList< DifferenceOfGaussianPeak< FloatType > > peaks = dog.getPeaks();
		
		//SubpixelLocalization< FloatType > sbp = new SubpixelLocalization<FloatType>( image, peaks );
		//sbp.process();

		//for ( final DifferenceOfGaussianPeak< FloatType > peak : peaks )
			//System.out.println( Util.printCoordinates( peak.getPosition() ) + " " + Util.printCoordinates( peak.getSubPixelPosition() ) + " " + peak.getPeakType() );
		
		System.out.println( "Found " + peaks.size() + " local maxima." );
		
		//ImageJFunctions.show( dog.getDoGImage() );
		//SimpleMultiThreading.threadHaltUnClean();
		
		return peaks;
	}
	
	public static void main( String[] args ) throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{
		final Image< FloatType > image = new ImageFactory< FloatType >( new FloatType(), new ArrayContainerFactory() ).createImage( new int[]{ 256, 256, 256 } );
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
		
		// extract data for all spots
		final ArrayList< Spot > spots = Spot.extractSpots( image, findLocalMaxima( image ) );
		
		//GradientDescent.testGradientDescent( spots, new boolean[]{ false, false, true } );
		//SimpleMultiThreading.threadHaltUnClean();
		
		// ransac on all spots
		Spot.ransac( spots, 100, 0.15, 20.0/100.0 );
		
		// print localizations
		for ( final Spot spot : spots )
		{
			spot.computeAverageCostInliers();
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

		
		final Image< FloatType > draw = image.createNewImage();
		Spot.drawRANSACArea( spots, draw );
		draw.getDisplay().setMinMax();
		ImageJFunctions.copyToImagePlus( draw ).show();
		
		final Image< FloatType > detected = image.createNewImage();
		for ( final Spot spot : spots )
		{
			if ( spot.inliers.size() == 0 )
				continue;
			
			final double[] location = new double[]{ spot.center.getXc(), spot.center.getYc(), spot.center.getZc() };
			addGaussian( detected, location, sigma );			
		}
		detected.getDisplay().setMinMax();
		ImageJFunctions.copyToImagePlus( detected ).show();
	}
	
	/**
	 * Adds additive gaussian noise: i = i + gauss(x, sigma)
	 * 
	 * @param amount - how many times sigma
	 * @return the signal-to-noise ratio (measured)
	 */
	public static double addGaussianNoise( final Image< FloatType > img, final Random rnd, final float sigma, boolean onlyPositive )
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

	final public static void addGaussian( final Image< FloatType > image, final double[] location, final double[] sigma )
	{
		final int numDimensions = image.getNumDimensions();
		final int[] size = new int[ numDimensions ];
		final int[] offset = new int[ numDimensions ];
		final double[] two_sq_sigma = new double[ numDimensions ];
		
		for ( int d = 0; d < numDimensions; ++d )
		{
			size[ d ] = Util.getSuggestedKernelDiameter( sigma[ d ] ) * 2;
			offset[ d ] = (int)Math.round( location[ d ] ) - size[ d ]/2;
			two_sq_sigma[ d ] = 2 * sigma[ d ] * sigma[ d ];
		}

		final LocalizableByDimCursor< FloatType > randomAccess = image.createLocalizableByDimCursor( new OutOfBoundsStrategyValueFactory<FloatType>() );
		final RegionOfInterestCursor< FloatType > roi = new RegionOfInterestCursor<FloatType>( randomAccess, offset, size );
		
		while ( roi.hasNext() )
		{
			roi.fwd();
			
			double value = 1;
			
			for ( int d = 0; d < numDimensions; ++d )
			{
				final double x = location[ d ] - randomAccess.getPosition( d );
				value *= Math.exp( -(x * x) / two_sq_sigma[ d ] );
			}
			
			roi.getType().set( roi.getType().get() + (float)value );
		}
	}

}
