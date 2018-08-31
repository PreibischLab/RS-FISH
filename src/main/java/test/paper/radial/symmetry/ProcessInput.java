package test.paper.radial.symmetry;

import java.util.ArrayList;
import java.util.Random;

import net.imagej.ImageJ;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.real.FloatType;

import ij.io.FileSaver;
import test.TestGauss3d;

public class ProcessInput {
	// generate the 2D image
	public static void generate2dRandom(String path, String imgName, String posName, long [] dims, double [] sigma, long numSpots, int seed, boolean padding) {
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
				minValue[d] = 4*sigma[d] + 1;
				maxValue[d] = dims[d] - 4*sigma[d] - 1;
			}
		}

		// will be used to save the data
		ArrayList <double[]> positions = new ArrayList<>();
		for (int i = 0; i < numSpots; ++i) {
			final double[] pos = new double[numDimensions];
			// small adjustment to have a padding around all points
			// (int)(Math.random() * ((Max - Min) + 1)) + Min
			for (int d = 0; d < numDimensions; d++)
				pos[d] = rnd.nextDouble() * ((maxValue[d] - minValue[d]) + 1) + minValue[d];
			positions.add(pos);
			TestGauss3d.addGaussian(img, pos, sigma, 1, false); // false indicates to take the max intensity instead of the cumulative
		}
		
		// save data to .csv file
		String fullPosPath = path + posName + "-" + numSpots + "-" + sigma[0] + "-" + sigma[1] + "-" + seed + ".csv";
		IOFunctions.writeCSV(positions, fullPosPath);
		// show the resulting image
		ImageJFunctions.show(img).setTitle(imgName);
		// saving part
		FileSaver fs = new FileSaver(ImageJFunctions.wrap(img, imgName));
		String fullImgPath = path + imgName + "-" + numSpots + "-" + sigma[0] + "-" + sigma[1] + "-" + seed + ".tif";
		fs.saveAsTiff(fullImgPath);
		
	}

	// generate the 2D image
	public static void generate3dRandom(String path, String imgName, String posName, long [] dims, double [] sigma, long numSpots, int seed, boolean padding) {
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
				minValue[d] = 4*sigma[d] + 1;
				maxValue[d] = dims[d] - 4*sigma[d] - 1;
			}
		}

		// will be used to save the data
		ArrayList <double[]> positions = new ArrayList<>();
		for (int i = 0; i < numSpots; ++i) {
			final double[] pos = new double[numDimensions];
			// small adjustment to have a padding around all points
			// (int)(Math.random() * ((Max - Min) + 1)) + Min
			for (int d = 0; d < numDimensions; d++)
				pos[d] = rnd.nextDouble() * ((maxValue[d] - minValue[d]) + 1) + minValue[d];
			positions.add(pos);
			TestGauss3d.addGaussian(img, pos, sigma, 1, false); // false indicates to take the max intensity instead of the cumulative
		}
		
		// save data to .csv file
		String fullPosPath = path + posName + "-" + numSpots + "-" + sigma[0] + "-" + sigma[1] + "-" + seed + ".csv";
		IOFunctions.writeCSV(positions, fullPosPath);
		// show the resulting image
		ImageJFunctions.show(img).setTitle(imgName);
		// saving part
		FileSaver fs = new FileSaver(ImageJFunctions.wrap(img, imgName));
		String fullImgPath = path + imgName + "-" + numSpots + "-" + sigma[0] + "-" + sigma[1] + "-" + seed + ".tif";
		fs.saveAsTiff(fullImgPath);
		
	}
	
	public static void runGenerate2dRandom() {
		InputParamsGenerated ipg = new InputParamsGenerated("", 0);
		
		String path = ipg.path;
		String imgName = ipg.imgName;
		String posName = ipg.posName;
		long [] dims = ipg.dims;
		double [] sigma = ipg.sigma; 
		long numSpots = ipg.numSpots;
		int seed = ipg.seed;
		boolean padding = ipg.padding;
		
		generate2dRandom(path, imgName, posName, dims, sigma, numSpots, seed, padding);
	}
	
	public static void runGenerate3dRandom() {
		InputParamsGenerated ipg = new InputParamsGenerated("", 1);
		
		String path = ipg.path;
		String imgName = ipg.imgName;
		String posName = ipg.posName;
		long [] dims = ipg.dims;
		double [] sigma = ipg.sigma; 
		long numSpots = ipg.numSpots;
		int seed = ipg.seed;
		boolean padding = ipg.padding;
		
		generate3dRandom(path, imgName, posName, dims, sigma, numSpots, seed, padding);
	}

	public static void main(String[] args) {
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		runGenerate3dRandom();
		System.out.println("Doge!");
	}

}
