package process.radialsymmetry.cluster;

import java.io.File;

public class FullProcess {
	
	
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
		
		// path to the csv file with RS detected centers
		File pathCenters = new File("");		
		int centerIndex = 2; // run the second iteration of the radial symmetry
		
		boolean allPathsAreCorrect = checkAllPaths(pathImages, pathImagesRoi, pathImagesMedian, pathDatabase, pathCenters);
		if (!allPathsAreCorrect)
			return;

		if (true){
			// trigger preprocessing
			Preprocess.runPreprocess(pathImages, pathImagesRoi, pathImagesMedian, pathDatabase, pathCenters, centerIndex);
			// trigger fixing; reslice and add roi's
			FixImages.runFixImages(pathImagesMedian, pathImagesRoi, pathImagesMedian);
		}
		if (false) {
			// use only images that are fine 
			BatchProcess.runProcess(pathImagesMedian, pathDatabase, pathResultCsv);
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
			BatchProcess.runProcess(pathImagesMedian, pathDatabase, pathResultCsv);
			// 
		}
	}


	public static void main(String[] args) {
		runFullProcess2();
		System.out.println("DOGE!");
	}

}
