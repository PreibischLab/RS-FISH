package cluster.radial.symmetry.process;

import java.io.File;
import java.nio.file.Paths;

import ij.ImageJ;
import radial.symmetry.utils.IOUtils;

public class RadialSymmetryBothSteps {
	
	public static void runFullProcess2StepsSEA12() {
		String prefix = "/Users/kkolyva/Desktop/2018-07-31-09-53-32-SEA-all-results-together/test";
		// path to the csv file with RS detected centers
		File pathCenters = Paths.get(prefix, "centers", "all-centers.csv").toFile();
		// path to the images with masks
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
		File pathDatabase = Paths.get(prefix, "smFISH-database", "N2-Table 1.csv").toFile();
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
		// RunStepsPreprocess.runFirstStepPreprocess(pathImages, pathDatabase, pathImagesMask, pathImagesMedian);
		RunBatchProcess.runProcess(pathImagesMedian, pathImagesMask, pathDatabase, pathZcorrected, pathResultCsvBeforeCorrection, pathParameters, pathResultCsv, doZcorrection);
		// RunStepsPreprocess.runSecondStepPreprocess(pathZcorrected, pathDatabase, pathImagesMask, pathCenters, pathImagesMedian2);
		// RunBatchProcess.runProcess(pathImagesMedian2, pathImagesMask, pathDatabase, pathZcorrected2, null, null, pathResultCsv2, doZcorrection);
	}

	public static void main(String[] args) {
		// new ImageJ();
		runFullProcess2StepsN2();
		System.out.println("DOGE!");
	}

}
