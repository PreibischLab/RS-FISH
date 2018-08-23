package cluster.radial.symmetry.process;

import java.awt.Rectangle;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import radial.symmetry.parameters.GUIParams;
import radial.symmetry.utils.IOUtils;
import io.scif.img.ImgSaver;

import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.algorithm.stats.Normalize;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import gui.radial.symmetry.interactive.HelperFunctions;
import util.ImgLib2Util;
import util.MedianFilter;
import util.NotSoUsefulOutput;
import util.opencsv.CSVReader;

public class Preprocess {

	public static void runPreprocess(File pathImages, File pathImagesRoi, File pathImagesMedian, File pathDb, File pathCenters, int centerIndex) {
		// parse the db with smFish labels and good looking images
		ArrayList<ImageData> imageData = IOUtils.readDb(pathDb);

		// parse the centers of the histograms
		ArrayList<ImageData> centerData = null;
		if (centerIndex == 2)
			centerData = IOUtils.readCenters(pathCenters);

		// to see the feedback
		long currentIdx = 0;
		for (ImageData imageD : imageData) {
			currentIdx++;			
			// unprocessed path
			String inputImagePath = pathImages.getAbsolutePath() + "/" + imageD.getFilename() + ".tif";
			// processed path 
			String outputImagePath = pathImagesMedian.getAbsolutePath() + "/" + imageD.getFilename() + ".tif";
			// image that contains roi 
			String roiImagePath = pathImagesRoi.getAbsolutePath() + "/" + imageD.getFilename().substring(3) + ".tif";

			System.out.println( currentIdx + "/" + imageData.size() + ": " + inputImagePath);
			// System.out.println(outputImagePath);
			// System.out.println(roiImagePath);

			// check that the corresponding files is not missing
			if (new File(inputImagePath).exists() && new File(roiImagePath).exists()) {

				float normMax = 1; // default value is 1; corresponds to normal workflow

				boolean isInList = false;
				// check that the image is in histogram centers
				if (centerIndex == 2) {
					// find the intensity value in the list
					for (ImageData items : centerData) {
						if (imageD.getFilename().equals(items.getFilename())){
							normMax = items.getCenter();
							isInList = true; 
							break;
						}
					}
					if (!isInList)
						System.out.println("Could not find " + imageD.getFilename() + " in the list.");
				}


				// run full stack preprocess
				// FIXME: THIS ONE IS NOT WORKING PROPERLY!
				// if (centerIndex == 2 && isInList)
				if (centerIndex == 2 || true) {
					System.out.println("------------PROCESSING!-----------");
					preprocessImage(new File(inputImagePath), new File(roiImagePath), new File(outputImagePath), normMax);
				}
			}
			else {
				System.out.println("Preprocess.java: " + inputImagePath + " file is missing");
				System.out.println("Preprocess.java: " + roiImagePath + " file is missing");
			}
		}
	}

	// TODO: OLD: REMOVE
	public static float[] getMinmax(Img <FloatType> img, ImagePlus imp) {
		float [] minmax = new float[2];

		ImageProcessor ip = imp.getMask();
		Rectangle bounds = imp.getRoi().getBounds();
		// System.out.println(ip.getWidth() + " : " + ip.getHeight());
		// System.out.println(imp.getWidth() + " : " + imp.getHeight());

		System.out.println(bounds.x + " : " + bounds.y);

		final Cursor< FloatType > cursor = img.cursor();
		float currentMin = Float.MAX_VALUE;
		float currentMax = -Float.MAX_VALUE;

		while(cursor.hasNext()) {
			cursor.fwd();

			int x = cursor.getIntPosition(0) - bounds.x;
			int y = cursor.getIntPosition(1) - bounds.y;

			if (ip != null && ip.getPixel(x, y) != 0) {
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

	public static float[] getMinmax(Img<FloatType> img, Img<FloatType> mask) {
		int numDimensions = img.numDimensions();
		float [] minmax = new float[2];

		final Cursor< FloatType > cursor = img.cursor();
		RandomAccess<FloatType> ra = mask.randomAccess();
		
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
		// System.out.println(ip.getWidth() + " : " + ip.getHeight());
		// System.out.println(imp.getWidth() + " : " + imp.getHeight());

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

	// FIXME: old one; remove
	// use only pixels that are inside roi for normalization 
	public static void normalize( Img <FloatType> img, ImagePlus imp, float min, float max, float center)
	{	
		ImageProcessor ip = imp.getMask();
		Rectangle bounds = imp.getRoi().getBounds();
		// System.out.println(ip.getWidth() + " : " + ip.getHeight());
		// System.out.println(imp.getWidth() + " : " + imp.getHeight());

		System.out.println(bounds.x + " : " + bounds.y);

		final Cursor< FloatType > cursor = img.cursor();
		float currentMin = Float.MAX_VALUE;
		float currentMax = -Float.MAX_VALUE;

		while(cursor.hasNext()) {
			cursor.fwd();

			int x = cursor.getIntPosition(0) - bounds.x;
			int y = cursor.getIntPosition(1) - bounds.y;

			if (ip != null && ip.getPixel(x, y) != 0) {
				float currentValue = cursor.get().get();
				if (currentValue < currentMin)
					currentMin = currentValue;
				if (currentValue > currentMax)
					currentMax = currentValue;
			}
		}

		// IMP!: decided to use image min value as 0 
		// not the minimum of the image
		currentMin = 0;

		// no roi in the image case
		if (currentMin == Float.MAX_VALUE || currentMax == -Float.MAX_VALUE) {
			currentMax = 1;
			currentMin = 0;
		}

		if (center > 0)
			currentMax = (currentMax - currentMin)*center - currentMin;


		float scale = currentMax;
		scale -= currentMin;

		cursor.reset();
		while(cursor.hasNext()) {
			cursor.fwd();

			float currentVal = cursor.get().get();
			currentVal -= currentMin;
			currentVal /= scale;
			cursor.get().set(currentVal);
		}
	}

	// the =1 values that we will use for the second run of the radial symmetry
	public static float findMaxValue(Img <FloatType> img, ImagePlus imp, float center) {
		// take the corresponding image name 
		// pull the corresponding value
		// profit

		ImageProcessor ip = imp.getMask();
		Rectangle bounds = imp.getRoi().getBounds();

		System.out.println(bounds.x + " : " + bounds.y);

		final Cursor< FloatType > cursor = img.cursor();
		float currentMin = Float.MAX_VALUE;
		float currentMax = -Float.MAX_VALUE;

		while(cursor.hasNext()) {
			cursor.fwd();

			int x = cursor.getIntPosition(0) - bounds.x;
			int y = cursor.getIntPosition(1) - bounds.y;

			if (ip != null && ip.getPixel(x, y) != 0) {
				float currentValue = cursor.get().get();
				if (currentValue < currentMin)
					currentMin = currentValue;
				if (currentValue > currentMax)
					currentMax = currentValue;
			}
		}

		float newMaxValue = (currentMax - currentMin)*center + currentMin;
		return newMaxValue;
	}

	// reads the corresponding value for the center of the peak and returns it here 
	public static float findCenterValue() {
		float center = 1; // default value 


		return center;
	}


	public static void runFirstStepPreprocess(File pathImages, File pathDb, File pathImagesRoi, File pathImagesMedian) {
		// parse the db with smFish labels and good looking images
		ArrayList<ImageData> imageData = IOUtils.readDb(pathDb);

		// to see the feedback
		long currentIdx = 0;
		String ext = ".tif";
		String classname = Preprocess.class.getSimpleName();
		
		for (ImageData imageD : imageData) {
			currentIdx++;
			// unprocessed path
			File inputImageFile = Paths.get(pathImages.getAbsolutePath(), imageD.getFilename(), ext).toFile();
			// processed path 
			File outputImageFile = Paths.get(pathImagesMedian.getAbsolutePath(), imageD.getFilename(), ext).toFile();
			// roi path 
			File maskFile = Paths.get(pathImagesRoi.getAbsolutePath(), imageD.getFilename().substring(3), ext).toFile();

			System.out.println(NotSoUsefulOutput.toProgressString(currentIdx, imageData.size(), inputImageFile.getAbsolutePath()));
			// TODO: debug only remove once down with this part
			// System.out.println(outputImagePath);
			// System.out.println(roiImagePath);

			// check that the corresponding files is not missing
			if (inputImageFile.exists() && maskFile.exists()) {
				firstStepPreprocess(inputImageFile, maskFile, outputImageFile);
			}
			else {
				System.out.println(NotSoUsefulOutput.toComplaintString(classname, inputImageFile.getAbsolutePath()));
			}
		}
	}

	public static float getCenter(ArrayList<ImageData> centers, String filename) {
		float center = 1;

		for(ImageData id : centers)
			if (filename.equals(id.getFilename()))
				center = id.getCenter();

		return center;
	}

	public static void runSecondStepPreprocess(File pathImages, File pathDb, File pathImagesRoi, File pathCenters, File pathImagesMedian) {
		// parse the db with smFish labels and good looking images
		ArrayList<ImageData> imageData = IOUtils.readDb(pathDb);
		// grab the values of the centers
		ArrayList<ImageData> centers = IOUtils.readCenters(pathCenters);

		// to see the feedback
		long currentIdx = 0;
		String ext = ".tif";
		String classname = Preprocess.class.getSimpleName();
		
		for (ImageData imageD : imageData) {
			currentIdx++;
			// unprocessed path
			File inputImageFile = Paths.get(pathImages.getAbsolutePath(), imageD.getFilename(), ext).toFile();
			// processed path 
			File outputImageFile = Paths.get(pathImagesMedian.getAbsolutePath(), imageD.getFilename(), ext).toFile();
			// roi path 
			File maskFile = Paths.get(pathImagesRoi.getAbsolutePath(), imageD.getFilename().substring(3), ext).toFile();
			// peak center value
			float center = getCenter(centers, imageD.getFilename());

			System.out.println(NotSoUsefulOutput.toProgressString(currentIdx, imageData.size(), inputImageFile.getAbsolutePath()));
			// System.out.println(outputImagePath);
			// System.out.println(roiImagePath);

			// check that the corresponding files is not missing
			if (inputImageFile.exists() && maskFile.exists()) {
				secondStepPreprocess(inputImageFile, maskFile, outputImageFile, center);
			}
			else {
				System.out.println(NotSoUsefulOutput.toComplaintString(classname, inputImageFile.getAbsolutePath()));
			}
		}
	}

	// 
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


	public static void preprocessImage(File imgPath, File roiPath, File outputPath, float newMax){
		Img<FloatType> img = ImgLib2Util.openAs32Bit(imgPath.getAbsoluteFile());
		Img<FloatType> bg = new ArrayImgFactory<FloatType>().create(img, new FloatType());
		ImagePlus imp = IJ.openImage(roiPath.getAbsolutePath());

		// 1. get the background
		// TODO: move kernel size as the parameter
		int [] kernelDim = new int []{19, 19}; 
		MedianFilter.medianFilterSliced(img, bg, kernelDim);

		// 2. subtract the background
		HelperFunctions.subtractImg(img, bg);

		// 3. run the validation step here 
		boolean isValid = true;
		if (!isValid) return;

		// 4. normalize image 
		float min = 0;
		float max = 1;

		// normalize over roi only
		normalize(img, imp, min, max, newMax);
		// Normalize.normalize(img, new FloatType(min), new FloatType(max));

		// 4.* just resave the images at the moment
		try {
			new ImgSaver().saveImg(outputPath.getAbsolutePath(), img);
		}
		catch (Exception exc) {
			exc.printStackTrace();
		}
		System.out.println("Saving done!");
		// perform the processing steps from above
	}

	public static void main(String [] args){
		File folder = new File("");
		// runPreprocess(folder);
	}

}
