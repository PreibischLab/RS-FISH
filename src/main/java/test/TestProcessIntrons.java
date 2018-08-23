package test;

import java.io.File;
import java.nio.file.Paths;

import scripts.radial.symmetry.process.ProcessIntrons;

public class TestProcessIntrons {

	public static void testProcessImage() {
		String root = "/Users/kkolyva/Desktop/2018-07-31-09-53-32-N2-all-results-together/test";
		String intronImageFilename = "C2-N2_395.tif";
		String exonFilename = "C1-N2_395.csv";
		String intronFilename = "C2-N2_395.csv";

		// IOUtils.checkPaths(...)

		File intronImagePath = Paths.get(root, "median", intronImageFilename).toFile();
		File exonPath = Paths.get(root, exonFilename).toFile();
		File intronPath = Paths.get(root, intronFilename).toFile();

		ProcessIntrons.processImage(intronImagePath, exonPath, intronPath);

		System.out.println("DOGE!");
	}
	
	public static void main(String[] args){
		testProcessImage();
	}
}
