package test;

import java.util.ArrayList;
import java.util.Random;

import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;

import background.NormalizedGradient;
import background.NormalizedGradientAverage;
import fitting.Spot;
import gradient.Gradient;
import gradient.GradientPreCompute;
import ij.ImageJ;
import localmaxima.LocalMaxima;
import localmaxima.LocalMaximaNeighborhood;
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
public class TestGauss2d 
{
	public static void main( String[] args ) throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{
		// size around the detection to use
		// we detect at 0.5, 0.5, 0.5 - so we need an even size
		final long[] range = new long[]{ 10, 10 };
		
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

		// adding a global gradient for testing
		/*
		final Cursor< FloatType > cu = image.localizingCursor();
		while ( cu.hasNext() )
		{
			cu.fwd();
			cu.get().set( cu.get().get() + cu.getIntPosition( 0 )/50.0f + cu.getIntPosition( 1 )/100.0f );
		}*/

		// we need something to compute the derivatives
		final Gradient derivative;
		
		//derivative = new DerivativeOnDemand( image );
		derivative = new GradientPreCompute( image );
		
		ImageJFunctions.show( new GradientPreCompute( image ).preCompute( image ) );
		//SimpleMultiThreading.threadHaltUnClean();

		// normalize gradient?
		NormalizedGradient ng = null;
		//ng = new NormalizedGradientRANSAC( derivative );
		//ng = new NormalizedGradientMedian( derivative );
		ng = new NormalizedGradientAverage( derivative );

		final ArrayList< Spot > spots = Spot.extractSpots( image, int2long( peaks ), derivative, ng, range );
		
		//GradientDescent.testGradientDescent( spots, new boolean[]{ false, false, true } );
		//SimpleMultiThreading.threadHaltUnClean();
		
		// ransac on all spots
		Spot.ransac( spots, 100, 0.15, 10.0/100.0, false );
		
		// print localizations
		int c = 0;
		ArrayList< Spot > goodspots = new ArrayList<Spot>();
		for ( final Spot spot : spots )
		{
			spot.computeAverageCostInliers();
			
			if ( spot.inliers.size() > 10 && dist( spot.localize(), spot.getOriginalLocation() ) < 0.7 )
			{
				System.out.println( spot + " " +  (c++) + " --- " + Util.printCoordinates( spot.getOriginalLocation() ) );
				goodspots.add( spot );
			}
		}
		
		Img< FloatType > ransacarea = image.factory().create( image, image.firstElement() );
		Spot.drawRANSACArea( goodspots, ransacarea, true);
		ImageJFunctions.show( ransacarea );
	}

	public static ArrayList< long[] > int2long( final ArrayList< int[] > a )
	{
		final ArrayList< long[] > b = new ArrayList<>();

		for ( final int[] ai : a )
		{
			final long[] bi = new long[ ai.length ];
			for ( int i = 0; i < bi.length; ++i )
				bi[ i ] = ai[ i ];
			b.add( bi );
		}
		return b;
	}

	public static double dist( final double[] p1, final long[] p2 )
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
