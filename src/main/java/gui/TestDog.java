package gui;

import java.util.concurrent.Executors;

import net.imglib2.RandomAccess;
import net.imglib2.algorithm.dog.DifferenceOfGaussian;
import net.imglib2.algorithm.dog.DogDetection;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.img.imageplus.ImagePlusImgs;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import ij.ImageJ;
import mpicbg.imglib.multithreading.SimpleMultiThreading;
import mpicbg.spim.registration.detection.DetectionSegmentation;

public class TestDog {
	
	public static float computeSigma2(final float sigma1, final int sensitivity) {
		final float k = (float) DetectionSegmentation.computeK(sensitivity);
		final float[] sigma = DetectionSegmentation.computeSigma(k, sigma1);

		return sigma[1];
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
	
	
	public static void main (String[] args) throws IncompatibleTypeException{
		new ImageJ();
		Img<FloatType> img = ArrayImgs.floats(new long[]{50, 50, 50});	

		long [] iPos = new long []{20, 20, 20}; 
		RandomAccess<FloatType> ra = img.randomAccess();
		ra.setPosition(iPos);
		ra.get().set(255);

		ImageJFunctions.show(img);
		
		testDog(img, iPos);
	}
}
