package process.radialsymmetry.cluster;

import java.io.File;

public class FullProcess {

	public static void runFullProcess() {
		// TODO: make this one more general
		String prefix = "/media/milkyklim/1TB";
		// path to the images with roi
		File pathImagesRoi = new File(prefix +"/2018-03-29-laura-radial-symmetry-numbers/SEA-12");
		// path to separate channel images
		File pathImages = new File(prefix +"/2018-03-29-laura-radial-symmetry-numbers/SEA-12-channels-correct");
		// path to the database with the images
		File pathDatabase = new File(prefix +"/2018-03-29-laura-radial-symmetry-numbers/smFISH-database/SEA-12-Table 1.csv");
		// path to the median filtered images that we save
		File pathImagesMedian = new File(prefix +"/2018-04-03-laura-images-processing");
		// path to save the .csv files with the results
		File pathResultCsv = new File("");
		
		// trigger preprocessing
		Preprocess.runPreprocess(pathImages, pathImagesMedian, pathDatabase);
		// trigger fixing; reslice and add roi's
		FixImages.runFixImages(pathImagesMedian, pathImagesRoi, pathImagesMedian);
		// use only images that are fine 
		// RadialSymmetry on pathImagesMedian
		// 
	}
	
	public static void main(String[] args) {
		runFullProcess();
	}

}
