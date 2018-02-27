package process.radialsymmetry.cluster;

import java.io.File;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.real.FloatType;

import ij.ImageJ;
import mpicbg.util.RealSum;
import util.ImgLib2Util;

public class Preprocess {
	// class to preporcess images that will be used for radial symmetry
	// - median filter + background subtraction 
	// - illumination check (intensities between planes should not change drastically)
	// - normalize the image if valid 
	// - run radial symmetry on the resulting image
	// - gauss fit on the intensity values 
	// - take only spots that are in the given region
	// - save as csv?

	
	public static void readFolder() {
		
	}
	
	public static void runPreprocess() {
		// grab all file path to the images in the folder
		new ImageJ();
		Img<FloatType> img = ImgLib2Util.openAs32Bit(new File("/Users/kkolyva/Downloads/test.tif"));//path + file + ".tif"));
		
		
		// iterate over each image in the folder
		
		// perform the processing steps from above
	}
	
	public static void preprocessImage(){
		new ImageJ();
		String path = "";
		String file = "";
		Img<FloatType> img = ImgLib2Util.openAs32Bit(new File("/Volumes/Samsung_T3/2017-04-25-beads/cropped/tif-14-c.tif"));//path + file + ".tif"));
		Img<FloatType> bg  = ImgLib2Util.openAs32Bit(new File("/Volumes/Samsung_T3/2017-04-25-beads/cropped/bg/tif-14-c-m.tif"));//path + "bg/" + file + "-m.tif"));
		
		double average = getImageAverage(bg);	
		System.out.println(average);
		
		subtractValue(bg, average);
		subtractImg(img, bg);	
		
		ImageJFunctions.show(img);
		
		// ImagePlus towrite = ImageJFunctions.wrap(img, "hi").duplicate();
		// new FileSaver(towrite).saveAsTiffStack( "file.tif");
	}
		
	public static void subtractValue(Img<FloatType> bg, double value){			
		Cursor<FloatType> cursor = bg.cursor();
		
		while(cursor.hasNext()){
			cursor.fwd();
			float val = (float) (cursor.get().get() - value);
			cursor.get().set(val);
		}
	}
	
	public static double getImageAverage(Img<FloatType> bg){
		// compute average over all pixels
		double sum = sumImage(bg);
		for (int d = 0; d < bg.numDimensions(); ++d)
			sum /= bg.dimension(d);
		return sum;
	}
	
	public static double sumImage(Img<FloatType> img)
	{
		final RealSum sum = new RealSum();		
		for ( final FloatType t : img )
			sum.add( t.get() );
		return sum.getSum();
	}
	
	public static void subtractImg(Img<FloatType> img, Img<FloatType> bg){
		Cursor<FloatType> cursor = img.cursor();
		RandomAccess<FloatType> ra = bg.randomAccess();
		
		while(cursor.hasNext()){
			cursor.fwd();		
			ra.setPosition(cursor);			
			float val = cursor.get().get() - ra.get().get();			
			cursor.get().set(val);
		}
	}
	
	public static void main(String [] args){
		preprocessImage();
	}
	
}
