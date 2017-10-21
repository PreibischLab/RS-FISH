package test.radialsymmetry;

import java.io.File;
import java.io.IOException;

import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.real.FloatType;

import gui.Radial_Symmetry;
import test.TestGauss3d;
import util.ImgLib2Util;

public class TestRun {
	
	public static void triggerTest() {
		String folder = "src/main/resources/rs-test/";
		int numTest = 4;
		File[] files = new File[numTest];
		files[0] = new File(folder + "Test-2D.tif");
		files[1] = new File(folder + "Test-3D.tif");
		files[2] = new File(folder + "Test-2D+time.tif");
		files[3] = new File(folder + "Test-3D+time.tif");

		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		Dataset[] dataset = new Dataset[numTest];
		try {
			for (int d = 0; d < numTest; d++) {
				// open the image
				dataset[d] = ij.scifio().datasetIO().open(files[d].getAbsolutePath());
				// show the image
				ij.ui().show(dataset[d]);
			}

			// invoke the plugin
			ij.command().run(Radial_Symmetry.class, true);
		} catch (IOException exc) {
			System.out.println("LUL!");
		}
	}

	public static void main(String[] args) {
		// triggerTest();
		GenerateImages.generateImages();
		System.out.println("Doge!");
	}

}
