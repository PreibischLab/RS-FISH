package process.radialsymmetry.cluster;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

import util.opencsv.CSVReader;

public class Process {

	// 
	public static void checkThatTheImageIsSmFish(File path) {
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
	
	public static void main(String[] args) {
		// Preprocess.runPreprocess(new File(""), new File("")); 
		// now we have images saved but they have wrong channel\z-stack dims
		// and don't have roi's on them 
		// FixImages.runFixImage(new File(""), new File(""));
		// now all images have proper dimensionality and roi's 
		
		// TODO: add actual batch processing here 
		
	}
	
}
