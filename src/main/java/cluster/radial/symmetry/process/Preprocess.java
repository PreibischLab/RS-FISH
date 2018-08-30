package cluster.radial.symmetry.process;

import io.scif.img.ImgSaver;

import java.io.File;
import java.util.ArrayList;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import gui.radial.symmetry.interactive.HelperFunctions;
import util.ImgLib2Util;
import util.MedianFilter;

public class Preprocess {

	public static float[] getMinmax(Img<FloatType> img, Img<FloatType> mask) {
		int numDimensions = img.numDimensions();
		float [] minmax = new float[2];

		final Cursor< FloatType > cursor = img.cursor();
		// FIXME: it is a hack after wrong deconvolution cropping
		RandomAccess<FloatType> ra = Views.extendMirrorSingle(mask).randomAccess();
		
		float currentMin = Float.MAX_VALUE;
		float currentMax = -Float.MAX_VALUE;
		
		long [] position = new long [numDimensions];

		while(cursor.hasNext()) {
			cursor.fwd();

			cursor.localize(position);
			ra.setPosition(new long[] {position[0], position[1]});

			if (ra.get().get() > 0){
				float currentValue = cursor.get().get();
				if (currentValue < currentMin)
					currentMin = currentValue;
				if (currentValue > currentMax)
					currentMax = currentValue;
			}
		}

		minmax[0] = currentMin;
		minmax[1] = currentMax;
		return minmax;
	}

	public static Img<FloatType> normalize( Img <FloatType> img, float fromMin, float fromMax, float toMin, float toMax){
		Img<FloatType> pImg = new ArrayImgFactory<FloatType>().create(img, new FloatType());

		final Cursor< FloatType > cursor = img.cursor();
		RandomAccess<FloatType> ra = pImg.randomAccess();

		// no roi in the image case
		if (fromMin == Float.MAX_VALUE || fromMax == -Float.MAX_VALUE) {
			fromMax = 1;
			fromMin = 0;
		}

		float scale = fromMax;
		scale -= fromMin;

		while(cursor.hasNext()) {
			cursor.fwd();
			ra.setPosition(cursor);

			float currentVal = cursor.get().get();
			currentVal -= fromMin;
			currentVal /= scale;

			// cursor.get().set(currentVal);
			ra.get().set(currentVal);
		}

		return pImg;
	}

	public static float getCenter(ArrayList<ImageData> centers, String filename) {
		float center = 1;
		// TODO: optimize
		for(ImageData id : centers)
			if (filename.equals(id.getFilename()))
				center = id.getCenter();
		return center;
	}

	// TODO: make all operations in place
	public static void firstStepPreprocess(File imgPath, File roiPath, File outputPath) {
		Img<FloatType> img = ImgLib2Util.openAs32Bit(imgPath.getAbsoluteFile());
		Img<FloatType> bg = new ArrayImgFactory<FloatType>().create(img, new FloatType());
		Img<FloatType> mask =  ImgLib2Util.openAs32Bit(roiPath.getAbsoluteFile());

		// 1. get the background
		int [] kernelDim = new int []{19, 19}; 
		MedianFilter.medianFilterSliced(img, bg, kernelDim);

		// 2. subtract the background
		HelperFunctions.subtractImg(img, bg);
		System.out.println("Median filtering done!");

		float medianMedianPerPlane = ExtraPreprocess.calculateMedianIntensity(img, mask);
		Img<FloatType> pImg = ExtraPreprocess.subtractValue(img, medianMedianPerPlane);
		System.out.println("Median of median done!");

		// 5. normalize image 
		float min = 0;
		float max = 1;

		// imp used for roi only
		float [] fromMinMax = getMinmax(pImg, mask);
		// IMP: decided to take min as 0
		fromMinMax[0] = 0;

		pImg = normalize(pImg, fromMinMax[0], fromMinMax[1], min, max);
		System.out.println("Normalization done!");

		// 4.* just resave the images at the moment
		try {
			new ImgSaver().saveImg(outputPath.getAbsolutePath(), pImg);
		}
		catch (Exception exc) {
			exc.printStackTrace();
		}
		System.out.println("Saving done!");
	}

	public static void secondStepPreprocess(File imgPath, File roiPath, File outputPath, float center) {
		Img<FloatType> img = ImgLib2Util.openAs32Bit(imgPath.getAbsoluteFile());
		Img<FloatType> mask = ImgLib2Util.openAs32Bit(roiPath.getAbsoluteFile());

		// 5. normalize image 
		float min = 0;
		float max = 1;

		// TODO: do I actually need this one 
		float [] fromMinMax = getMinmax(img, mask);

		fromMinMax[0] = 0;
		fromMinMax[1] = center; // - medianMedianPerPlane;

		Img<FloatType> pImg = normalize(img, fromMinMax[0], fromMinMax[1], min, max);
		System.out.println("Normalization done!");
		

		// 4.* just resave the images at the moment
		try {
			new ImgSaver().saveImg(outputPath.getAbsolutePath(), pImg);
		}
		catch (Exception exc) {
			exc.printStackTrace();
		}
		System.out.println("Saving done!");
	}

	public static void main(String [] args){
		File folder = new File("");
		// runPreprocess(folder);
	}

}
