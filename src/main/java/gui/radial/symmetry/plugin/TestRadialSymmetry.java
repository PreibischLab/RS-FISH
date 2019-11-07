package gui.radial.symmetry.plugin;

import java.io.File;
import java.io.IOException;

import net.imagej.Dataset;

/*
 * 
 * New main class for testing
 * */

public class TestRadialSymmetry {

	public static void main(String[] args) {
		final net.imagej.ImageJ ij = new net.imagej.ImageJ();
		ij.ui().showUI();

		String sysPath = "src/main/resources/rs-test/";
		String testType = "vizualisation"; // algorithm
		String isRandom = true ? "random-" : ""; 

		int numTests = 4;
		File [] paths = new File[numTests];
		Dataset [] datasets = new Dataset[numTests];

		int idx = 0; 
		for (int d = 2; d <= 3; d++) {
			for(int t = 0; t <= 1; t++){
				String spatial = (d == 2 ? "xy" : "xyz");
				String temporal = (t == 0 ? "" : "t");
				String fullPath = sysPath + testType + "/test-" + isRandom + spatial + temporal + ".tif";

				System.out.println(fullPath);
				paths[idx] = new File(fullPath);
				try {
					datasets[idx] = ij.scifio().datasetIO().open(paths[idx].getAbsolutePath());
					// show the image
					ij.ui().show(datasets[idx]);
				}
				catch(IOException e){
					System.out.println("LUL!");
				}
			}
		}

		try {
			Dataset dataset = ij.scifio().datasetIO().open("/Users/kkolyva/Downloads/test.tif");
			ij.ui().show(dataset);
		} catch (IOException e) {
			e.printStackTrace();
		}

		ij.command().run(Radial_Symmetry.class, true);
		System.out.println("Doge!");
	}
}
