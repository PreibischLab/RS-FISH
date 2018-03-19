package process.radialsymmetry.cluster;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.real.FloatType;

import ij.ImagePlus;
import util.ImgLib2Util;
import util.opencsv.CSVReader;

public class FixImages {
	
	
	
	// class to fix the number of slices in the stacks 
	// and add the roi that are gone because of channel
	// splitting 
	// folder - folder with stacks to reshape + add roi
	public static ArrayList<File> readImages(File iFolder) {
		ArrayList<File> paths = IOFunctions.readFolder(iFolder, ".tif");
		for (File imgPath : paths) {
			// File outputPath = new File(imgPath.getAbsolutePath().substring(0, imgPath.getAbsolutePath().length() - 4));
			reshapeImage(imgPath);
		}
		return paths;
	}

	// here process only one image
	public static void reshapeImage(File imgPath) {
		System.out.println(imgPath.getName());
		// open the image
		Img<FloatType> img = ImgLib2Util.openAs32Bit(imgPath.getAbsoluteFile());
		// save the image at the same location but with the roi on it
		ImagePlus imp = ImageJFunctions.wrap(img, "");
		imp.setDimensions(1, imp.getNSlices(), 1);

		// TODO: uncomment when the code is working to have proper saving 
		// IOFunctions.saveResult(imp, "");
	}

	// 
	public static void checkThatTheImageIsSmFish() {
		// TODO:
		// iterate over xls 
		// iterate over the columns 
		// if column is smfish add the file to the process file set 
		// otherwise ignore

		// TODO: always check the input folder 
		File path = new File("/Users/kkolyva/Desktop/smFISH-database/SEA-12-Table 1.csv");
		
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
			reader = new CSVReader(new FileReader(path), ',', CSVReader.DEFAULT_QUOTE_CHARACTER, toSkip);
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

	public static void main(String [] args) {
		checkThatTheImageIsSmFish();
	}
}
