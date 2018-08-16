package gui.test.start;

import java.io.File;
import java.io.IOException;

import net.imagej.Dataset;

import gui.anisotropy.plugin.Anisotropy_Plugin;

public class TestAnisotropyPlugin {
	
	public static void main(String[] args)
	{
		File path = new File("/Users/kkolyva/Desktop/wow-desktop/2017-11-17-resort/2017-09-20-hackathon-dresden-projects/2017-09-20-anisotropy-fix/beads-cropped.tif");

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
			ij.command().run(Anisotropy_Plugin.class, true);

		} catch (IOException exc) {
			System.out.println("LUL!");
		}
	
		System.out.println("DOGE!");
	}
}
