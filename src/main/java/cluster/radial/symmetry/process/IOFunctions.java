package cluster.radial.symmetry.process;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.real.FloatType;

import ij.IJ;
import ij.ImagePlus;
import ij.io.FileSaver;
import util.opencsv.CSVReader;

public class IOFunctions {
	
	public static ArrayList<ImageData> readCenters(File filePath) {
		ArrayList <ImageData> imageData = new ArrayList<>(); 
		final int nColumns = 2;

		CSVReader reader = null;
		String[] nextLine = new String [nColumns];

		try {
			int toSkip = 1; 
			reader = new CSVReader(new FileReader(filePath), ',', CSVReader.DEFAULT_QUOTE_CHARACTER, toSkip);
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
			reader = new CSVReader(new FileReader(databasePath), ',', CSVReader.DEFAULT_QUOTE_CHARACTER, toSkip);
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
	
	public static void saveResult(Img <FloatType> img, String path) {
		// saving part
		IJ.saveAsTiff(ImageJFunctions.wrap(img, "").duplicate(), path);
	}
	
	public static void saveResultXyz(Img <FloatType> img, String path) {
		// saving part
		ImagePlus imp = ImageJFunctions.wrap(img, "");
		imp.setDimensions(1, imp.getNSlices(), 1);
		FileSaver fs = new FileSaver(imp);
		fs.saveAsTiff(path);
	} 
	
	public static void saveResult(ImagePlus imp, String path) {
		// saving part
		FileSaver fs = new FileSaver(imp);
		fs.saveAsTiff(path);
	}
	
	public static ArrayList<File> readFolder(File folder, String ext) {
		ArrayList<File> images = new ArrayList<>();
		System.out.println("Grab images from " + folder.getAbsolutePath());
		for (File file : folder.listFiles())
			// if the file is not hidden and ends with .tif we take it 
			if (file.isFile() && !file.getName().startsWith(".") && file.getName().endsWith(ext))
				images.add(file);
		return images;
	}
	
}
