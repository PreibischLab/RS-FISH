package process.radialsymmetry.cluster;

import java.io.File;

public class FullProcess {

	public static void runFullProcess() {
		// TODO: make this one more general
		String prefix = "/media/milkyklim/1TB";
		// pre
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

		if (false){
			// trigger preprocessing
			Preprocess.runPreprocess(pathImages, pathImagesRoi, pathImagesMedian, pathDatabase);
			// trigger fixing; reslice and add roi's
			FixImages.runFixImages(pathImagesMedian, pathImagesRoi, pathImagesMedian);
		}
		if (true) {
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
		File pathImagesMedian = new File(prefix +"/2018-04-03-laura-radial-symmetry-numbers/median");
		// path to save the .csv files with the results
		File pathResultCsv = new File(prefix +"/2018-04-03-laura-radial-symmetry-numbers/results");


		if (false){
			// trigger preprocessing
			Preprocess.runPreprocess(pathImages, pathImagesRoi, pathImagesMedian, pathDatabase);
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
