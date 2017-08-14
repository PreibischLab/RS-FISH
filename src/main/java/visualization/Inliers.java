package visualization;

import java.util.ArrayList;

import fit.Spot;
import ij.ImagePlus;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

public class Inliers {
	
	ImagePlus imp; // initial image
	
	public Inliers(){
		
	}
	
	// make the overlay of the initital image and the RANSAC result 
	// TODO: DEPRECATED
	public static void showInliers(ImagePlus imp, ArrayList<Spot> spots){	
		int [] impDimensions = imp.getDimensions();
		long [] dimensions;
		
		if (imp.getNChannels() != 1)
			System.out.println("only 1 channel images are supported");
		
		
		if(imp.getNFrames() != 1)
			dimensions = new long[]{impDimensions[0], impDimensions[1], impDimensions[3], impDimensions[4]};
		else 
			dimensions = new long[]{impDimensions[0], impDimensions[1], impDimensions[3]};
				
		// TODO: for the images with time have to make a loop
		
		
		Img<FloatType> imgInliers = new ArrayImgFactory<FloatType>().create(dimensions, new FloatType());
		Spot.drawRANSACArea(spots, imgInliers);
		
		ImageJFunctions.show(imgInliers);
	}
	
	public static void showInliers(RandomAccessibleInterval<FloatType> img, ArrayList<Spot> spots)
	{
		long [] dimensions = new long [img.numDimensions()];
		img.dimensions(dimensions);
		
	
		// TODO: doesn't fix all the cases! 
		if (dimensions.length == 5)
			System.out.println("only 1 channel images are supported");
				
		// TODO: for the images with time have to make a loop
		
		Img<FloatType> imgInliers = new ArrayImgFactory<FloatType>().create(dimensions, new FloatType());
		Spot.drawRANSACArea(spots, imgInliers);
		// TODO: (re)-move 
		ImageJFunctions.show(imgInliers).setTitle("RANSAC inliers");
		
		mergeImages(img, imgInliers);
	}
	
	
	public static void mergeImages(RandomAccessibleInterval<FloatType> img, RandomAccessibleInterval<FloatType> inliers){
		Cursor<FloatType> imgCursor = Views.iterable(img).cursor();
		Cursor<FloatType> inlCursor = Views.iterable(inliers).cursor();
		
		if (isSameSize(img, inliers)){
			
			int numDimensions = img.numDimensions();
			long [] dimensions = new long[numDimensions + 1];
			img.dimensions(dimensions);
			dimensions[numDimensions] = 2; // will have 2 channel image 
			
			Img<FloatType> result = new ArrayImgFactory<FloatType>().create(dimensions, new FloatType());
			
			RandomAccess<FloatType> ra = result.randomAccess();
			
			long [] position = new long[numDimensions + 1];
						
			while(imgCursor.hasNext()){
				imgCursor.fwd();
				inlCursor.fwd();	
				
				// set the 1st channel
				imgCursor.localize(position);
				position[numDimensions] = 0;
				ra.setPosition(position);
				ra.get().set(imgCursor.get());
				// set the 2nd channel
				inlCursor.localize(position);
				position[numDimensions] = 1;
				ra.setPosition(position);
				ra.get().set(inlCursor.get());
				
			}
			
			ImageJFunctions.show(result);
		}
		
		
	} 
	
	public static <T extends NativeType<T>> boolean isSameSize(RandomAccessibleInterval<T> img1, RandomAccessibleInterval<T> img2){
		
		boolean isSame = true; 
		
		if (img1.numDimensions() != img2.numDimensions())
			isSame = false;
		else{
			for (int d = 0; d < img1.numDimensions(); d++){
				if(img1.dimension(d) != img2.dimension(d)){
					isSame = false;
					break;
				}
					
			}
		}
		
		return isSame;
	}
	
	
	public static void main(String[] args){
		
	}
	
	
}
