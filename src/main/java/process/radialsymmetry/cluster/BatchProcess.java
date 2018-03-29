package process.radialsymmetry.cluster;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import compute.RadialSymmetry;
import fit.Spot;
import gui.interactive.HelperFunctions;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.Roi;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import parameters.GUIParams;
import parameters.RadialSymmetryParameters;
import util.ImgLib2Util;
import util.opencsv.CSVWriter;

public class BatchProcess {

	public static void runProcess(String imgPath, GUIParams params, File outputPath) {
		process(imgPath, params, outputPath);
	}

	public static void process(String imgPath, GUIParams params, File outputPath) {
		Img<FloatType> img = ImgLib2Util.openAs32Bit(new File(imgPath));

		ImagePlus imp = ImageJFunctions.wrap(img, "");
		// convert to 3D stack
		imp.setDimensions(1, imp.getNSlices(), 1);
		// set the calibration for the given image
		double[] calibration = HelperFunctions.initCalibration(imp, imp.getNDimensions()); 
		// set the parameters for the radial symmetry 
		RadialSymmetryParameters rsm = new RadialSymmetryParameters(params, calibration);
		// don't have to normalize the image and can use it directly
		RandomAccessibleInterval<FloatType> rai = img;

		// x y z
		long[] dims = new long[img.numDimensions()];
		img.dimensions(dims);

		// stores the intensity values for gauss fitting
		ArrayList<Float> intensity = new ArrayList<>(0);
		ArrayList<Spot> spots = processImage(img, img, rsm, dims, params.getSigmaDoG(), intensity);

		// TODO: filter the spots that are inside of the roi
		ImagePlus impRoi = IJ.openImage(imgPath);
		Roi roi = impRoi.getRoi();

		// filtered images
		ArrayList<Float> fIntensity = new ArrayList<>(0);
		ArrayList<Spot> fSpots = new ArrayList<>(0);

		if (roi == null) {
			System.out.println("smth is wrong. roi is null");
		}
		else {
			for (Spot spot : spots) {
				int x = spot.getIntPosition(0);
				int y = spot.getIntPosition(1);
				// filter spots that are not in the roi
				if (roi.contains(x, y)) {
					int idx = spots.indexOf(spot);

					fSpots.add(spots.get(idx));
					fIntensity.add(intensity.get(idx));
				}
			}

			// TODO: filter the spot with the gaussian fit
			saveResult(outputPath, fSpots, fIntensity);
		}
	}

	/*
	 * Class to process multiple images in a batch mode
	 * */
	public static ArrayList<Spot> processImage(Img<FloatType> img, RandomAccessibleInterval<FloatType> rai, RadialSymmetryParameters rsm,
		long[] dims, double sigma, ArrayList<Float> intensity) {
		RadialSymmetry rs = new RadialSymmetry(rai, rsm);
		rs.compute();

		// TODO: Check if this part is redundant 
		// TODO: if the detect spot has at least 1 inlier add it
		ArrayList<Spot> filteredSpots = HelperFunctions.filterSpots(rs.getSpots(), 1 );

		//  iterate over all points and perform the linear interpolation for each of the spots
		NLinearInterpolatorFactory<FloatType> factory = new NLinearInterpolatorFactory<>();
		RealRandomAccessible<FloatType> interpolant = Views.interpolate(Views.extendMirrorSingle(img), factory);

		for (Spot fSpot : filteredSpots){
			RealRandomAccess<FloatType> rra = interpolant.realRandomAccess();
			double[] position = fSpot.getCenter();
			rra.setPosition(position);
			intensity.add(new Float(rra.get().get()));	
		}
		return filteredSpots;
	}

	public static void saveResult(File path, ArrayList<Spot> spots, ArrayList<Float> intensity) {
		CSVWriter writer = null;
		String[] nextLine = new String [5];

		try {
			writer = new CSVWriter(new FileWriter(path.getAbsolutePath()), '\t', CSVWriter.NO_QUOTE_CHARACTER);
			for (int j = 0; j < spots.size(); j++) {
				double[] position = spots.get(j).getCenter();

				nextLine = new String[]{
					String.valueOf(j + 1), 
					String.format(java.util.Locale.US, "%.2f", position[0]), 
					String.format(java.util.Locale.US, "%.2f", position[1]), 
					String.format(java.util.Locale.US, "%.2f", position[2]),
					String.format(java.util.Locale.US, "%.2f", intensity.get(j))
				}; 	
				writer.writeNext(nextLine);
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
	}
}
