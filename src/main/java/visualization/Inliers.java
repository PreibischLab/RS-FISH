package visualization;

import java.util.ArrayList;

import fit.Spot;
import ij.ImagePlus;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.real.FloatType;

public class Inliers {
	
	ImagePlus imp; // initial image
	
	public Inliers(){
		
	}
	
	public static void showInliers(ImagePlus imp, ArrayList<Spot> spots){
		
		// what are the possible values for the dimensions
		// x y z + time scale ? 
		// TODO: how to process the multi-channel images
		
		int [] impDimensions = imp.getDimensions();
		long [] dimensions;
		if(imp.getNFrames() != 1)
			dimensions = new long[]{impDimensions[0], impDimensions[1], impDimensions[3], impDimensions[4]};
		else 
			dimensions = new long[]{impDimensions[0], impDimensions[1], impDimensions[3]};
				
		// TODO: for the images with time have to make a loop
		
		
		Img<FloatType> imgInliers = new ArrayImgFactory<FloatType>().create(dimensions, new FloatType());
		Spot.drawRANSACArea(spots, imgInliers);
		
		ImageJFunctions.show(imgInliers);
	}
	
	public static void main(String[] args){
		
	}
	
	
}
