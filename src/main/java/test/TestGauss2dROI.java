/*-
 * #%L
 * A plugin for radial symmetry localization on smFISH (and other) images.
 * %%
 * Copyright (C) 2016 - 2023 RS-FISH developers.
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

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Random;

import net.imglib2.Point;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.dog.DogDetection;
import net.imglib2.algorithm.localextrema.RefinedPeak;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

import background.NormalizedGradient;
import fitting.Spot;
import gradient.Gradient;
import gradient.GradientPreCompute;
import gui.interactive.HelperFunctions;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
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
public class TestGauss2dROI 
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
			System.out.println( Util.printCoordinates( location ) );
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
		
		// final LocalMaxima candiateSearch;
		// candiateSearch = new LocalMaximaDoG( image, 0.7, 1.2, 0.1 );
		// candiateSearch = new LocalMaximaNeighborhood( image );
		// candiateSearch = new LocalMaximaSmoothNeighborhood( image, new double[]{ 1, 1 } );
		// candiateSearch = new LocalMaximaAll( image );
		// final ArrayList< int[] > peaks = candiateSearch.estimateLocalMaxima();

		// grab the image
		ImagePlus imp = WindowManager.getCurrentImage();
		// set the square roi in the middle of the image 
		Rectangle rectangle = new Rectangle(imp.getWidth() / 4, imp.getHeight() / 4, imp.getWidth() / 2, imp.getHeight() / 2);
		imp.setRoi(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
		
		int supportRadius = 5;
		float sigma1 = 5.0f;
		int stepsPerOctave = 4;
		float threshold = 0.3f;
		float sigma2 = HelperFunctions.computeSigma2(sigma1, stepsPerOctave);
		double [] calibration = new double[]{1, 1};
	
		long [] min = new long []{rectangle.x - supportRadius, rectangle.y - supportRadius};
		long [] max = new long []{rectangle.width + rectangle.x + supportRadius - 1, rectangle.height + rectangle.y + supportRadius - 1};
		// crop the roi + support region for calculations 			
		RandomAccessibleInterval<FloatType> extendedRoi = Views.interval(Views.extendMirrorSingle(image), min, max);
		// search for initial spots
		final DogDetection<FloatType> dog2 = new DogDetection<>(extendedRoi, calibration, sigma1, sigma2 , DogDetection.ExtremaType.MINIMA,  threshold/4, false);
		ArrayList<RefinedPeak<Point>> dogPeaks = dog2.getSubpixelPeaks();
		
		//ImageJFunctions.show( dog2.typedDogDetection.dogImg );
		
		System.out.println();
		
		for ( final RefinedPeak<Point > p : dogPeaks )
			System.out.println( p.getOriginalPeak() + ": " + p.getDoublePosition(0) + " " + p.getDoublePosition(1) + ": " + p.getValue() );
		

		// copy peaks to ArrayList<long[]> to use Spot interface	
		final ArrayList<long[]> simplifiedPeaks = new ArrayList<>(1);
		HelperFunctions.copyPeaks(dogPeaks, simplifiedPeaks, extendedRoi.numDimensions(), null, 0 );
	
		// 155,74 -- (155.06198848551116, 73.87444439259127)

		// we need something to compute the derivatives
		final Gradient derivative;
		//derivative = new DerivativeOnDemand( image );
		derivative = new GradientPreCompute( extendedRoi ); // is corrected for min-offset
		
		ImageJFunctions.show( new GradientPreCompute( extendedRoi ).preCompute( extendedRoi ) ).setTitle("extendedRoi gradient");
		//SimpleMultiThreading.threadHaltUnClean();

		// normalize gradient?
		NormalizedGradient ng = null;
		// ng = new NormalizedGradientRANSAC( derivative );
		// ng = new NormalizedGradientMedian( derivative );
		// ng = new NormalizedGradientAverage( derivative );

		final ArrayList< Spot > spots = Spot.extractSpots( extendedRoi, simplifiedPeaks, derivative, ng, range );
		
		//GradientDescent.testGradientDescent( spots, new boolean[]{ false, false, true } );
		//SimpleMultiThreading.threadHaltUnClean();
		
		// ransac on all spots
		Spot.ransac( spots, 100, 0.15, 10.0/100.0, 0, false, 0.0, 0.0, null, null, null, null );
		
		// print localizations
		int c = 0;
		ArrayList< Spot > goodspots = new ArrayList<Spot>();
		for ( final Spot spot : spots )
		{
			spot.computeAverageCostInliers();
			
			if ( spot.inliers.size() > 10 && dist( spot.localize(), spot.getOriginalLocation() ) < 2 )
			{
				System.out.println( spot + " " +  (c++) + " --- " + Util.printCoordinates( spot.getOriginalLocation() ) );
				goodspots.add( spot );
			}
			else
			{
				System.out.println( spot + " " +  (c++) + " BAD--- " + Util.printCoordinates( spot.getOriginalLocation() ) );
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
