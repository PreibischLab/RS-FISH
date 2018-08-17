package test;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import background.plane.fit.BackgroundSubtraction;
import ij.ImageJ;
import util.ImgLib2Util;

public class TestBackgroundSubtraction {
	
	public static void testBackgroundSubtraction(){
		new ImageJ();
		File file = new File("/home/milkyklim/Desktop/white.tif");
		final Img<FloatType> img = ImgLib2Util.openAs32Bit(file);
		final Img<FloatType> dst = ImgLib2Util.openAs32Bit(file);
		
		sloppyImage(img);
		sloppyImage(dst);
		
		ImageJFunctions.show(img);
		long [] position = new long []{200, 200};		
		long [] offset = new long []{300, 300}; 
		long inT = System.nanoTime();
		
		ArrayList<long[]> peaks = new ArrayList<>(1);
		peaks.add(position);
		
 		BackgroundSubtraction.applyBackgroundSubtraction(img, peaks, offset[0]);
				
		System.out.println("plain fitting in "+ TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inT)/1000.0);
		ImageJFunctions.show(img);
	} 
	
	// creates the slop in the image 
	public static void sloppyImage(RandomAccessibleInterval<FloatType> src){
		
		Cursor<FloatType> cursor = Views.iterable(src).cursor();
		
		long [] position = new long[src.numDimensions()];
		
		while(cursor.hasNext()){
			cursor.fwd();
			cursor.localize(position);
			
			// create a sloppy image
			cursor.get().set(position[1]);
		}
	}
	
	public static void main(String [] args){
		testBackgroundSubtraction();
	} 
}
