package test;

import ij.ImageJ;

import java.util.ArrayList;
import java.util.Random;

import localmaxima.LocalMaxima;
import localmaxima.LocalMaximaAll;
import localmaxima.LocalMaximaDoG;
import localmaxima.LocalMaximaNeighborhood;
import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.NotEnoughDataPointsException;
import net.imglib2.RealLocalizable;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.multithreading.SimpleMultiThreading;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;
import fit.Spot;
import gradient.Gradient;
import gradient.GradientPreCompute;

public class TestGauss2d 
{
	public static void main( String[] args ) throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{
		// size around the detection to use
		// we detect at 0.5, 0.5, 0.5 - so we need an even size
		final int[] range = new int[]{ 10, 10 };
		
		final Img< FloatType > image = new ArrayImgFactory< FloatType >().create( new int[]{ 256, 256 }, new FloatType() );
		final ArrayList< double[] > points = new ArrayList<double[]>();
		final Random rnd = new Random( 534545 );
		final double[] sigma = new double[]{ 2, 2 };
		
		
		for ( int i = 0; i < 100; ++i )
		{
			final double[] location = new double[]{ rnd.nextDouble() * 256, rnd.nextDouble() * 256 };
			TestGauss3d.addGaussian( image, location, sigma );
			points.add( location );
		}
		
		/*
		TestGauss3d.addGaussian( image, new double[]{ 10.6, 10 }, new double[]{ 2, 2 } );
		TestGauss3d.addGaussian( image, new double[]{ 100.3, 100.1 }, new double[]{ 2, 2 } );
		TestGauss3d.addGaussian( image, new double[]{ 102.8, 104.0 }, new double[]{ 2, 2 } );
		*/
		//addGaussianNoise( image, rnd, 0.1f, true );
		
		new ImageJ();
		ImageJFunctions.show( image );
		
		//SimpleMultiThreading.threadHaltUnClean();
		
		//
		// extract data for all spots
		//
		
		// we need an initial candidate search
		
		final LocalMaxima candiateSearch;
		//candiateSearch = new LocalMaximaDoG( image, 0.7, 1.2, 0.1 );
		candiateSearch = new LocalMaximaNeighborhood( image );
		//candiateSearch = new LocalMaximaSmoothNeighborhood( image, new double[]{ 1, 1 } );
		//candiateSearch = new LocalMaximaAll( image );
		
		final ArrayList< int[] > peaks = candiateSearch.estimateLocalMaxima();
		
		// we need something to compute the derivatives
		final Gradient derivative;
		
		//derivative = new DerivativeOnDemand( image );
		derivative = new GradientPreCompute( image );
		
		final ArrayList< Spot > spots = Spot.extractSpots( image, peaks, derivative, range );
		
		//GradientDescent.testGradientDescent( spots, new boolean[]{ false, false, true } );
		//SimpleMultiThreading.threadHaltUnClean();
		
		// ransac on all spots
		Spot.ransac( spots, 100, 0.15, 10.0/100.0 );
		
		// print localizations
		int c = 0;
		ArrayList< Spot > goodspots = new ArrayList<Spot>();
		for ( final Spot spot : spots )
		{
			spot.computeAverageCostInliers();
			
			if ( spot.inliers.size() > 10 && dist( spot.getCenter(), spot.getOriginalLocation() ) < 0.7 )
			{
				System.out.println( spot + " " +  (c++) + " --- " + Util.printCoordinates( spot.getOriginalLocation() ) );
				goodspots.add( spot );
			}
		}
		
		Img< FloatType > ransacarea = image.factory().create( image, image.firstElement() );
		Spot.drawRANSACArea( goodspots, ransacarea );
		ImageJFunctions.show( ransacarea );
	}
	
	public static double dist( final double[] p1, final int[] p2 )
	{
		double dist = 0;
		
		for ( int d = 0; d < p1.length; ++d )
		{
			final double t = p1[ d ] - p2[ d ];
			dist += t*t;
		}
		
		return Math.sqrt( dist );
	}
}
