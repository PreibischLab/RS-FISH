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
import imglib2.RealTypeNormalization;
import imglib2.TypeTransformingRandomAccessibleInterval;

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

	// N2 SECOND RUN parameters
	public static GUIParams setParametersN2Second(int lambda) {
		// set the parameters according to the lambda value
		final GUIParams params = new GUIParams();

		// same for all lambda values
		params.setAnisotropyCoefficient(1.08f);
		boolean useRANSAC = true;
		params.setRANSAC(useRANSAC);

		// FIXME: Check that the values for the params are correct
		// Larger support radius smaller number of inliers
		if (lambda == 670) {
			
			// CHECKED THIS VALUES ON THE GOOD LOOKING IMAGE 
			// CHECK ON THE CROWDED ONE
			
			// pre-detection
			params.setSigmaDog(1.50f);
			params.setThresholdDog(0.0081f);
			// detection
			params.setSupportRadius(3);
			params.setInlierRatio(0.37f);
			params.setMaxError(0.5034f);
		} else if(lambda == 610){
			
			// CHECKED THIS VALUES ON THE GOOD LOOKING IMAGE 
			// CHECK ON THE CROWDED ONE
			
			// pre-detection
			params.setSigmaDog(1.50f);
			params.setThresholdDog(0.0081f);
			// detection
			params.setSupportRadius(3);
			params.setInlierRatio(0.37f);
			params.setMaxError(0.5034f);
		} else if(lambda == 570){
			
			// CHECKED THIS VALUES ON THE GOOD LOOKING IMAGE 
			// CHECK ON THE CROWDED ONE
			
			// pre-detection
			params.setSigmaDog(1.50f);
			params.setThresholdDog(0.0081f);
			// detection
			params.setSupportRadius(3);
			params.setInlierRatio(0.37f);
			params.setMaxError(0.5034f);
		} else {
			System.out.println("This is the wave length value that you didn't use before. Check the results carefully");
		}

		return params;
	}
	
	// N2 parameters
	public static GUIParams setParameters(int lambda) {
		// set the parameters according to the lambda value
		final GUIParams params = new GUIParams();

		// same for all lambda values
		params.setAnisotropyCoefficient(1.08f);
		boolean useRANSAC = true;
		params.setRANSAC(useRANSAC);

		// FIXME: Check that the values for the params are correct
		// Larger support radius smaller number of inliers
		if (lambda == 670) {
			
			// CHECKED THIS VALUES ON THE GOOD LOOKING IMAGE 
			// CHECK ON THE CROWDED ONE
			
			// pre-detection
			params.setSigmaDog(1.50f);
			params.setThresholdDog(0.0081f);
			// detection
			params.setSupportRadius(3);
			params.setInlierRatio(0.37f);
			params.setMaxError(0.5034f);
		} else if(lambda == 610){
			
			// CHECKED THIS VALUES ON THE GOOD LOOKING IMAGE 
			// CHECK ON THE CROWDED ONE
			
			// pre-detection
			params.setSigmaDog(1.50f);
			params.setThresholdDog(0.0081f);
			// detection
			params.setSupportRadius(3);
			params.setInlierRatio(0.37f);
			params.setMaxError(0.5034f);
		} else if(lambda == 570){
			
			// CHECKED THIS VALUES ON THE GOOD LOOKING IMAGE 
			// CHECK ON THE CROWDED ONE
			
			// pre-detection
			params.setSigmaDog(1.50f);
			params.setThresholdDog(0.0081f);
			// detection
			params.setSupportRadius(3);
			params.setInlierRatio(0.37f);
			params.setMaxError(0.5034f);
		} else {
			System.out.println("This is the wave length value that you didn't use before. Check the results carefully");
		}

		return params;
	}
	
	// SEA-12 parameters
	public static GUIParams setParameters2(int lambda) {
		// set the parameters according to the lambda value
		final GUIParams params = new GUIParams();

		// same for all lambda values
		params.setAnisotropyCoefficient(1.08f);
		boolean useRANSAC = true;
		params.setRANSAC(useRANSAC);

		// FIXME: Check that the values for the params are correct
		// Larger support radius smaller number of inliers
		if (lambda == 670) {
			
			// CHECKED THIS VALUES ON THE GOOD LOOKING IMAGE 
			// CHECK ON THE CROWDED ONE
			
			// pre-detection
			params.setSigmaDog(1.50f);
			params.setThresholdDog(0.0081f);
			// detection
			params.setSupportRadius(3);
			params.setInlierRatio(0.37f);
			params.setMaxError(0.5034f);
		} else if(lambda == 610){
			
			// CHECKED THIS VALUES ON THE GOOD LOOKING IMAGE 
			// CHECK ON THE CROWDED ONE
			
			// pre-detection
			params.setSigmaDog(1.50f);
			params.setThresholdDog(0.0081f);
			// detection
			params.setSupportRadius(3);
			params.setInlierRatio(0.37f);
			params.setMaxError(0.5034f);
		} else if(lambda == 570){
			
			// CHECKED THIS VALUES ON THE GOOD LOOKING IMAGE 
			// CHECK ON THE CROWDED ONE
			
			// pre-detection
			params.setSigmaDog(1.50f);
			params.setThresholdDog(0.0081f);
			// detection
			params.setSupportRadius(3);
			params.setInlierRatio(0.37f);
			params.setMaxError(0.5034f);
		} else {
			System.out.println("This is the wave length value that you didn't use before. Check the results carefully");
		}

		return params;
	}
	
	public static void runProcess(File pathImagesMedian, File pathDatabase, File pathResultCsv) {
		// parse the db with smFish labels and good looking images
		ArrayList<ImageData> imageData = Preprocess.readDb(pathDatabase);
		
		long currentIndex = 0;
		
		for (ImageData imageD : imageData) {
			currentIndex++;
			// path to the processed image
			String inputImagePath = pathImagesMedian.getAbsolutePath() + "/" + imageD.getFilename() + ".tif";
			System.out.println(currentIndex + "/" + imageData.size());
			if (new File(inputImagePath).exists()){
				// table to store the results for each channel
				String outputPathCsv = pathResultCsv.getAbsolutePath() + "/" + imageD.getFilename() + ".csv";
				// set the params according to the way length
				GUIParams params = setParametersN2Second(imageD.getLambda());
				BatchProcess.process(inputImagePath, params, new File(outputPathCsv));
			}
			else {
				System.out.println("Missing file: " + inputImagePath);
			}
		}
	}

	public static void process(String imgPath, GUIParams params, File outputPath) {
		Img<FloatType> img = ImgLib2Util.openAs32Bit(new File(imgPath));
		ImagePlus imp = ImageJFunctions.wrap(img, "");
		// TODO: might be redundant
		// convert to 3D stack
		imp.setDimensions(1, imp.getNSlices(), 1);
		// set the calibration for the given image
		double[] calibration = HelperFunctions.initCalibration(imp, imp.getNDimensions()); 
		// set the parameters for the radial symmetry 
		RadialSymmetryParameters rsm = new RadialSymmetryParameters(params, calibration);
		// FIXME: MAYBE WE ACTUALLY HAVE TO
		// don't have to normalize the image and can use it directly

		// dirty cast that can't be avoided :(
		double[] minmax = HelperFunctions.computeMinMax(img);

		float min = (float) minmax[0];
		float max = (float) minmax[1];
		
		RandomAccessibleInterval<FloatType> rai;
		if (!Double.isNaN(min) && !Double.isNaN(max)) // if normalizable
			rai = new TypeTransformingRandomAccessibleInterval<>(img,
					new RealTypeNormalization<>(min, max - min), new FloatType());
		else // otherwise use
			rai = img;
		
		// x y z
		long[] dims = new long[img.numDimensions()];
		img.dimensions(dims);

		// stores the intensity values for gauss fitting
		ArrayList<Float> intensity = new ArrayList<>(0);
		ArrayList<Spot> spots = processImage(img, rai, rsm, dims, params.getSigmaDoG(), intensity);

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
				// TODO: this one can be improved if rewritten with getMask() 
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
		
		// test run that the correct values are written to the csv file
		
		
		String outputPathCsv = "/Volumes/1TB/test/test-out/data.csv";
		// set the params according to the way length
		GUIParams params = setParametersN2Second(670);
		String inputImagePath = "/Volumes/1TB/test/2/C1-N2_96-p.tif";
		
		BatchProcess.process(inputImagePath, params, new File(outputPathCsv));
		System.out.println("DONE!");
	}
}
