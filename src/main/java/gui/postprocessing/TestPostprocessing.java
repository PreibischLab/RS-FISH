package gui.postprocessing;

import java.io.IOException;

import net.imagej.Dataset;

public class TestPostprocessing {

	public static void main(String[] args) {
		final net.imagej.ImageJ ij = new net.imagej.ImageJ();
		ij.ui().showUI();
		// open one test image
		// try {
		// String path =
		// "src/main/resources/rs-test/vizualisation/test-random-xy.tif";
		// Dataset dataset = ij.scifio().datasetIO().open(path);
		// ij.ui().show(dataset);
		// } catch (IOException e) {
		// e.printStackTrace();
		// }

		try {
			Dataset dataset = ij.scifio().datasetIO().open("/Users/kkolyva/Desktop/mask.tif");
			ij.ui().show(dataset);
		} catch (IOException e) {
			e.printStackTrace();
		}

		ij.command().run(Postprocessing.class, true);
		System.out.println("Doge!");
	}

}