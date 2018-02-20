package paper.test.radialsymmetry;

import java.util.Random;

import net.imagej.ImageJ;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import ij.ImagePlus;
import ij.io.FileSaver;
import test.TestGauss3d;

public class ProcessInput {
	// either read or generate test images

	// TODO: 
	// [x] Simulated data in 2D 
	// [] Real data in 2D
	// [] Real data in 3D 
	// [] Anisotropic images in 3D

	// generate the 2D image
	public static void generate2dRandom(String path, String name, long [] dims, double [] sigma, long numSpots, int seed, boolean padding) {
		int numDimensions = dims.length;
		final Img<FloatType> img = new ArrayImgFactory<FloatType>().create(dims, new FloatType());
		final Random rnd = new Random(seed);

		double [] minValue = new double[numDimensions];
		double [] maxValue = new double[numDimensions];

		// create padding to fit all the points
		for (int d = 0; d < numDimensions; d++) {
			if (!padding) {
				minValue[d] = 0;
				maxValue[d] = dims[d] - 1;
			}
			else {
				minValue[d] = sigma[d] + 1;
				maxValue[d] = dims[d] - sigma[d] - 1;
			}
		}

		final double[] location = new double[numDimensions];
		for (int i = 0; i < numSpots; ++i) {
			// small adjustment to have a padding around all points
			// (int)(Math.random() * ((Max - Min) + 1)) + Min
			for (int d = 0; d < numDimensions; d++) {
				location[d] = rnd.nextDouble() * ((maxValue[d] - minValue[d]) + 1) + minValue[d];
			}
			TestGauss3d.addGaussian(img, location, sigma, 1, false);
		}

		ImageJFunctions.show(img).setTitle(name);
		// saving part
		FileSaver fs = new FileSaver(ImageJFunctions.wrap(img, name));
		String fullPath = path + name + "-" + numSpots + "-" + sigma[0] + "-" + sigma[1] + "-" + seed + ".tif";
		fs.saveAsTiff(fullPath);
		
	}

	public static void runGenerate2dRandom() {
		String path = "/Users/kkolyva/Desktop/";
		String name = "test-image";
		long [] dims = new long[] {512, 512};
		double [] sigma = new double[] {3, 3}; 
		long numSpots = 800;
		int seed = 42;
		boolean padding = false;
		
		generate2dRandom(path, name, dims, sigma, numSpots, seed, padding);
	}

	public static void main(String[] args) {
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		runGenerate2dRandom();
		System.out.println("Doge!");
	}

}
