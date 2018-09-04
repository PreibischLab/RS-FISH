package cluster.radial.symmetry.process;

import java.io.File;
import java.nio.file.Paths;

import radial.symmetry.utils.IOUtils;
import util.NotSoUsefulOutput;

public class RadialSymmetryBothSteps {
	
	
	public static void runFullProcess1Step1Image(String root, String channelFilename, String experimentType, int step, int waveLength) {
		// distinguish between N2 and SEA12: exprerimentType
		// step: 1st or 2nd run of the radial symmetry
		String dataExt = ".csv";
		String imgExt = ".tif";
		
		// path to the csv file with RS detected centers
		File pathCenters = Paths.get(root, "centers", "all-centers" + dataExt).toFile();
		// path to the images with mask
		File pathImageMask = Paths.get(root, "roi", channelFilename.substring(3) + imgExt).toFile();
		
		// path to separate channel images
		File pathImage = Paths.get(root, "channels", channelFilename + imgExt).toFile();
		// path to the database with the images
		File pathDatabase = Paths.get(root, "smFISH-database", "N2-Table 1" + dataExt).toFile();
		// path to the median filtered images that we save
		File pathImageMedian = Paths.get(root, "median", channelFilename + imgExt).toFile();
		File pathImageMedian2 =  Paths.get(root, "median-2", channelFilename + imgExt).toFile();
		// path to save the .csv files with the results without the correction
		File pathResultCsvBeforeCorrection = Paths.get(root, "csv-before", channelFilename + dataExt).toFile();
		// path to save the .csv files with the results
		File pathResultCsv = Paths.get(root, "csv", channelFilename + dataExt).toFile();
		// path to save the .csv files with the results
		File pathResultCsv2 = Paths.get(root, "csv-2", channelFilename + dataExt).toFile();
		// path z-corrected image
		File pathZcorrected = Paths.get(root, "zCorrected", channelFilename + imgExt).toFile();
		// path z-corrected image
		File pathZcorrected2 = Paths.get(root, "zCorrected-2", channelFilename + imgExt).toFile();
		// path to the files with the parameters
		File pathParameters =  Paths.get(root, "csv-parameters", channelFilename + dataExt).toFile();
		
		File [] allPaths = new File[] {pathImageMask, pathImage, pathDatabase, pathZcorrected};
		NotSoUsefulOutput.printFiles(allPaths);
		boolean allPathsAreCorrect = IOUtils.checkPaths(allPaths);
		if (!allPathsAreCorrect)
			return;
		
		
		boolean doZcorrection = true;
		if (step == 1) {
			RunStepsPreprocess.runFirstStepPreprocessImage(pathImage, pathImageMask, pathImageMedian); 
			RunBatchProcess.runProcessImage(pathImageMedian, pathImageMask, pathZcorrected, pathResultCsvBeforeCorrection, pathParameters, pathResultCsv, doZcorrection, waveLength);
		}
		if (step == 2) {
			RunStepsPreprocess.runSecondStepPreprocessImage(pathZcorrected, pathImageMask, pathCenters, pathImageMedian2);
			RunBatchProcess.runProcessImage(pathImageMedian2, pathImageMask, pathZcorrected2, new File(""), new File(""), pathResultCsv2, doZcorrection, waveLength);
		}
		if (step != 1 && step != 2) {
			System.out.println("Wrong step specified");
		}
		
	}
	
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
		RunStepsPreprocess.runFirstStepPreprocess(pathImages, pathDatabase, pathImagesMask, pathImagesMedian);
		RunBatchProcess.runProcess(pathImagesMedian, pathImagesMask, pathDatabase, pathZcorrected, pathResultCsvBeforeCorrection, pathParameters, pathResultCsv, doZcorrection);
		// RunStepsPreprocess.runSecondStepPreprocess(pathZcorrected, pathDatabase, pathImagesMask, pathCenters, pathImagesMedian2);
		// RunBatchProcess.runProcess(pathImagesMedian2, pathImagesMask, pathDatabase, pathZcorrected2, null, null, pathResultCsv2, doZcorrection);
	}

	public static void main(String[] args) {
		// new ImageJ();
		// runFullProcess2StepsN2();
		
		String root = "/Users/kkolyva/Desktop/2018-07-31-09-53-32-N2-all-results-together/test";
		String channelFilename = "C2-N2_395";
		String experimentType = "N2";
		int step = 2;
		int waveLength = 670;
		
		runFullProcess1Step1Image(root, channelFilename, experimentType, step, waveLength);
		
		System.out.println("DOGE!");
	}

}
