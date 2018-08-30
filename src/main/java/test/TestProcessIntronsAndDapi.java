package test;

import java.io.File;
import java.nio.file.Paths;

import radial.symmetry.utils.IOUtils;
import scripts.radial.symmetry.process.PreprocessIntronAndDapi;
import scripts.radial.symmetry.process.ProcessIntronsAndDapi;

public class TestProcessIntronsAndDapi {
	
	public static void testProcessOneTripplet() {
		// C1-N2_342 C2-N2_342 C5-N2_342
		String root = "/Volumes/1TB/2018-06-14-12-36-00-N2-full-stack";
		String intronFilename = "C2-N2_342";
		String exonFilename = "C1-N2_342";
		String dapiFilename = "C5-N2_342";
		
		ProcessIntronsAndDapi.processOneTriplet(root, exonFilename, intronFilename, dapiFilename);
	}

	public static void testProcessExonIntronDapiResave() {
		String root = "/Users/kkolyva/Desktop/2018-07-31-09-53-32-N2-all-results-together/test";
		int nFiles = 4;

		String[] exonFilename = new String []{"C1-N2_395.csv", "C1-N2_447.csv","C1-N2_459.csv","C1-N2_542.csv"};
		String[] dapiFilename = new String []{"C5-N2_395.csv", "C5-N2_447.csv","C5-N2_459.csv","C5-N2_542.csv"};
		String[] intronFilename = new String []{"C2-N2_395.csv", "C3-N2_447.csv","C3-N2_459.csv","C3-N2_542.csv"};



		for (int j = 0; j < nFiles; j++) {			
			File exonPath = Paths.get(root, "csv-2", exonFilename[j]).toFile();
			File intronPath = Paths.get(root, "csv-dapi-intron", intronFilename[j]).toFile();
			File dapiPath = Paths.get(root, "csv-dapi-intron", dapiFilename[j]).toFile();

			File outputPath = Paths.get(root, "csv-dapi-intron", exonFilename[j].substring(3)).toFile();

			IOUtils.mergeExonIntronDapiAndWriteToCsv(exonPath, intronPath, dapiPath, outputPath, '\t');

			// PreprocessIntronAndDapi.normalizeAndSave(dapiImagePath, maskDapiPath, normalizedDapiPath);
			// ProcessIntronsAndDapi.processImage(normalizedDapiPath, exonPath, dapiPath);
		}

		System.out.println("DOGE!");
	}

	public static void testProcessDapiImageSet(){
		// new ImageJ();
		String root = "/Users/kkolyva/Desktop/2018-07-31-09-53-32-N2-all-results-together/test";

		int nFiles = 4;

		String[] dapiImageFilename = new String []{"C5-N2_395.tif", "C5-N2_447.tif","C5-N2_459.tif","C5-N2_542.tif"};
		String[] exonFilename = new String []{"C1-N2_395.csv", "C1-N2_447.csv","C1-N2_459.csv","C1-N2_542.csv"};
		String[] dapiFilename = new String []{"C5-N2_395.csv", "C5-N2_447.csv","C5-N2_459.csv","C5-N2_542.csv"};

		for (int j = 0; j < nFiles; j++) {
			File dapiImagePath = Paths.get(root, "channels", dapiImageFilename[j]).toFile();
			File exonPath = Paths.get(root, "csv-2", exonFilename[j]).toFile();
			File dapiPath = Paths.get(root, "csv-dapi-intron", dapiFilename[j]).toFile();

			File maskDapiPath = Paths.get(root, "roi", dapiImageFilename[j]).toFile();
			File normalizedDapiPath = Paths.get(root, "normalized", dapiImageFilename[j]).toFile();

			PreprocessIntronAndDapi.normalizeAndSave(dapiImagePath, maskDapiPath, normalizedDapiPath);
			ProcessIntronsAndDapi.processImage(normalizedDapiPath, exonPath, dapiPath);
		}

		System.out.println("DOGE!");
	}

	public static void testProcessIntronImageSet(){
		String root = "/Users/kkolyva/Desktop/2018-07-31-09-53-32-N2-all-results-together/test";

		int nFiles = 4;

		String[] intronImageFilename = new String []{"C2-N2_395.tif", "C3-N2_447.tif","C3-N2_459.tif","C3-N2_542.tif"};
		String[] exonFilename = new String []{"C1-N2_395.csv", "C1-N2_447.csv","C1-N2_459.csv","C1-N2_542.csv"};
		String[] intronFilename = new String []{"C2-N2_395.csv", "C3-N2_447.csv","C3-N2_459.csv","C3-N2_542.csv"};

		for (int j = 0; j < nFiles; j++) {
			File intronImagePath = Paths.get(root, "median", intronImageFilename[j]).toFile();
			File exonPath = Paths.get(root, "csv-2", exonFilename[j]).toFile();
			File intronPath = Paths.get(root, "csv-dapi-intron", intronFilename[j]).toFile();

			ProcessIntronsAndDapi.processImage(intronImagePath, exonPath, intronPath);
		}

		System.out.println("DOGE!");
	}

	public static void testProcessImage() {

		String root = "/Users/kkolyva/Desktop/2018-07-31-09-53-32-N2-all-results-together/test";
		String intronImageFilename = "C2-N2_395.tif";
		String exonFilename = "C1-N2_395.csv";
		String intronFilename = "C2-N2_395.csv";

		// IOUtils.checkPaths(...)

		File intronImagePath = Paths.get(root, "median", intronImageFilename).toFile();
		File exonPath = Paths.get(root, "csv-2", exonFilename).toFile();
		File intronPath = Paths.get(root, "csv-dapi-intron", intronFilename).toFile();

		ProcessIntronsAndDapi.processImage(intronImagePath, exonPath, intronPath);

		System.out.println("DOGE!");
	}

	public static void main(String[] args){
//		testProcessIntronImageSet();
//		testProcessDapiImageSet();
//		testProcessExonIntronDapiResave();
		
		testProcessOneTripplet();
	}
}
