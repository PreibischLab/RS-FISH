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

	public static void showInliers(ImagePlus imagePlus, ArrayList<Spot> spots)
	{
		// figure put if we have xy or xyz image
		int numDimensions = imagePlus.getNSlices() == 1 ? 2 : 3; 
		long [] dimensions = new long [numDimensions];

		// TODO: doesn't fix all the cases! 
		if (dimensions.length == 5)
			System.out.println("only 1 channel images are supported");

		// FIXME: for the images with time have to make a loop

		final boolean encodeErrors = false;
		Img<FloatType> imgInliers = new ArrayImgFactory<FloatType>().create(dimensions, new FloatType());

		Spot.drawRANSACArea(spots, imgInliers, encodeErrors);
		// TODO: (re)-move 
		ImageJFunctions.show(imgInliers).setTitle("RANSAC inliers");

		final ImagePlus imp = imagePlus.duplicate();
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

	public static void showInliersNew(ImagePlus imagePlus, ArrayList<Spot> spots)
	{ 
		if (imagePlus.getNChannels() != 1)
			System.out.println("only 1 channel images are supported");

		// figure put if we have xy or xyz image
		int numDimensions = imagePlus.getNSlices() == 1 ? 2 : 3; 
		long [] dimensions = new long [numDimensions];
		// grab the full imagePlus 
		// take only only slice without comying if possible 
		// shift the view so that it starts at (0.0)
		// remove all the singular dimensions 
		final ImageStack stack = new ImageStack(imagePlus.getWidth(), imagePlus.getHeight() );
		final boolean encodeErrors = false;
		// That was the full image with the inliers 
		Img<FloatType> imgInliers = new ArrayImgFactory<FloatType>().create(imagePlus.getDimensions(), new FloatType());

		// go over all time steps and return
		// xy or xyz slice
		for (int t = 0; t < imagePlus.getNFrames(); ++t){
			// get the correct slice 
			
			
		}

			
			Spot.drawRANSACArea(spots, imgInliers, encodeErrors);
			// TODO: (re)-move 
			// ImageJFunctions.show(imgInliers).setTitle("RANSAC inliers");

			final ImagePlus imp = imagePlus.duplicate();
			final ImagePlus impInliers = ImageJFunctions.wrap(imgInliers, "inliers" ).duplicate();

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
