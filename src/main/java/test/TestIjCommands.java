package test;

import java.io.File;

import ij.ImageJ;
import ij.ImagePlus;
import ij.io.Opener;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

public class TestIjCommands< T extends RealType<T> & NativeType<T>>{
	
	public void testIjCommands(ImagePlus imp){		
		int [] impDim = imp.getDimensions();
		long[] dim; // stores xyz dimensions
		if (impDim[3] == 1){ // if there is no z dimension -- 2D image
			dim = new long []{impDim[0], impDim[1]};
		}
		else { // 3D image
			dim = new long []{impDim[0], impDim[1], impDim[3]};
		}
		
		// TODO: use different image Constructors
		RandomAccessibleInterval<FloatType> img = ArrayImgs.floats(dim);
		
		for (int c = 1; c <= imp.getNChannels(); c++){
			imp.setC(c);
			for (int t = 1; t <= imp.getNFrames(); t++){
				imp.setT(t);
				// ImageJFunctions.show(ImageJFunctions.wrap(imp));

				System.out.println("imp: c, t: " + imp.getC() + " " + imp.getT());
		
				
				
			    // final Img< FloatType > rai = ImageJFunctions.wrap(imp); //HelperFunctions.toImg( imp, dim, type );		
			    // ImageJFunctions.show(rai);
				// RadialSymmetry rs = new RadialSymmetry(rsm, rai);
			}
		}
	}
	
	public static < T extends RealType<T> & NativeType<T> > void copyImg(ImagePlus imp, RandomAccessibleInterval<T> img, int [] impDim, long [] dim){
		RandomAccessibleInterval<T> rai = ImageJFunctions.wrap(imp);
		
		Cursor <T> cursorTo = Views.iterable(rai).localizingCursor();
		Cursor <T> cursorFrom = Views.iterable(img).localizingCursor();
		
		while (cursorTo.hasNext()){
			cursorTo.fwd();
			cursorFrom.fwd();
			
			long [] it = new long [dim.length];
			
			if ((it[0] + 1)*(it[1] + 1) % dim[0]*dim[1] == 0){
				// shift 3rd dim 
			}
			
		}
}
	
	public static void main(String[] args){
		new ImageJ();
		
		File path = new File( "/home/milkyklim/Desktop/test-image.tif" );
		// path = path.concat("test_background.tif");

		if ( !path.exists() )
			throw new RuntimeException( "'" + path.getAbsolutePath() + "' doesn't exist." );

		new ImageJ();
		System.out.println( "Opening '" + path + "'");

		ImagePlus imp = new Opener().openImage( path.getAbsolutePath() );

		if (imp == null)
			throw new RuntimeException( "image was not loaded" );

		imp.show();
		
		new TestIjCommands().testIjCommands(imp);
		
		System.out.println("Doge!");
	}
}
