package gui.csv.overlay.plugin;

import java.io.File;
import java.io.IOException;

import net.imagej.Dataset;

public class TestCsvOverlayPlugin {

	public static void main(String[] args) {
		File path = new File("/Users/kkolyva/Downloads/test.tif");

		// create the ImageJ application context with all available services
		final net.imagej.ImageJ ij = new net.imagej.ImageJ();
		ij.ui().showUI();

		// load the dataset
		Dataset dataset;
		try {
			dataset = ij.scifio().datasetIO().open(path.getAbsolutePath());
			// show the image
			ij.ui().show(dataset);
			// invoke the plugin
			ij.command().run(CsvOverlay_Plugin.class, true);

		} catch (IOException exc) {
			System.out.println("LUL!");
		}
	
		System.out.println("DOGE!");
	}

}
