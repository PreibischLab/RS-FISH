package process.radialsymmetry.cluster;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

import gui.interactive.HelperFunctions;
import ij.ImageJ;
import net.imglib2.algorithm.stats.Normalize;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
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

	public static void runPreprocess(File iFolder, File oFolder, File databasePath) {
		// set the parameters that we define manually first
		boolean useRANSAC = true;
		final GUIParams params = new GUIParams();
		// apparently the best value for now
		params.setAnisotropyCoefficient(1.09f);
		params.setRANSAC(useRANSAC);
		// pre-detection
		params.setSigmaDog(1.50f);
		params.setThresholdDog(0.0083f);
		// detection
		params.setSupportRadius(3);
		params.setInlierRatio(0.37f);
		params.setMaxError(0.5034f);
		
		// grab all file path to the images in the folder
		ArrayList<File> paths = readFolder(iFolder, ".tif");
		// parse the db with smFish labels
		ArrayList<ImageData> imageData = readDb(databasePath);
		
		for (ImageData imageD : imageData) {
			String inputPath = iFolder.getAbsolutePath() + "/" + imageD.getFilename() + ".tif";
			String outputPath = oFolder.getAbsolutePath() + "/" + imageD.getFilename() + ".tif";
		
			// check that the corresponding files is not missing
			if (new File(inputPath).exists()) {
			
				// File outputPath = new File(imgPath.getAbsolutePath().substring(0, imgPath.getAbsolutePath().length() - 4));
			
				// File outputPath = new File(oFolder.getAbsolutePath() + "/" + imgPath.getName()); 
				// System.out.println(outputPath.getAbsolutePath());
			
				// IMP: 1 stage where we drop dome of the images
				// we don't take into account those that are you marked as smFISH signal
				System.out.println(inputPath);
				// preprocessImage(new File(inputPath), params, new File(outputPath));
			}
		}
		new ImageJ();
		// iterate over each image in the folder
	}
	
	public static ArrayList <ImageData> readDb(File databasePath) {

		ArrayList <ImageData> imageData = new ArrayList<>(); 
		// some constants
		final int nColumns = 24;
		// columns ids: 3, 6, 9, 12, 15 
		final int [] lambdaIndices = new int[] {2, 5, 8, 11, 14};
		final int [] stainIndices = new int[] {3, 6, 9, 12, 15};
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
					if (nextLine[stainIndices[j]].equals("FISH") && conditionalPick()) {
						// TODO: Check the naming for the files!
						// files.add(new String(nextLine[newFilenameIndex] + "-C" + j));

						int lambda = Integer.parseInt(nextLine[lambdaIndices[j]]);
						String filename = "C" + (j + 1) + "-"+ nextLine[newFilenameIndex];

						imageData.add(new ImageData(lambda, filename));
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

//		for (ImageData id : imageData)
//			System.out.println(id.getLambda() + " " + id.getFilename());
		
		return imageData;
	}
	
	// verify that the given image is smFish
	public static void checkThatTheImageIsSmFish(File databasePath) {
		// TODO:
		// iterate over xls 
		// iterate over the columns 
		// if column is smfish add the file to the process file set 
		// otherwise ignore

		// TODO: always check the input folder 
		// File path = new File("/Users/kkolyva/Desktop/smFISH-database/SEA-12-Table 1.csv");

		// files that we will consider in the end
		// ArrayList <String> files = new ArrayList<>();
		// TODO: add the length wave check here, too
		ArrayList <ImageData> imageData = new ArrayList<>(); 

		CSVReader reader = null;
		int nColumns = 24;
		String[] nextLine = new String [nColumns];
		// columns: 3, 6, 9, 12, 15 
		int [] lambdaIndices = new int[] {2, 5, 8, 11, 14};
		int [] stainIndices = new int[] {3, 6, 9, 12, 15};
		// index for the column with the new name
		int newFilenameIndex = 23;

		try {
			int toSkip = 1; 
			reader = new CSVReader(new FileReader(databasePath), ',', CSVReader.DEFAULT_QUOTE_CHARACTER, toSkip);
			// while there are rows in the file
			while ((nextLine = reader.readNext()) != null) {
				// iterate over the row; that is 25 elements long
				for (int j = 0; j < stainIndices.length; j++) {
					// 
					if (nextLine[stainIndices[j]].equals("FISH") && conditionalPick()) {
						// TODO: Check the naming for the files!
						// files.add(new String(nextLine[newFilenameIndex] + "-C" + j));

						int lambda = Integer.parseInt(nextLine[lambdaIndices[j]]);
						String filename = nextLine[newFilenameIndex] + "-C" + j;

						imageData.add(new ImageData(lambda, filename));
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		for (ImageData id : imageData)
			System.out.println(id.getLambda() + " " + id.getFilename());
	}

	// drop bad quality images
	public static boolean conditionalPick() {
		// true -> good quality
		return true;
	}

	public static void preprocessImage(File imgPath, GUIParams params, File outputPath){
		Img<FloatType> img = ImgLib2Util.openAs32Bit(imgPath.getAbsoluteFile());
		Img<FloatType> bg = new ArrayImgFactory<FloatType>().create(img, new FloatType());

		// 1. get the background
		int [] kernelDim = new int []{21, 21}; 
		MedianFilter.medianFilterSliced(img, bg, kernelDim);

		// 2. subtract the background
		HelperFunctions.subtractImg(img, bg);

		// 3. run the validation step here 
		boolean isValid = true;
		if (!isValid) return;

		// 4. normalize image 
		float min = 0;
		float max = 1;
		Normalize.normalize(img, new FloatType(min), new FloatType(max));
		
		// 4.* just resave the images at the moment
		IOFunctions.saveResult(img, outputPath.getAbsolutePath());

		// perform the processing steps from above
		// 5. run radial symmetry 
		// 6. filter the spot and save them 
		// BatchProcess.runProcess(img, params, outputPath);
	}

	public static void main(String [] args){
		File folder = new File("");
		// runPreprocess(folder);
	}

}
