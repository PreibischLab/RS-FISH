package test.radial.symmetry;

import java.util.Random;

import net.imagej.ImageJ;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import test.TestGauss3d;

public class GenerateImages {
	public static void generateImages() {
		String folder = "/Users/kkolyva/Desktop/2017-10-06-rs-test/";

		int numTest = 4;
		String[] files = new String[numTest];
		files[0] = "test-random-xy.tif";
		files[1] = "test-random-xyz.tif";
		files[2] = "test-random-xyt.tif";
		files[3] = "test-random-xyzt.tif";

		generateAllRandomSpot(folder, files);
		// generateAllOneSpot(folder, files);
	}

	public static void generateAllRandomSpot(String folder, String[] files) {
		generate2dRandom(folder, files[0]);
		generate3dRandom(folder, files[1]);
		generate2dTimeRandom(folder, files[2]);
		generate3dTimeRandom(folder, files[3]);
	}

	public static void generate2dRandom(String path, String name) {
		final long[] dims = new long[] { 128, 128 };
		final Img<FloatType> img = new ArrayImgFactory<FloatType>().create(dims, new FloatType());
		final Random rnd = new Random(42);

		final double[] sigma = new double[] { 2, 2 };
		double minValue = 0;
		double maxValue = 127;

		for (int i = 0; i < 100; ++i) {
			// small adjustment to have a padding around all points
			// (int)(Math.random() * ((Max - Min) + 1)) + Min
			double x = rnd.nextDouble() * ((maxValue - minValue) + 1) + minValue;
			double y = rnd.nextDouble() * ((maxValue - minValue) + 1) + minValue;

			final double[] location = new double[] { x, y };
			TestGauss3d.addGaussian(img, location, sigma);
		}

		ImageJFunctions.show(img).setTitle(name);
	}

	public static void generate3dRandom(String path, String name) {
		final long[] dims = new long[] { 128, 128, 128 };
		final Img<FloatType> img = new ArrayImgFactory<FloatType>().create(dims, new FloatType());
		final Random rnd = new Random(42);
		final double[] sigma = new double[] { 2, 2, 2 };

		double minValue = 0;
		double maxValue = 128;

		for (int i = 0; i < 100; ++i) {
			// small adjustment to have a padding around all points
			// (int)(Math.random() * ((Max - Min) + 1)) + Min
			double x = rnd.nextDouble() * ((maxValue - minValue) + 1) + minValue;
			double y = rnd.nextDouble() * ((maxValue - minValue) + 1) + minValue;
			double z = rnd.nextDouble() * ((maxValue - minValue) + 1) + minValue;

			final double[] location = new double[] { x, y, z };
			TestGauss3d.addGaussian(img, location, sigma);
		}

		ImageJFunctions.show(img).setTitle(name);
	}

	public static void generate2dTimeRandom(String path, String name) {
		final long[] dims = new long[] { 128, 128, 10 };
		final Img<FloatType> img = new ArrayImgFactory<FloatType>().create(dims, new FloatType());
		final Random rnd = new Random(42);
		final double[] sigma = new double[] { 2, 2 };

		int time = 2;

		double minValue = 0;
		double maxValue = 127;

		for (int i = 0; i < 100; ++i) {
			// small adjustment to have a padding around all points
			// (int)(Math.random() * ((Max - Min) + 1)) + Min
			double x = rnd.nextDouble() * ((maxValue - minValue) + 1) + minValue;
			double y = rnd.nextDouble() * ((maxValue - minValue) + 1) + minValue;
			int t = (int) (rnd.nextDouble() * (9 + 1) + 0);

			final double[] location = new double[] { x, y };
			TestGauss3d.addGaussian(Views.hyperSlice(img, time, t), location, sigma);
		}

		ImageJFunctions.show(img).setTitle(name);
	}

	public static void generate3dTimeRandom(String path, String name) {
		final long[] dims = new long[] { 128, 128, 128, 10 };
		final Img<FloatType> img = new ArrayImgFactory<FloatType>().create(dims, new FloatType());
		final Random rnd = new Random(42);
		final double[] sigma = new double[] { 2, 2, 2 };

		int time = 3;

		double minValue = 0;
		double maxValue = 127;

		for (int i = 0; i < 100; ++i) {
			// small adjustment to have a padding around all points
			// (int)(Math.random() * ((Max - Min) + 1)) + Min
			double x = rnd.nextDouble() * ((maxValue - minValue) + 1) + minValue;
			double y = rnd.nextDouble() * ((maxValue - minValue) + 1) + minValue;
			double z = rnd.nextDouble() * ((maxValue - minValue) + 1) + minValue;
			int t = (int) (rnd.nextDouble() * (9 + 1) + 0);

			final double[] location = new double[] { x, y, z };
			TestGauss3d.addGaussian(Views.hyperSlice(img, time, t), location, sigma);
		}

		ImageJFunctions.show(img).setTitle(name);
	}

	public static void generateAllOneSpot(String folder, String[] files) {
		generate2dOne(folder, files[0]);
		generate3dOne(folder, files[1]);
		generate2dTimeOne(folder, files[2]);
		generate3dTimeOne(folder, files[3]);
	}

	public static void generate2dOne(String path, String name) {
		final long[] dims = new long[] { 128, 128 };
		final double[] location = new double[] { 50.5, 43.6 };
		final double[] sigma = new double[] { 2, 2 };

		final Img<FloatType> img = new ArrayImgFactory<FloatType>().create(dims, new FloatType());
		TestGauss3d.addGaussian(img, location, sigma, 1);
		ImageJFunctions.show(img).setTitle(name);
	}

	public static void generate3dOne(String path, String name) {
		final long[] dims = new long[] { 128, 128, 128 };
		final double[] location = new double[] { 50.5, 43.6, 100.4 };
		final double[] sigma = new double[] { 2, 2, 2 };

		final Img<FloatType> img = new ArrayImgFactory<FloatType>().create(dims, new FloatType());
		TestGauss3d.addGaussian(img, location, sigma, 1);
		ImageJFunctions.show(img).setTitle(name);
	}

	public static void generate2dTimeOne(String path, String name) {
		final long[] dims = new long[] { 128, 128, 10 };
		final double[] location = new double[] { 50.5, 43.6 };
		final double[] sigma = new double[] { 2, 2 };

		final Img<FloatType> img = new ArrayImgFactory<FloatType>().create(dims, new FloatType());
		int time = 2;
		int timeSlice = 5;

		TestGauss3d.addGaussian(Views.hyperSlice(img, time, timeSlice), location, sigma, 1);
		ImageJFunctions.show(img).setTitle(name);
	}

	public static void generate3dTimeOne(String path, String name) {
		final long[] dims = new long[] { 128, 128, 128, 10 };
		final double[] location = new double[] { 50.5, 43.6, 100.4 };
		final double[] sigma = new double[] { 2, 2, 2 };

		final Img<FloatType> img = new ArrayImgFactory<FloatType>().create(dims, new FloatType());
		int time = 3;
		int timeSlice = 5;

		TestGauss3d.addGaussian(Views.hyperSlice(img, time, timeSlice), location, sigma, 1);
		ImageJFunctions.show(img).setTitle(name);
	}

	public static void main(String[] args) {
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		generateImages();
		System.out.println("Doge!");
	}

}
