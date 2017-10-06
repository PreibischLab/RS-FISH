package test.radialsymmetry;

import java.io.File;

import net.imagej.ImageJ;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import test.TestGauss3d;
import util.ImgLib2Util;

public class GenerateImages {

	public static void generateImages() {
		String folder = "/Users/kkolyva/Desktop/2017-10-06-rs-test/";

		int numTest = 4;
		String[] files = new String[numTest];
		files[0] = "test-xy.tif";
		files[1] = "test-xyz.tif";
		files[2] = "test-xyt.tif";
		files[3] = "test-xyzt.tif";

		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		generate2d(folder, files[0]);
		generate3d(folder, files[1]);
		generate2dTime(folder, files[2]);
		generate3dTime(folder, files[3]);
	}

	public static void generate2d(String path, String name) {
		final long[] dims = new long[] { 128, 128 };
		final double[] location = new double[] { 50.5, 43.6 };
		final double[] sigma = new double[] { 2, 2 };

		final Img<FloatType> img = new ArrayImgFactory<FloatType>().create(dims, new FloatType());
		TestGauss3d.addGaussian(img, location, sigma, 1);
		ImageJFunctions.show(img).setTitle(name);
	}

	public static void generate3d(String path, String name) {
		final long[] dims = new long[] { 128, 128, 128 };
		final double[] location = new double[] { 50.5, 43.6, 100.4 };
		final double[] sigma = new double[] { 2, 2, 2 };

		final Img<FloatType> img = new ArrayImgFactory<FloatType>().create(dims, new FloatType());
		TestGauss3d.addGaussian(img, location, sigma, 1);
		ImageJFunctions.show(img).setTitle(name);
	}

	public static void generate2dTime(String path, String name) {
		final long[] dims = new long[] { 128, 128, 10 };
		final double[] location = new double[] { 50.5, 43.6 };
		final double[] sigma = new double[] { 2, 2 };

		final Img<FloatType> img = new ArrayImgFactory<FloatType>().create(dims, new FloatType());
		int time = 2;
		int timeSlice = 5;

		TestGauss3d.addGaussian(Views.hyperSlice(img, time, timeSlice), location, sigma, 1);
		ImageJFunctions.show(img).setTitle(name);
	}

	public static void generate3dTime(String path, String name) {
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
