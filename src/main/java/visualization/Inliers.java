package visualization;

import java.util.ArrayList;

import fit.Spot;
import ij.CompositeImage;
import ij.ImagePlus;
import ij.ImageStack;

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
	
	// ImagePlus imp; // initial image
	
	public Inliers(){
		
	}
		
	public static void showInliers(RandomAccessibleInterval<FloatType> img, ArrayList<Spot> spots)
	{
		long [] dimensions = new long [img.numDimensions()];
		img.dimensions(dimensions);
	
		// TODO: doesn't fix all the cases! 
		if (dimensions.length == 5)
			System.out.println("only 1 channel images are supported");
				
		// FIXME: for the images with time have to make a loop
		
		final boolean encodeErrors = false;
		Img<FloatType> imgInliers = new ArrayImgFactory<FloatType>().create(dimensions, new FloatType());
		
		Spot.drawRANSACArea(spots, imgInliers, encodeErrors);
		// TODO: (re)-move 
		ImageJFunctions.show(imgInliers).setTitle("RANSAC inliers");
		
		final ImagePlus imp = ImageJFunctions.wrap(img, "input" ).duplicate();
		final ImagePlus impInliers = ImageJFunctions.wrap(imgInliers, "inliers" ).duplicate();

		final ImageStack stack = new ImageStack(imp.getWidth(), imp.getHeight() );
		
		for ( int i = 0; i < imp.getStackSize(); ++i )
		{
			stack.addSlice( imp.getStack().getProcessor( i + 1 ) );
			stack.addSlice( impInliers.getStack().getProcessor( i + 1 ) );
		}
		
		ImagePlus merged = new ImagePlus("merge", stack );
		merged.setDimensions( 2, imp.getStack().getSize(), 1 );
		
		CompositeImage ci = new CompositeImage( merged );
		ci.setDisplayMode( CompositeImage.COMPOSITE );
		ci.show();
	}
	
	
	public static void main(String[] args){
		
	}
	
	
}
