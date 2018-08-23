package cluster.radial.symmetry.process.updated;

import java.io.File;
import java.nio.file.Paths;

import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.real.FloatType;

import ij.ImageJ;
import radial.symmetry.utils.IOUtils;
import util.ImgLib2Util;

public class RadialSymmetryBothSteps {
	
	
	// TODO: KEEP this one as the only one working
	// TODO: CREATE the copy of this one for for N2
	public static void runFullProcess2StepsSEA12() {
		String prefix = "/Users/kkolyva/Desktop/2018-07-31-09-53-32-SEA-all-results-together/test";
		
		// path to the csv file with RS detected centers
		File pathCenters = Paths.get(prefix, "centers", "all-centers.csv").toFile();
		// path to the images with roi
		File pathImagesRoi = Paths.get(prefix, "roi").toFile();
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
		
		File [] allPaths = new File[] {pathImagesRoi, pathImages, pathDatabase, pathResultCsv, pathZcorrected, pathResultCsvBeforeCorrection, pathParameters};
		boolean allPathsAreCorrect = IOUtils.checkPaths(allPaths);
		if (!allPathsAreCorrect)
			return;
		
		boolean doZcorrection = true;
		Preprocess.runFirstStepPreprocess(pathImages, pathDatabase, pathImagesRoi, pathImagesMedian);
		BatchProcess.runProcess(pathImagesMedian, pathImagesRoi, pathDatabase, pathZcorrected, pathResultCsvBeforeCorrection, pathParameters, pathResultCsv, doZcorrection);
		Preprocess.runSecondStepPreprocess(pathZcorrected, pathDatabase, pathImagesRoi, pathCenters, pathImagesMedian2);
		BatchProcess.runProcess(pathImagesMedian2, pathImagesRoi, pathDatabase, pathZcorrected2, null, null, pathResultCsv2, doZcorrection);
	}
	
	
	public static void runFullProcess2StepsSEA12Old(){
		
		String prefix = "/Volumes/1TB/2018-05-15-12-30-27-SEA12-full-stack";
		
		// path to the csv file with RS detected centers
		File pathCenters = new File(prefix + "/centers/all-centers.csv");
		int centerIndex = 1; // run the second iteration of the radial symmetry
		// String suffix = centerIndex == 1 ? "" : "-2";

		// path to the images with roi
		File pathImagesRoi = new File(prefix +"/roi");
		// path to separate channel images
		File pathImages = new File(prefix +"/channels");
		// path to the database with the images
		File pathDatabase = new File(prefix +"/smFISH-database/SEA-12-Table 1.csv");
		// path to separate channel images
		File pathImagesMedianMedian = new File(prefix +"/median-median");
		// path to the median filtered images that we save
		File pathImagesMedian = new File(prefix +"/median");

		File pathImagesMedian2 = new File(prefix +"/median-2");

		// path to save the .csv files with the results without the correction
		File pathResultCsvBeforeCorrection = new File(prefix + "/csv-before");
		
		// path to save the .csv files with the results
		File pathResultCsv = new File(prefix +"/csv");
		// path to save the .csv files with the results
		File pathResultCsv2 = new File(prefix +"/csv-2");
		// path z-corrected image
		File pathZcorrected = new File(prefix +"/zCorrected");
		// path z-corrected image
		File pathZcorrected2 = new File(prefix +"/zCorrected-2");
		// path to the files with the parameters
		File pathParameters = new File(prefix + "/csv-parameters");
		
		File [] allPaths = new File[] {pathImagesRoi, pathImages, pathDatabase, pathImagesMedianMedian, pathResultCsv, pathZcorrected, pathResultCsvBeforeCorrection, pathParameters};
		boolean allPathsAreCorrect = IOUtils.checkPaths(allPaths);
		if (!allPathsAreCorrect)
			return;

		boolean doZcorrection = true;
		// - median per slice 
		// to get rid of the bright planes (run over the whole plane, NOT roi)
		// - median-median 
		// to estimate the bg (run over the roi only)
		// - normalize the image [0,1] where x_min=0 -> 0, brightest pixel -> 1;
		// (maybe it is a good idea to use median of r=1 and take the brightest pixel from there for stability)
		Preprocess.runFirstStepPreprocess(pathImages, pathDatabase, pathImagesRoi, pathImagesMedian);
		// fix the rois
		// FixImages.runFixImages(pathImagesMedian, pathImagesRoi, pathImagesMedian);
		// - run radial symmetry
		// - (subtract the x_min value before performing the z-correction but it is 0 in our case anyways)
		// can skip this step because x_min = 0
		// - z-correct the points 
		// - z-correct the image from the previous step (one normalized between 0 and 1)
		// BatchProcess.runProcess(pathImagesMedian, pathDatabase, pathZcorrected, pathResultCsvBeforeCorrection, pathParameters, pathResultCsv, doZcorrection);
		// - reuse the the z-corrected image from the previous step 
		// - normalize the image [0,1] where x_min=0 -> 0; center of the peak -> 1;

//		Preprocess.runSecondStepPreprocess(pathZcorrected, pathDatabase, pathImagesRoi, pathCenters, pathImagesMedian2);
//		FixImages.runFixImages(pathImagesMedian2, pathImagesRoi, pathImagesMedian2);
//		// - radial symmetry you are looking for!
//		// doZcorrection = true;
//		 
		// BatchProcess.runProcess(pathImagesMedian2, pathImagesRoi, pathDatabase, pathZcorrected2, null, null, pathResultCsv2, doZcorrection);
		// first iteration full preprossing 

	}

	public static void runFullProcess2Steps(){
		// TODO: make this one more general
		// String prefix = "/Volumes/1TB/test/2018-04-17-test-run";
		// String prefix = "/Users/kkolyva/Desktop/2018-04-18-08-29-25-test/test/2018-04-18-14-46-52-median-median-first-test"; // 2018-04-18-14-46-52-median-median-first-test"; //2018-04-18-14-46-52-median-median-first-test";

		String prefix = "/Volumes/500GB/2018-05-13-19-32-00-N2-full-stack";
		
		// path to the csv file with RS detected centers
		File pathCenters = new File(prefix + "/centers/all-centers.csv");
		int centerIndex = 1; // run the second iteration of the radial symmetry
		// String suffix = centerIndex == 1 ? "" : "-2";

		// path to the images with roi
		File pathImagesRoi = new File(prefix +"/roi");
		// path to separate channel images
		File pathImages = new File(prefix +"/channels");
		// path to the database with the images
		File pathDatabase = new File(prefix +"/smFISH-database/N2-Table 1.csv");
		// path to separate channel images
		File pathImagesMedianMedian = new File(prefix +"/median-median");
		// path to the median filtered images that we save
		File pathImagesMedian = new File(prefix +"/median");

		File pathImagesMedian2 = new File(prefix +"/median-2");

		// path to save the .csv files with the results without the correction
		File pathResultCsvBeforeCorrection = new File(prefix + "/csv-before");
		
		// path to save the .csv files with the results
		File pathResultCsv = new File(prefix +"/csv");
		// path to save the .csv files with the results
		File pathResultCsv2 = new File(prefix +"/csv-2");
		// path z-corrected image
		File pathZcorrected = new File(prefix +"/zCorrected");
		// path z-corrected image
		File pathZcorrected2 = new File(prefix +"/zCorrected-2");
		// path to the files with the parameters
		File pathParameters = new File(prefix + "/csv-parameters");
		
		File [] allPaths = new File[] {pathCenters, pathImagesRoi, pathImages, pathDatabase, pathImagesMedianMedian, pathResultCsv, pathZcorrected, pathResultCsvBeforeCorrection, pathParameters};
		boolean allPathsAreCorrect = IOUtils.checkPaths(allPaths);
		if (!allPathsAreCorrect)
			return;

		boolean doZcorrection = true;
		// - median per slice 
		// to get rid of the bright planes (run over the whole plane, NOT roi)
		// - median-median 
		// to estimate the bg (run over the roi only)
		// - normalize the image [0,1] where x_min=0 -> 0, brightest pixel -> 1;
		// (maybe it is a good idea to use median of r=1 and take the brightest pixel from there for stability)
		Preprocess.runFirstStepPreprocess(pathImages, pathDatabase, pathImagesRoi, pathImagesMedian);
		// fix the rois
		// TODO: THIS MIGHT BE OBSOLETE: CHECK AND REMOVE!
		// FixImages.runFixImages(pathImagesMedian, pathImagesRoi, pathImagesMedian);
		// - run radial symmetry
		// - (subtract the x_min value before performing the z-correction but it is 0 in our case anyways)
		// can skip this step because x_min = 0
		// - z-correct the points 
		// - z-correct the image from the previous step (one normalized between 0 and 1)
		BatchProcess.runProcess(pathImagesMedian, pathImagesRoi, pathDatabase, pathZcorrected, pathResultCsvBeforeCorrection, pathParameters, pathResultCsv, doZcorrection);
		// - reuse the the z-corrected image from the previous step 
		// - normalize the image [0,1] where x_min=0 -> 0; center of the peak -> 1;

		Preprocess.runSecondStepPreprocess(pathZcorrected, pathDatabase, pathImagesRoi, pathCenters, pathImagesMedian2);
//		FixImages.runFixImages(pathImagesMedian2, pathImagesRoi, pathImagesMedian2);
//		// - radial symmetry you are looking for!
//		// doZcorrection = true;
//		 
		BatchProcess.runProcess(pathImagesMedian2, pathImagesRoi, pathDatabase, pathZcorrected2, null, null, pathResultCsv2, doZcorrection);
		// first iteration full preprossing 

	}

	public static void main(String[] args) {
		new ImageJ();
		// runFullProcessTest();
		runFullProcess2StepsSEA12();
		System.out.println("DOGE!");
	}

}
