package cluster.radial.symmetry.process;

import io.scif.img.ImgSaver;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;

import fitting.Spot;
import fix.intensity.Intensity;
import gui.radial.symmetry.interactive.HelperFunctions;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.io.FileSaver;
import imglib2.type.numeric.real.normalized.RealTypeNormalization;
import imglib2.type.numeric.real.normalized.TypeTransformingRandomAccessibleInterval;
import radial.symmetry.computation.RadialSymmetry;
import radial.symmetry.parameters.GUIParams;
import radial.symmetry.parameters.RadialSymmetryParameters;
import radial.symmetry.utils.IOUtils;

import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.interpolation.randomaccess.LanczosInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import cluster.radial.symmetry.process.parameters.ParametersFirstRun;
import cluster.radial.symmetry.process.parameters.ParametersSecondRun;
import util.ImgLib2Util;
import util.NotSoUsefulOutput;
import util.opencsv.CSVWriter;

public class BatchProcess {

	// TODO: Refactor, divide into functions
	public static void process(File imgPath, File inputRoiPath, GUIParams params, File outputPathResultCsvBeforeCorrection, File outputPathParameters, File outputPathZCorrected, File outputPath, boolean doZcorrection) {
		Img<FloatType> img = ImgLib2Util.openAs32Bit(imgPath);
		// TODO: might be redundant
		ImagePlus imp = ImageJFunctions.wrap(img, "");
		// convert to 3D stack
		imp.setDimensions(1, imp.getNSlices(), 1);
		// set the calibration for the given image
		double[] calibration = HelperFunctions.initCalibration(imp, imp.getNDimensions()); 
		// set the parameters for the radial symmetry 
		RadialSymmetryParameters rsm = new RadialSymmetryParameters(params, calibration);
		// FIXME: MAYBE WE ACTUALLY HAVE TO
		// don't have to normalize the image and can use it directly

		double[] minmax = HelperFunctions.computeMinMax(img);

		float min = (float) minmax[0];
		float max = (float) minmax[1];

		RandomAccessibleInterval<FloatType> rai;
		if (!Double.isNaN(min) && !Double.isNaN(max)) // if normalizable
			rai = new TypeTransformingRandomAccessibleInterval<>(img,
					new RealTypeNormalization<>(min, max - min), new FloatType());
		else // otherwise use
		{
			// rai = img;
			System.out.println("Can\'t normalize image!");
			return;
		}
		// x y z
		long[] dims = new long[img.numDimensions()];
		img.dimensions(dims);

		// stores the intensity values for gauss fitting
		ArrayList<Float> intensity = new ArrayList<>(0);
		ArrayList<Spot> spots = processImage(img, rai, rsm, dims, params.getSigmaDoG(), intensity);

		// TODO: filter the spots that are inside of the roi
		Img<FloatType> mask = ImgLib2Util.openAs32Bit(inputRoiPath);

		// filtered images
		ArrayList<Float> fIntensity = new ArrayList<>(0);
		ArrayList<Spot> fSpots = new ArrayList<>(0);

		RandomAccess<FloatType> ra = mask.randomAccess();
		for (Spot spot : spots) {
			int x = spot.getIntPosition(0);
			int y = spot.getIntPosition(1);
			// filter spots that are not in the roi
			ra.setPosition(new long[] {x, y});
			if(ra.get().get() > 0){
				int idx = spots.indexOf(spot);

				fSpots.add(spots.get(idx));
				fIntensity.add(intensity.get(idx));
			}
		}

		// we want to save the intensity values that were not corrected yet
		if (outputPathResultCsvBeforeCorrection.getAbsolutePath().endsWith(".csv")) {
			IOUtils.writeSpotPositionsAndIntensitiesToCSV(outputPathResultCsvBeforeCorrection, fSpots, fIntensity);
		}

		// TODO: we don't need to check this - path always exists
		if (!outputPathZCorrected.getAbsolutePath().equals("")){

			int degree = 2; 
			double [] coeff = new double [degree + 1];
			Img<FloatType> fImg = ExtraPreprocess.fixIntensitiesOnlySpotsRansac(img, fSpots, fIntensity, coeff, doZcorrection);
			try {
				// new ImgSaver().saveImg(outputPathZCorrected.getAbsolutePath(), fImg);
				ImagePlus fImp = ImageJFunctions.wrap(fImg, "");
				fImp.setDimensions(1, fImp.getStackSize(), 1);
				IJ.saveAsTiff(fImp, outputPathZCorrected.getAbsolutePath() );
			}
			catch (Exception exc) {
				exc.printStackTrace();
			}

			if (outputPathParameters.getAbsolutePath().endsWith(".csv")) {
				IOUtils.writeParametersToCsv(outputPathParameters, coeff);
			}

			// TODO: filter the spot with the gaussian fit
			IOUtils.writeSpotPositionsAndIntensitiesToCSV(outputPath, fSpots, fIntensity);
		}
	}

	/*
	 * Class to process multiple images in a batch mode
	 * */
	// TODO: polish
	public static ArrayList<Spot> processImage(Img<FloatType> img, RandomAccessibleInterval<FloatType> rai, RadialSymmetryParameters rsm,
		long[] dims, double sigma, ArrayList<Float> intensity) {
		RadialSymmetry rs = new RadialSymmetry(rai, rsm);
		rs.compute();

		// TODO: Check if this part is redundant 
		// TODO: if the detect spot has at least 1 inlier add it
		ArrayList<Spot> filteredSpots = HelperFunctions.filterSpots(rs.getSpots(), 1 );

		// iterate over all points and perform the linear interpolation for each of the spots
		NLinearInterpolatorFactory<FloatType> factory = new NLinearInterpolatorFactory<>();
		// LanczosInterpolatorFactory< FloatType > factory = new LanczosInterpolatorFactory< FloatType >();
		RealRandomAccessible<FloatType> interpolant = Views.interpolate(Views.extendMirrorSingle(img), factory);

		// looks like we are working with the correct image
		// and taking the intensities from the correct place 
		// ImageJFunctions.show(img);
		// ImageJFunctions.show(rai);

		for (Spot fSpot : filteredSpots){
			RealRandomAccess<FloatType> rra = interpolant.realRandomAccess();
			double[] position = fSpot.getCenter();
			rra.setPosition(position);
			intensity.add(new Float(rra.get().get()));
			// [83.85610471462424, 336.9622269595374, 32.396389491090034]
			// FIXME: test purposes only
			// System.out.println(rra.get().get());

		}
		return filteredSpots;
	}

	public static void main(String[] args) {
		System.out.println("DONE!");
	}
}
