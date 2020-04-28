package process.radialsymmetry.cluster;

import java.awt.Rectangle;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import gui.interactive.HelperFunctions;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.algorithm.stats.Normalize;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.real.FloatType;
import parameters.GUIParams;
import util.ImgLib2Util;
import util.MedianFilter;

public class Preprocess {
	// String path - path to the folder with the images
	// String ext - image extension
	public static ArrayList<File> readFolder(File folder, String ext) {
		ArrayList<File> images= new ArrayList<>();
		System.out.println("Grab images from " + folder.getAbsolutePath());		
		for (File file : folder.listFiles())
			// if the file is not hidden and ends with .tif we take it 
			if (file.isFile() && !file.getName().startsWith(".") && file.getName().endsWith(ext))
				images.add(file);
		return images;
	}

	public static void runPreprocess(File pathImages, File pathImagesRoi, File pathImagesMedian, File pathDb, File pathCenters, int centerIndex) {
		// parse the db with smFish labels and good looking images
		ArrayList<ImageData> imageData = readDb(pathDb);

		// parse the centers of the histograms
		ArrayList<ImageData> centerData = null;
		if (centerIndex == 2)
			centerData = readCenters(pathCenters);

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

	public static ArrayList<ImageData> readCenters(File filePath) {
		ArrayList <ImageData> imageData = new ArrayList<>(); 
		final int nColumns = 2;

		CSVReader reader = null;
		String[] nextLine = new String [nColumns];

		try {
			int toSkip = 1; 
			reader = new CSVReader(new FileReader(filePath), ',', CSVWriter.DEFAULT_QUOTE_CHARACTER, toSkip);
			// while there are rows in the file
			while ((nextLine = reader.readNext()) != null) {
				String filename = nextLine[0];
				float center = Float.parseFloat(nextLine[1]);
				imageData.add(new ImageData(filename, center));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return imageData;
	}

	public static ArrayList <ImageData> readDb(File databasePath) {

		ArrayList <ImageData> imageData = new ArrayList<>(); 
		// some constants
		final int nColumns = 24;
		// columns ids: 3, 6, 9, 12, 15 
		final int [] lambdaIndices = new int[] {2, 5, 8, 11, 14};
		final int [] stainIndices = new int[] {3, 6, 9, 12, 15};
		final int qualityIndex = 20;
		final int stageIndex = 19;
		final int integrityIndex = 18;
		final int signalIndex = 17;
		// index for the column with the new name
		final int newFilenameIndex = 23;

		CSVReader reader = null;
		String[] nextLine = new String [nColumns];

		try {
			int toSkip = 1; 
			reader = new CSVReader(new FileReader(databasePath), ',', CSVWriter.DEFAULT_QUOTE_CHARACTER, toSkip);
			// while there are rows in the file
			while ((nextLine = reader.readNext()) != null) {
				// iterate over the row; that is 25 elements long
				for (int j = 0; j < stainIndices.length; j++) {
					if (nextLine[stainIndices[j]].equals("FISH") && conditionalPick(nextLine, signalIndex, integrityIndex, stageIndex, qualityIndex)) {
						// TODO: Check the naming for the files!
						// files.add(new String(nextLine[newFilenameIndex] + "-C" + j));

						int lambda = Integer.parseInt(nextLine[lambdaIndices[j]]);
						String filename = "C" + (j + 1) + "-"+ nextLine[newFilenameIndex];
						boolean defects = !nextLine[qualityIndex].equals(""); // empty string means no defect

						imageData.add(new ImageData(lambda, defects, filename));
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		//		for (ImageData id : imageData)
		//			System.out.println(id.getLambda() + " " + id.getFilename());

		System.out.println("Done reading the databse");
		return imageData;
	}

	// drop bad quality images
	public static boolean conditionalPick(String[] nextLine, int signalIndex, int integrityIndex, int stageIndex, int qualityIndex) {
		// true -> good quality
		// some parameters that are coming from Laura: 
		// integrity == 1 
		// signal in {3,4,5}
		boolean isGood = false;
		// if (!nextLine[qualityIndex].trim().equals("")) 

		int signalQuality = nextLine[signalIndex].trim().equals("") ? 0 : Integer.parseInt(nextLine[signalIndex].trim());
		int integrity = nextLine[integrityIndex].trim().equals("") ? 0 : Integer.parseInt(nextLine[integrityIndex].trim());

		if (signalQuality >= 3 // good signal
				&& integrity == 1 // embryo looks fine
				&& nextLine[stageIndex].trim().equals("E") // only embryos
				&& !nextLine[qualityIndex].trim().contains("z jumps") // skip
				&& !nextLine[qualityIndex].trim().contains("z cut") // skip
				)
			isGood = true;
		return isGood;
	}
	
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
		ArrayList<ImageData> imageData = Preprocess.readDb(pathDb);

		// to see the feedback
		long currentIdx = 0;
		for (ImageData imageD : imageData) {
			currentIdx++;
			// unprocessed path
			String inputImagePath = pathImages.getAbsolutePath() + "/" + imageD.getFilename() + ".tif";
			// processed path 
			String outputImagePath = pathImagesMedian.getAbsolutePath() + "/" + imageD.getFilename() + ".tif";
			// roi path 
			String roiImagePath = pathImagesRoi.getAbsolutePath() + "/" + imageD.getFilename().substring(3) + ".tif";

			System.out.println( currentIdx + "/" + imageData.size() + ": " + inputImagePath);
			// System.out.println(outputImagePath);
			// System.out.println(roiImagePath);

			// check that the corresponding files is not missing
			if (new File(inputImagePath).exists() && new File(roiImagePath).exists()) {
				firstStepPreprocess(new File(inputImagePath), new File(roiImagePath), new File(outputImagePath));
			}
			else {
				System.out.println("Preprocess.java: " + inputImagePath + " file is missing");
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
		ArrayList<ImageData> imageData = Preprocess.readDb(pathDb);
		// grab the values of the centers
		ArrayList<ImageData> centers = Preprocess.readCenters(pathCenters);
		
		// to see the feedback
		long currentIdx = 0;
		for (ImageData imageD : imageData) {
			currentIdx++;
			// unprocessed path
			String inputImagePath = pathImages.getAbsolutePath() + "/" + imageD.getFilename() + ".tif";
			// processed path 
			String outputImagePath = pathImagesMedian.getAbsolutePath() + "/" + imageD.getFilename() + ".tif";
			// roi path 
			String roiImagePath = pathImagesRoi.getAbsolutePath() + "/" + imageD.getFilename().substring(3) + ".tif";
			// peak center value
			float center = getCenter(centers, imageD.getFilename());

			System.out.println( currentIdx + "/" + imageData.size() + ": " + inputImagePath);
			// System.out.println(outputImagePath);
			// System.out.println(roiImagePath);

			// check that the corresponding files is not missing
			if (new File(inputImagePath).exists()) {
				secondStepPreprocess(new File(inputImagePath), new File(roiImagePath), new File(outputImagePath), center);
			}
			else {
				System.out.println("Preprocess.java: " + inputImagePath + " file is missing");
			}
		}
	}
	
	// 
	public static void firstStepPreprocess(File imgPath, File roiPath, File outputPath) {
		Img<FloatType> img = ImgLib2Util.openAs32Bit(imgPath.getAbsoluteFile());
		Img<FloatType> bg = new ArrayImgFactory<FloatType>().create(img, new FloatType());
		ImagePlus imp = IJ.openImage(roiPath.getAbsolutePath());

		// 1. get the background
		int [] kernelDim = new int []{19, 19}; 
		MedianFilter.medianFilterSliced(img, bg, kernelDim);
		
		// 2. subtract the background
		HelperFunctions.subtractImg(img, bg);
		System.out.println("Median filtering done!");

		// 3. calculate median per median

		// this one is used only to reduce the amount of code re-factoring
		ImagePlus tmpImg = ImageJFunctions.wrap(img, "");
		tmpImg.setDimensions(1, imp.getNSlices(), 1);
		Roi roi = imp.getRoi();
		if (roi != null)
			tmpImg.setRoi(roi);
		else
			System.out.println("There was the probelm with roi in " + imgPath.getAbsolutePath());

		float medianMedianPerPlane = ExtraPreprocess.calculateMedianIntensity(tmpImg);
		Img<FloatType> pImg = ExtraPreprocess.subtractValue(img, medianMedianPerPlane);
		System.out.println("Median of median done!");

		// 5. normalize image 
		float min = 0;
		float max = 1;
		
		// imp used for roi only
		float [] fromMinMax = getMinmax(pImg, imp);
		// IMP: decided to take min as 0
		fromMinMax[0] = 0;
		
		pImg = normalize(pImg, fromMinMax[0], fromMinMax[1], min, max);
		System.out.println("Normalization done!");
		
		// 4.* just resave the images at the moment
		IOFunctions.saveResult(pImg, outputPath.getAbsolutePath());
		System.out.println("Saving done!");
	}

	public static void secondStepPreprocess(File imgPath, File roiPath, File outputPath, float center) {
		Img<FloatType> img = ImgLib2Util.openAs32Bit(imgPath.getAbsoluteFile());
		// Img<FloatType> bg = new ArrayImgFactory<FloatType>().create(img, new FloatType());
		ImagePlus imp = IJ.openImage(roiPath.getAbsolutePath());
		// 1. get the background
//		int [] kernelDim = new int []{19, 19}; 
//		MedianFilter.medianFilterSliced(img, bg, kernelDim);
		
		// 2. subtract the background
//		HelperFunctions.subtractImg(img, bg);
//		System.out.println("Median filtering done!");

		// 3. calculate median per median

		// this one is used only to reduce the amount of code re-factoring
		ImagePlus tmpImg = ImageJFunctions.wrap(img, "");
		tmpImg.setDimensions(1, imp.getNSlices(), 1);
		Roi roi = imp.getRoi();
		if (roi != null)
			tmpImg.setRoi(roi);
		else
			System.out.println("There was the probelm with roi in " + imgPath.getAbsolutePath());

//		float medianMedianPerPlane = ExtraPreprocess.calculateMedianIntensity(tmpImg);
//		Img<FloatType> pImg = ExtraPreprocess.subtractValue(img, medianMedianPerPlane);
//		System.out.println("Median of median done!");

		// 5. normalize image 
		float min = 0;
		float max = 1;
		
		// imp used for roi only
		// TODO: do I actually need this one 
		float [] fromMinMax = getMinmax(img, imp);
		
		fromMinMax[0] = 0;
		fromMinMax[1] = center; // - medianMedianPerPlane;
		
		Img<FloatType> pImg = normalize(img, fromMinMax[0], fromMinMax[1], min, max);
		System.out.println("Normalization done!");
		
		// ImageJFunctions.show(pImg).setTitle(imgPath.getAbsolutePath());
		
		// 4.* just resave the images at the moment
		IOFunctions.saveResult(pImg, outputPath.getAbsolutePath());
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
		IOFunctions.saveResult(img, outputPath.getAbsolutePath());

		// perform the processing steps from above
	}

	public static void main(String [] args){
		File folder = new File("");
		// runPreprocess(folder);
	}

}
