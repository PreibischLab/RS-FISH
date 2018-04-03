package process.radialsymmetry.cluster;

import java.awt.Rectangle;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

import gui.interactive.HelperFunctions;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.algorithm.stats.Normalize;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.real.FloatType;
import parameters.GUIParams;
import util.ImgLib2Util;
import util.MedianFilter;
import util.opencsv.CSVReader;

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

	public static void runPreprocess(File pathImages, File pathImagesRoi, File pathImagesMedian, File pathDb) {
		// parse the db with smFish labels and good looking images
		ArrayList<ImageData> imageData = readDb(pathDb);
		// to see the feedback
		long currentIdx = 0;
		
		boolean flag = false;
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
			
			if (!inputImagePath.equals("/home/milkyklim/Desktop/2018-04-03-laura-radial-symmetry-numbers/N2-channels-correct/C1-N2_241.tif")) {
				// 
			}
			else {
				flag = true;
			}
			
			if (!flag) continue;
			
			// check that the corresponding files is not missing
			if (new File(inputImagePath).exists() && new File(roiImagePath).exists()) {
				// run full stack preprocess
				preprocessImage(new File(inputImagePath), new File(roiImagePath), new File(outputImagePath));
			}
			else {
				System.out.println("Preprocess.java: " + inputImagePath + " file is missing");
			}
		}
	}

// MIGHT BE out-dated	
//	public static void runPreprocess1(File pathImages, File pathImagesRoi, File pathImagesMedian, File pathDb) {
//		// TODO: Remove this one it is not necessary
//		// grab all file paths to the images in the folder
//		// ArrayList<File> paths = readFolder(pathImages, ".tif");
//		
//		// parse the db with smFish labels and good looking images
//		ArrayList<ImageData> imageData = readDb(pathDb);
//
//		for (ImageData imageD : imageData) {
//			// unprocessed path
//			String inputImagePath = pathImages.getAbsolutePath() + "/" + imageD.getFilename() + ".tif";
//			// processed path 
//			String outputImagePath = pathImagesMedian.getAbsolutePath() + "/" + imageD.getFilename() + ".tif";
//
//			// check that the corresponding files is not missing
//			if (new File(inputImagePath).exists()) {
//
//				// File outputPath = new File(imgPath.getAbsolutePath().substring(0, imgPath.getAbsolutePath().length() - 4));
//
//				// File outputPath = new File(oFolder.getAbsolutePath() + "/" + imgPath.getName()); 
//				// System.out.println(outputPath.getAbsolutePath());
//
//				// IMP: 1 stage where we drop dome of the images
//				// we don't take into account those that are you marked as smFISH signal
//				System.out.println(inputImagePath + " : " + imageD.getLambda());
//				// remove the params 
//				preprocessImage(new File(inputImagePath), new File(outputImagePath));
//				// continue with 
//				// 5. run radial symmetry 
//				// 6. filter the spot and save them 
//				
//				// output path for csv
//				
//				String outputPathCsv = pathImagesMedian.getAbsolutePath() + "/" + imageD.getFilename() + ".csv";
//				GUIParams params = setParameters(imageD.getLambda());
//				// FIX THE ROI FOLDER
//				// BatchProcess.runProcess(inputPath, params, new File(outputPathCsv) );
//			}
//		}
//		new ImageJ();
//		// iterate over each image in the folder
//	}

	public static ArrayList <ImageData> readDb(File databasePath) {

		ArrayList <ImageData> imageData = new ArrayList<>(); 
		// some constants
		final int nColumns = 24;
		// columns ids: 3, 6, 9, 12, 15 
		final int [] lambdaIndices = new int[] {2, 5, 8, 11, 14};
		final int [] stainIndices = new int[] {3, 6, 9, 12, 15};
		final int qualityIndex = 20;
		// index for the column with the new name
		final int newFilenameIndex = 23;

		CSVReader reader = null;
		String[] nextLine = new String [nColumns];

		try {
			int toSkip = 1; 
			reader = new CSVReader(new FileReader(databasePath), ',', CSVReader.DEFAULT_QUOTE_CHARACTER, toSkip);
			// while there are rows in the file
			while ((nextLine = reader.readNext()) != null) {
				// iterate over the row; that is 25 elements long
				for (int j = 0; j < stainIndices.length; j++) {
					if (nextLine[stainIndices[j]].equals("FISH") && conditionalPick(nextLine, qualityIndex)) {
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

// REMOVE: out-dated
//	// verify that the given image is smFish
//	public static void checkThatTheImageIsSmFish(File databasePath) {
//		// TODO:
//		// iterate over xls 
//		// iterate over the columns 
//		// if column is smfish add the file to the process file set 
//		// otherwise ignore
//
//		// TODO: always check the input folder 
//		// File path = new File("/Users/kkolyva/Desktop/smFISH-database/SEA-12-Table 1.csv");
//
//		// files that we will consider in the end
//		// ArrayList <String> files = new ArrayList<>();
//		// TODO: add the length wave check here, too
//		ArrayList <ImageData> imageData = new ArrayList<>(); 
//
//		CSVReader reader = null;
//		int nColumns = 24;
//		String[] nextLine = new String [nColumns];
//		// columns: 3, 6, 9, 12, 15 
//		int [] lambdaIndices = new int[] {2, 5, 8, 11, 14};
//		int [] stainIndices = new int[] {3, 6, 9, 12, 15};
//		final int qualityIndex = 20;
//		// index for the column with the new name
//		int newFilenameIndex = 23;
//
//		try {
//			int toSkip = 1; 
//			reader = new CSVReader(new FileReader(databasePath), ',', CSVReader.DEFAULT_QUOTE_CHARACTER, toSkip);
//			// while there are rows in the file
//			while ((nextLine = reader.readNext()) != null) {
//				// iterate over the row; that is 25 elements long
//				for (int j = 0; j < stainIndices.length; j++) {
//					// 
//					if (nextLine[stainIndices[j]].equals("FISH") && conditionalPick(nextLine, qualityIndex)) {
//						// TODO: Check the naming for the files!
//						// files.add(new String(nextLine[newFilenameIndex] + "-C" + j));
//
//						int lambda = Integer.parseInt(nextLine[lambdaIndices[j]]);
//						String filename = nextLine[newFilenameIndex] + "-C" + j;
//
//						imageData.add(new ImageData(lambda, false, filename));
//					}
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//
//		for (ImageData id : imageData)
//			System.out.println(id.getLambda() + " " + id.getFilename());
//	}

	// drop bad quality images
	public static boolean conditionalPick(String[] nextLine, int qualityIndex) {
		// true -> good quality
		boolean isGood = true;
		if (!nextLine[qualityIndex].trim().equals("")) 
			isGood = false;
		return isGood;
	}
	
	// use only pixels that are inside roi for normalization 
	public static void normalize( Img <FloatType> img, ImagePlus imp, float min, float max )
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
		
		// no roi in the image case
		if (currentMin == Float.MAX_VALUE || currentMax == -Float.MAX_VALUE) {
			currentMax = 1;
			currentMin = 0;
		}
		
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

	public static void preprocessImage(File imgPath, File roiPath, File outputPath){
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
		// normalize ove roi only
		normalize(img, imp, min, max);
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
