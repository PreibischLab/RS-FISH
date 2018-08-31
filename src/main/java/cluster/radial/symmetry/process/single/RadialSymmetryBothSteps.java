package cluster.radial.symmetry.process.single;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Paths;
import java.util.ArrayList;

import cluster.radial.symmetry.process.ImageDataFull;
import cluster.radial.symmetry.process.RunBatchProcess;
import cluster.radial.symmetry.process.RunStepsPreprocess;
import radial.symmetry.utils.IOUtils;
import util.NotSoUsefulOutput;

public class RadialSymmetryBothSteps {
	
//	public static void setupProcess(String root, String datasetDbFile) {
//		
//		// path to the csv file with RS detected centers
//		File pathCenters = Paths.get(prefix, "centers", "all-centers.csv").toFile();
//		// path to the images with mask
//		File pathImagesMask = Paths.get(prefix, "roi").toFile();
//		// path to separate channel images
//		File pathImages = Paths.get(prefix, "channels").toFile();
//		// path to the database with the images
//		File pathDatabase = Paths.get(prefix, "smFISH-database", "SEA-12-Table 1.csv").toFile();
//		// path to the median filtered images that we save
//		File pathImagesMedian = Paths.get(prefix, "median").toFile();
//		File pathImagesMedian2 =  Paths.get(prefix, "median-2").toFile();
//		// path to save the .csv files with the results without the correction
//		File pathResultCsvBeforeCorrection = Paths.get(prefix, "csv-before").toFile();
//		// path to save the .csv files with the results
//		File pathResultCsv = Paths.get(prefix, "csv").toFile();
//		// path to save the .csv files with the results
//		File pathResultCsv2 = Paths.get(prefix, "csv-2").toFile();
//		// path z-corrected image
//		File pathZcorrected = Paths.get(prefix, "zCorrected").toFile();
//		// path z-corrected image
//		File pathZcorrected2 = Paths.get(prefix, "zCorrected-2").toFile();
//		// path to the files with the parameters
//		File pathParameters =  Paths.get(prefix, "csv-parameters").toFile();
//		
//		
//		// [x] create all the dirs if they are missing 
//		// [x] check that all necessary dirs are there 
//		File smFishDbFilename = Paths.get(root, "smFISH-database", datasetDbFile).toFile();
//		String [] folders = new String [] {"roi", "median", "roi", "channels", "normalized","csv-dapi-intron"};
//		int numFolders = folders.length + 1;
//
//		File [] allPaths = new File[numFolders];
//		allPaths[numFolders - 1] = smFishDbFilename;
//
//		for (int j = 0; j < numFolders - 1; j++) {
//			allPaths[j] = Paths.get(root, folders[j]).toFile();
//			if(!allPaths[j].exists()) {
//				allPaths[j].mkdirs();
//				System.out.println("Created: " + allPaths[j].getAbsolutePath());
//			}
//		}
//		NotSoUsefulOutput.printFiles(allPaths);
//	}

//	public static void createInputArguments(String root, String datasetDbFile, boolean doFilter) throws FileNotFoundException, UnsupportedEncodingException {
//		// [ ] create the file with the triplets that should be processed
//		File smFishDbFilename = Paths.get(root, "smFISH-database", datasetDbFile).toFile();
//		File outputFilename = Paths.get(root, "intpu-triplet.txt").toFile();
//
//		ArrayList<ImageDataFull> imageDataFull = IOUtils.readDbFull(smFishDbFilename, doFilter);
//
//		final String exon = "DPY-23_EX"; 
//		final String intron = "DPY-23_INT";
//		final String dapi = "DAPI";
//
//		PrintWriter outputFile = new PrintWriter(outputFilename, "UTF-8");
//		for (ImageDataFull idf : imageDataFull) {
//			if (idf.getChannels().containsKey(exon) && idf.getChannels().containsKey(dapi) && idf.getChannels().containsKey(intron)) {
//				// write the file as exon \ dapi \ intron
//				String names = String.format("%s %s %s", 
//					getNameWithoutExt(idf.getChannel(exon), idf.getFilename()),
//					getNameWithoutExt(idf.getChannel(intron), idf.getFilename()),
//					getNameWithoutExt(idf.getChannel(dapi), idf.getFilename()));
//				outputFile.println(names);
//			}
//		}
//		outputFile.close();
//	}
	
	
	//FIXME: fix the way the parameters are set
	public static void runFullProcess2StepsN2() {
		String prefix = "/Users/kkolyva/Desktop/2018-07-31-09-53-32-N2-all-results-together/test";
		
		// path to the csv file with RS detected centers
		File pathCenters = Paths.get(prefix, "centers", "all-centers.csv").toFile();
		// path to the images with mask
		File pathImagesMask = Paths.get(prefix, "roi").toFile();
		// path to separate channel images
		File pathImages = Paths.get(prefix, "channels").toFile();
		// path to the database with the images
		File pathDatabase = Paths.get(prefix, "smFISH-database", "SEA-12-Table 1.csv").toFile();
		// path to the median filtered images that we save
		File pathImagesMedian = Paths.get(prefix, "median").toFile();
		File pathImagesMedian2 =  Paths.get(prefix, "median-2").toFile();
		// path to save the .csv files with the results without the correction
		File pathResultCsvBeforeCorrection = Paths.get(prefix, "csv-before").toFile();
		// path to save the .csv files with the results
		File pathResultCsv = Paths.get(prefix, "csv").toFile();
		// path to save the .csv files with the results
		File pathResultCsv2 = Paths.get(prefix, "csv-2").toFile();
		// path z-corrected image
		File pathZcorrected = Paths.get(prefix, "zCorrected").toFile();
		// path z-corrected image
		File pathZcorrected2 = Paths.get(prefix, "zCorrected-2").toFile();
		// path to the files with the parameters
		File pathParameters =  Paths.get(prefix, "csv-parameters").toFile();
		
		File [] allPaths = new File[] {pathImagesMask, pathImages, pathDatabase, pathResultCsv, pathZcorrected, pathResultCsvBeforeCorrection, pathParameters};
		boolean allPathsAreCorrect = IOUtils.checkPaths(allPaths);
		if (!allPathsAreCorrect)
			return;
		
		boolean doZcorrection = true;
		RunStepsPreprocess.runFirstStepPreprocess(pathImages, pathDatabase, pathImagesMask, pathImagesMedian);
		RunBatchProcess.runProcess(pathImagesMedian, pathImagesMask, pathDatabase, pathZcorrected, pathResultCsvBeforeCorrection, pathParameters, pathResultCsv, doZcorrection);
		RunStepsPreprocess.runSecondStepPreprocess(pathZcorrected, pathDatabase, pathImagesMask, pathCenters, pathImagesMedian2);
		RunBatchProcess.runProcess(pathImagesMedian2, pathImagesMask, pathDatabase, pathZcorrected2, null, null, pathResultCsv2, doZcorrection);
	}

	public static void main(String[] args) {
		// [ ] pass an argument to choose which step do you need  

	}

}
