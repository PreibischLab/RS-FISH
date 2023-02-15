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

import java.util.concurrent.Executors;

import ij.ImageJ;
import mpicbg.imglib.multithreading.SimpleMultiThreading;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.algorithm.dog.DifferenceOfGaussian;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

public class TestDog {
	
	public static float computeSigma2(final float sigma1, final int sensitivity) {
		final float k = (float) computeK(sensitivity);
		final float[] sigma = computeSigma(k, sigma1);

		return sigma[1];
	}

	public static double computeK( final float stepsPerOctave ) { return Math.pow( 2f, 1f / stepsPerOctave ); }
	public static double computeK( final int stepsPerOctave ) { return Math.pow( 2f, 1f / stepsPerOctave ); }
	public static float[] computeSigma( final float k, final float initialSigma )
	{
		final float[] sigma = new float[ 2 ];

		sigma[ 0 ] = initialSigma;
		sigma[ 1 ] = sigma[ 0 ] * k;

		return sigma;
	}
	public static float getDiffSigma( final float sigmaA, final float sigmaB ) { return (float) Math.sqrt( sigmaB * sigmaB - sigmaA * sigmaA ); }
	public static float[] computeSigmaDiff( final float[] sigma, final float imageSigma )
	{
		final float[] sigmaDiff = new float[ 2 ];

		sigmaDiff[ 0 ] = getDiffSigma( imageSigma, sigma[ 0 ] );
		sigmaDiff[ 1 ] = getDiffSigma( imageSigma, sigma[ 1 ] );

		return sigmaDiff;
	}	

	public static void testDog(Img<FloatType> img, long [] iPos) throws IncompatibleTypeException{
		
		//Gauss3.gauss(2, Views.extendZero( img ), img );
		//ImageJFunctions.show( img );
		
		double [] calibration = new double [img.numDimensions()];
		for (int d = 0; d < img.numDimensions(); ++d)
			calibration[d] = 1;		
		
		float imageSigma = 0.5f;
		int sensitivity = 4;
		float threshold = 0.4f;
		
		Img<FloatType> out = img.factory().create(img, img.firstElement());

		float sigma = 3.5f; // max sigma = 10;				
		float sigma2 = computeSigma2(sigma, sensitivity);
		
		final double[][] sigmas = DifferenceOfGaussian.computeSigmas(imageSigma, 2.0, calibration, sigma, sigma2);

		DifferenceOfGaussian.DoG( sigmas[ 0 ], sigmas[ 1 ], Views.extendZero( img ), out, Executors.newFixedThreadPool( 4 ) );
		RandomAccess<FloatType> ra = out.randomAccess(); 
		ra.setPosition(iPos);
		final float v = ra.get().get();
		
		ImageJFunctions.show( out );
		SimpleMultiThreading.threadHaltUnClean();

		for(double j = 0.5f; j < 10; j *= 1.1 ){
			
			Gauss3.gauss( new double[]{ 0.1, 0.1, j }, Views.extendZero(img), out );
			
			DifferenceOfGaussian.DoG( sigmas[ 0 ], sigmas[ 1 ], Views.extendZero( out ), out, Executors.newFixedThreadPool( 4 ) );
			//ImageJFunctions.show( out );
			//SimpleMultiThreading.threadHaltUnClean();
			//final DogDetection<FloatType> dog = new DogDetection<>(img, calibration, sigma1y, sigma2y, DogDetection.ExtremaType.MINIMA, threshold / 4, false);
			//dog.setKeepDoGImg( true );
			//dog.getPeaks();

			
			
						
			System.out.print(j);// + " " + sigmas[0][0] + " " + sigmas[1][0]);

			ra = out.randomAccess(); 
			ra.setPosition(iPos);
				System.out.println( ": " + ra.get().get()/v + " ");
				
//				final DogDetection<FloatType> dog = new DogDetection<>(img, calibration, sigma1y, sigma2y, DogDetection.ExtremaType.MINIMA, threshold / 4, false);	
//				
//				// FIXME: Function for gauss smoothing? 
//				
//				long [] position = new long[img.numDimensions()];
//				if (dog.getPeaks().size() < 1)
//					dog.getPeaks().get(0).localize(position);	
				
		}	
	}
	
	
	public static void testRAI(Img<FloatType> img){
		Cursor<FloatType> cursor = img.cursor();
		
		while(cursor.hasNext()){
			cursor.fwd();
			System.out.println(cursor.getLongPosition(0) + " " + cursor.getLongPosition(1));
		}
	}
	
	public static void main (String[] args) throws IncompatibleTypeException{
		new ImageJ();
		Img<FloatType> img = ArrayImgs.floats(new long[]{50, 50});	

		long [] iPos = new long []{20, 20}; 
		RandomAccess<FloatType> ra = img.randomAccess();
		ra.setPosition(iPos);
		ra.get().set(255);

		ImageJFunctions.show(img);
		
		testRAI(img);
		// testDog(img, iPos);
	}
}
