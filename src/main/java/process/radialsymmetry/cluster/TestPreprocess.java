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

		Preprocess.runPreprocess(new File("/home/milkyklim/Desktop/test")); 
		
		System.out.println("Doge!");
	}
}
