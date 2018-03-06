package process.radialsymmetry.cluster;

import java.io.File;
import java.io.IOException;

import net.imagej.Dataset;

import gui.radial.symmetry.plugin.Radial_Symmetry;

public class TestPreprocess {
	public static void main(String[] args) {
		// run the test case
//		final net.imagej.ImageJ ij = new net.imagej.ImageJ();
//		ij.ui().showUI();
//
//		try {
//			Dataset dataset = ij.scifio().datasetIO().open("/Users/kkolyva/Downloads/test.tif");
//			ij.ui().show(dataset);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}

		Preprocess.runPreprocess(new File("/Volumes/1TB/2018-03-06-laura-rs-numbers/SEA-12-32-channels"), 
														new File("/Volumes/1TB/2018-03-06-laura-rs-numbers/SEA-12-32-channels-preprocessed")); 
		
		System.out.println("Doge!");
	}
}
