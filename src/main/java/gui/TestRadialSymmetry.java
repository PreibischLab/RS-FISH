package gui;

import java.io.File;
import java.io.IOException;

import net.imagej.Dataset;

/*
 * 
 * New main class for testing, SUCKS
 * */

public class TestRadialSymmetry {

	public static void main(String[] args) {
		File path = new File( "/Users/kkolyva/Desktop/2017-09-20-hackathon-dresden-projects/2017-09-20-anisotropy-fix/Simulated_3D_2x.tif" );
		File path2 = new File( "/Users/kkolyva/Desktop/2017-09-20-hackathon-dresden-projects/2017-09-20-anisotropy-fix/Simulated_3D_4x.tif" );
		// create the ImageJ application context with all available services
		final net.imagej.ImageJ ij = new net.imagej.ImageJ();
		ij.ui().showUI();

		// load the dataset
		Dataset dataset;
		Dataset dataset2;
		try {
			dataset = ij.scifio().datasetIO().open(path.getAbsolutePath());
			dataset2 = ij.scifio().datasetIO().open(path2.getAbsolutePath());
			
			// show the image
			ij.ui().show(dataset);
			ij.ui().show(dataset2);
			// invoke the plugin
			ij.command().run(Radial_Symmetry.class, true);

		} catch (IOException exc) {
			System.out.println("LUL!");
		}
		
		System.out.println("Doge!");
	}
}
