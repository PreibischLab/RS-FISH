package process.radialsymmetry.cluster;

import java.io.File;

import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.real.FloatType;

import ij.ImageJ;
import util.ImgLib2Util;

public class FullProcess {


	public static boolean checkAllPaths(File [] paths) {
		boolean allGood = true; 
		for (File path: paths) {
			if (!path.exists()) {
				System.out.println("Doesn't exist: " + path.getAbsolutePath());
				allGood = false;
			}
		}
		return allGood;
	}

	// FIXME: OLD: CAN BE removed
	// check that all paths exist
	public static boolean checkAllPaths(File pathImages, File pathImagesRoi, File pathImagesMedian, File pathDatabase, File pathCenters) {
		boolean allGood = true; 

		if (!pathImages.exists()) {
			allGood = false;
			System.out.println("Doesn't exist: " + pathImages.getAbsolutePath());
		}
		if (!pathImagesRoi.exists()) {
			allGood = false;
			System.out.println("Doesn't exist: " + pathImagesRoi.getAbsolutePath());
		}
		if (!pathImagesMedian.exists()) {
			allGood = false;
			System.out.println("Doesn't exist: " + pathImagesMedian.getAbsolutePath());
		}
		if (!pathDatabase.exists()) {
			allGood = false;
			System.out.println("Doesn't exist: " + pathDatabase.getAbsolutePath());
		}
		if (!pathCenters.exists()) {
			allGood = false;
			System.out.println("Doesn't exist: " + pathCenters.getAbsolutePath());
		}


		return allGood; 
	}


	public static void runFullProcess2Steps(){
		// TODO: make this one more general
		// String prefix = "/Volumes/1TB/test/2018-04-17-test-run";
		// String prefix = "/Users/kkolyva/Desktop/2018-04-18-08-29-25-test/test/2018-04-18-14-46-52-median-median-first-test"; // 2018-04-18-14-46-52-median-median-first-test"; //2018-04-18-14-46-52-median-median-first-test";

		String prefix = "/Users/kkolyva/Desktop/2018-04-18-08-29-25-test/test/2018-05-02-13-33-11-median-median-first-test";
		
		// path to the csv file with RS detected centers
		File pathCenters = new File(prefix + "/centers/all-centers.csv");
		int centerIndex = 1; // run the second iteration of the radial symmetry
		String suffix = centerIndex == 1 ? "" : "-2";

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

		// path to save the .csv files with the results
		File pathResultCsv = new File(prefix +"/csv" + suffix);
		// path to save the .csv files with the results
		File pathResultCsv2 = new File(prefix +"/csv-2");
		// path z-corrected image
		File pathZcorrected = new File(prefix +"/zCorrected");
		// path z-corrected image
		File pathZcorrected2 = new File(prefix +"/zCorrected-2");

		File [] allPaths = new File[] {pathCenters, pathImagesRoi, pathImages, pathDatabase, pathImagesMedianMedian, pathResultCsv, pathZcorrected};
		boolean allPathsAreCorrect = checkAllPaths(allPaths);
		if (!allPathsAreCorrect)
			return;

		boolean doZcorrection = true;

		// - median per slice 
		// to get rid of the bright planes (run over the whole plane, NOT roi)
		// - median-median 
		// to estimate the bg (run over the roi only)
		// - normalize the image [0,1] where x_min=0 -> 0, brightest pixel -> 1;
		// (maybe it is a good idea to use median of r=1 and take the brightest pixel from there for stability)
		// Preprocess.runFirstStepPreprocess(pathImages, pathDatabase, pathImagesRoi, pathImagesMedian);
		// fix the rois
		// FixImages.runFixImages(pathImagesMedian, pathImagesRoi, pathImagesMedian);
		// - run radial symmetry
		// - (subtract the x_min value before performing the z-correction but it is 0 in our case anyways)
		// can scip this step because x_min = 0
		// - z-correct the points 
		// - z-correct the image from the previous step (one normalized between 0 and 1)
		// BatchProcess.runProcess(pathImagesMedian, pathDatabase, pathZcorrected, pathResultCsv, doZcorrection);
		// - reuse the the z-corrected image from the previous step 
		// - normalize the image [0,1] where x_min=0 -> 0; center of the peak -> 1;

		Preprocess.runSecondStepPreprocess(pathZcorrected, pathDatabase, pathImagesRoi, pathCenters, pathImagesMedian2);
		FixImages.runFixImages(pathImagesMedian2, pathImagesRoi, pathImagesMedian2);
		// - radial symmetry you are looking for!
		// doZcorrection = true;
		BatchProcess.runProcess(pathImagesMedian2, pathDatabase, pathZcorrected2, pathResultCsv2, doZcorrection);
		// first iteration full preprossing 

	}

	public static void runFullProcessTest() {
		// TODO: make this one more general
		// String prefix = "/Volumes/1TB/test/2018-04-17-test-run";
		String prefix = "/Users/kkolyva/Desktop/2018-04-18-08-29-25-test/test/2018-04-18-14-46-52-median-median-first-test";

		// path to the csv file with RS detected centers
		File pathCenters = new File(prefix + "/centers/all-centers.csv");
		int centerIndex = 2; // run the second iteration of the radial symmetry
		String suffix = centerIndex == 1 ? "" : "-2";

		// path to the images with roi
		File pathImagesRoi = new File(prefix +"/roi");
		// path to separate channel images
		File pathImages = new File(prefix +"/channels");
		// path to the database with the images
		File pathDatabase = new File(prefix +"/smFISH-database/N2-Table 1.csv");
		// path to separate channel images
		File pathImagesMedianMedian = new File(prefix +"/median-median");
		// path to the median filtered images that we save
		File pathImagesMedian = new File(prefix +"/median" + suffix);
		// path to save the .csv files with the results
		File pathResultCsv = new File(prefix +"/csv" + suffix);
		// path z-corrected image
		File pathZcorrected = new File(prefix +"/zCorrected");

		boolean allPathsAreCorrect = checkAllPaths(pathDatabase, pathDatabase, pathImagesMedian, pathDatabase, pathDatabase);
		if (!allPathsAreCorrect)
			return;

		// Img<FloatType> img =  ImgLib2Util.openAs32Bit(new File("/Volumes/1TB/test/2/img/C1-N2_96.tif"));
		// ImageJFunctions.show(img);

		if (false) {
			ExtraPreprocess.runExtraPreprocess(pathImages, pathDatabase, pathImagesMedianMedian);
		}

		if (true){
			// trigger preprocessing
			if (centerIndex == 1) {
				Preprocess.runPreprocess(pathImagesMedianMedian, pathImagesRoi, pathImagesMedian, pathDatabase, pathCenters, centerIndex);
				// trigger fixing; reslice and add roi's
				FixImages.runFixImages(pathImagesMedian, pathImagesRoi, pathImagesMedian);
			}
			if (centerIndex == 2) {
				Preprocess.runPreprocess(pathImagesMedianMedian, pathImagesRoi, pathImagesMedian, pathDatabase, pathCenters, centerIndex);
				// trigger fixing; reslice and add roi's
				FixImages.runFixImages(pathImagesMedian, pathImagesRoi, pathImagesMedian);
			}

		}
		if (true) {
			// use only images that are fine 
			if (centerIndex == 1)
				BatchProcess.runProcess(pathImagesMedian, pathDatabase, pathZcorrected, pathResultCsv, true);
			if (centerIndex == 2)
				BatchProcess.runProcess(pathZcorrected, pathDatabase, null, pathResultCsv, true);
		}

	}


	public static void runFullProcess() {
		// TODO: make this one more general
		String prefix = "/media/milkyklim/1TB/new/";
		// path to the images with roi
		File pathImagesRoi = new File(prefix +"/2018-03-29-laura-radial-symmetry-numbers/SEA-12");
		// path to separate channel images
		File pathImages = new File(prefix +"/2018-03-29-laura-radial-symmetry-numbers/SEA-12-channels-correct");
		// path to the database with the images
		File pathDatabase = new File(prefix +"/2018-03-29-laura-radial-symmetry-numbers/smFISH-database/SEA-12-Table 1.csv");
		// path to the median filtered images that we save
		File pathImagesMedian = new File(prefix +"/2018-04-03-laura-images-processing/median-correct");
		// path to save the .csv files with the results
		File pathResultCsv = new File(prefix +"/2018-04-03-laura-images-processing/results");

		// path z-corrected image
		File pathZcorrected = new File(prefix +"/zCorrected");

		// path to the csv file with RS detected centers
		File pathCenters = new File("");		
		int centerIndex = 2; // run the second iteration of the radial symmetry



		boolean allPathsAreCorrect = checkAllPaths(pathImages, pathImagesRoi, pathImagesMedian, pathDatabase, pathCenters);
		if (!allPathsAreCorrect)
			return;

		if (false){
			// trigger preprocessing
			// Preprocess.runPreprocess(pathImages, pathImagesRoi, pathImagesMedian, pathDatabase, pathCenters, centerIndex);
			// trigger fixing; reslice and add roi's
			// FixImages.runFixImages(pathImagesMedian, pathImagesRoi, pathImagesMedian);
		}
		if (true) {
			// use only images that are fine 
			BatchProcess.runProcess(pathImagesMedian, pathDatabase, pathZcorrected, pathResultCsv, true);
			// 
		}
	}

	public static void runFullProcess2() {
		// TODO: make this one more general
		String prefix = "/home/milkyklim/Desktop";
		// name of the line
		String line = "N2";
		// path to the images with roi
		File pathImagesRoi = new File(prefix +"/2018-04-03-laura-radial-symmetry-numbers/N2");
		// path to separate channel images
		File pathImages = new File(prefix +"/2018-04-03-laura-radial-symmetry-numbers/N2-channels-correct");
		// path to the database with the images
		File pathDatabase = new File(prefix +"/2018-04-03-laura-radial-symmetry-numbers/smFISH-database/N2-Table 1.csv");
		// path to the median filtered images that we save
		File pathImagesMedian = new File(prefix +"/2018-04-03-laura-radial-symmetry-numbers/median-correct-2");
		// path to save the .csv files with the results
		File pathResultCsv = new File(prefix +"/2018-04-03-laura-radial-symmetry-numbers/results-2");

		// path z-corrected image
		File pathZcorrected = new File(prefix +"/zCorrected");

		// path to the csv file with RS detected centers
		File pathCenters = new File(prefix +"/2018-04-03-laura-radial-symmetry-numbers/N2-results/centers/all-centers.csv");		
		int centerIndex = 2; // run the second iteration of the radial symmetry

		boolean allPathsAreCorrect = checkAllPaths(pathImages, pathImagesRoi, pathImagesMedian, pathDatabase, pathCenters);
		if (!allPathsAreCorrect)
			return;

		if (false){
			// trigger preprocessing
			Preprocess.runPreprocess(pathImages, pathImagesRoi, pathImagesMedian, pathDatabase, pathCenters, centerIndex);
			// trigger fixing; reslice and add roi's
			FixImages.runFixImages(pathImagesMedian, pathImagesRoi, pathImagesMedian);
		}
		if (true) {
			// use only images that are fine 
			BatchProcess.runProcess(pathImagesMedian, pathDatabase, pathZcorrected, pathResultCsv, true);
			// 
		}
	}


	public static void main(String[] args) {
		new ImageJ();
		// runFullProcessTest();
		runFullProcess2Steps();
		System.out.println("DOGE!");
	}

}
