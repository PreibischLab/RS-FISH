package cluster.radial.symmetry.process;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Paths;
import java.util.ArrayList;

import radial.symmetry.utils.IOUtils;
import util.NotSoUsefulOutput;
import util.Utils;

public class SetupProcess {
	
	public static void setupProcess(String root, String datasetDbFile) {
		// [x] check that all necessary dirs are there 
		// [x] create all the dirs if they are missing 
		File smFishDbFilename = Paths.get(root, "smFISH-database", datasetDbFile).toFile();
		String [] folders = new String [] {"centers", "roi", "channels", "smFISH-database", "median", "median-2", "csv-before", "csv", "csv-2",
			"zCorrected", "zCorrected-2", "csvParameters"};
		int numFolders = folders.length + 1;

		File [] allPaths = new File[numFolders];
		allPaths[numFolders - 1] = smFishDbFilename;

		for (int j = 0; j < numFolders - 1; j++) {
			allPaths[j] = Paths.get(root, folders[j]).toFile();
			if(!allPaths[j].exists()) {
				allPaths[j].mkdirs();
				System.out.println("Created: " + allPaths[j].getAbsolutePath());
			}
		}
		
		NotSoUsefulOutput.printFiles(allPaths);
	}

	public static void createInputArguments(String root, String datasetDbFile, String fishType, boolean doFilter) throws FileNotFoundException, UnsupportedEncodingException {
		// [ ] create the file with the triplets that should be processed
		
		// fishType: 
		//		final String exon = "DPY-23_EX"; 
		//		final String intron = "DPY-23_INT";
		//		final String dapi = "DAPI";
		
		// what happens in function then ?
		File smFishDbFilename = Paths.get(root, "smFISH-database", datasetDbFile).toFile();
		File outputFilename = Paths.get(root, fishType + ".txt").toFile();
		ArrayList<ImageDataFull> imageDataFull = IOUtils.readDbFull(smFishDbFilename, doFilter);

		PrintWriter outputFile = new PrintWriter(outputFilename, "UTF-8");
		for (ImageDataFull idf : imageDataFull) {
			if (idf.getChannels().containsKey(fishType)) {
				outputFile.println(Utils.getNameWithoutExt(idf.getChannel(fishType), idf.getFilename()));
			}
		}
		outputFile.close();
	}
	
	public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException {
		String root = "/Users/kkolyva/Desktop/2018-07-31-09-53-32-N2-all-results-together/test";
		String smFishDbFilename = "N2-Table 1.csv";
		final String exon = "MDH-1"; 

		// setupProcess(root, smFishDbFilename);
		createInputArguments(root, smFishDbFilename, exon, true);
		System.out.println("DOGE!");
	}

}
