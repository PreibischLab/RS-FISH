package test;

import java.io.File;
import java.nio.file.Paths;

import scripts.radial.symmetry.process.ProcessIntronsAndDapi;

public class TestProcessIntronsAndDapi {
	
	
	public static void testProcessImageSet(){
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
		testProcessImageSet();
	}
}
