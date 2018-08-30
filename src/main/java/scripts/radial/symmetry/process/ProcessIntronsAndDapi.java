package scripts.radial.symmetry.process;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessible;
import net.imglib2.RealPoint;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.Img;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.RealSum;
import net.imglib2.view.Views;

import cluster.radial.symmetry.process.ImageData;
import cluster.radial.symmetry.process.ImageDataFull;
import radial.symmetry.utils.IOUtils;
import util.ImgLib2Util;
import util.NotSoUsefulOutput;

public class ProcessIntronsAndDapi {

	// take smFISH detections in exon channel and find intensities for the corresponding locations 
	// in intron and dapi channels (in general, any other channel)

	public static void setupProcess(String root, String datasetDbFile) {
		// [x] create all the dirs if they are missing 
		// [x] check that all necessary dirs are there 
		File smFishDbFilename = Paths.get(root, "smFISH-database", "datasetDbFile").toFile();
		String [] folders = new String [] {"csv-2", "median", "roi", "channels", "normalized","csv-dapi-intron"};
		int numFolders = folders.length + 1;
		
		File [] allPaths = new File[numFolders];
		allPaths[numFolders - 1] = smFishDbFilename;
		
		for (int j = 0; j < numFolders - 1; j++) {
			allPaths[j] = Paths.get(root, folders[j]).toFile();
			if(!allPaths[j].exists()) {
					allPaths[j].mkdirs();
					System.out.println("Created: " + allPaths[j].getAbsolutePath());
			}
		}
		NotSoUsefulOutput.printFiles(allPaths);
	}
	
	public static void createInputArguments() {
		// [ ] create the file with the triplets that should be processed
	}
	
	// same as process image but for all good images
	public static void processImages(String root) {
		File smFishDbFilename = Paths.get(root, "smFISH-database", "SEA-12-Table 1.csv").toFile();

		boolean doFilter = true;
		ArrayList<ImageDataFull> imageDataFull = IOUtils.readDbFull(smFishDbFilename, doFilter);

		// String root = "/Users/kkolyva/Desktop/2018-07-31-09-53-32-N2-all-results-together/test";
		String dataExt = ".csv";
		String imgExt = ".tif";

		final String exon = "DPY-23_EX";
		final String dapi = "DAPI"; 
		final String intron = "DPY-23_INT";

		for (ImageDataFull idf : imageDataFull) {
			try {
				if (idf.getChannels().containsKey(exon) && idf.getChannels().containsKey(dapi) && idf.getChannels().containsKey(intron)) {
					// in : exonCsv, intronImage, dapiImage
					File exonCsvPath = Paths.get(root, "csv-2", getFullName(idf.getChannel(exon), idf.getFilename(), dataExt)).toFile();
					File intronImagePath = Paths.get(root, "median", getFullName(idf.getChannel(intron), idf.getFilename(), imgExt)).toFile();
					File dapiImagePath = Paths.get(root, "channels", getFullName(idf.getChannel(dapi), idf.getFilename(), imgExt)).toFile();
					// extra input
					File maskDapiPath = Paths.get(root, "roi", idf.getFilename() + imgExt).toFile();
					// out: intronCsv, dapiCsv
					File intronCsvPath = Paths.get(root, "csv-dapi-intron", getFullName(idf.getChannel(intron), idf.getFilename(), dataExt)).toFile();
					File dapiCsvPath = Paths.get(root, "csv-dapi-intron", getFullName(idf.getChannel(dapi), idf.getFilename(), dataExt)).toFile();
					File mergedCsvPath = Paths.get(root, "csv-dapi-intron", idf.getFilename() + dataExt).toFile();

					// extra output
					File normalizedDapiPath = Paths.get(root, "normalized", getFullName(idf.getChannel(dapi), idf.getFilename(), imgExt)).toFile();

					File [] allPaths = new File[] {exonCsvPath, intronImagePath, dapiImagePath, maskDapiPath};

					NotSoUsefulOutput.printFiles(allPaths);

					// System.out.println(maskDapiPath.getAbsolutePath());

					boolean allPathsAreCorrect = IOUtils.checkPaths(allPaths);
					if (!allPathsAreCorrect)
						continue; 

					System.out.println(exonCsvPath.getAbsolutePath());

					PreprocessIntronAndDapi.normalizeAndSave(dapiImagePath, maskDapiPath, normalizedDapiPath);
					ProcessIntronsAndDapi.processImage(normalizedDapiPath, exonCsvPath, dapiCsvPath);
					ProcessIntronsAndDapi.processImage(intronImagePath, exonCsvPath, intronCsvPath);
					IOUtils.mergeExonIntronDapiAndWriteToCsv(exonCsvPath, intronCsvPath, dapiCsvPath, mergedCsvPath, '\t');
				}
			}
			catch(Exception e) {
				// exception happened
			}

		}


		// 
		//NotSoUsefulOutput.printImageDataFullParameters(imageDataFull);

	}

	public static String getFullName(String channel, String filename, String ext) {
		return String.format("%s-%s%s", channel, filename, ext);
	}

	// return the triplets corresponding to the same image 
	public static void assembleCorrespondingChannels() {

	}



	// TODO: check this might be old and not used
	// pick images of the specific stain type and without the defects
	public static HashMap<String, ImageData> filterImageData(ArrayList<ImageData> imageData, String type) {
		HashMap<String, ImageData> filteredImageData = new HashMap<>();

		for (ImageData id : imageData) {
			if (!id.getDefects() && id.getType().equals(type))
				filteredImageData.put(id.getFilename(), id);
		} 

		return filteredImageData;
	}

	// grab anotherChannelImagePath and exonPath and put the detections to the intronPath
	public static void processImage(File anotherChannelImagePath, File exonPath, File anotherChannelPath) {
		// reading 
		Img<FloatType> img = ImgLib2Util.openAs32Bit(anotherChannelImagePath);
		ArrayList<RealPoint> exonSpots = IOUtils.readPositionsFromCSV(exonPath, '\t');
		// processing
		ArrayList<Double> allIntensities = calculateAnotherChannelSignals(exonSpots, img);
		// writing 
		// IOUtils.writeIntensitiesToCSV(anotherChannelPath, allIntensities, '\t');
		IOUtils.writePositionsAndIntensitiesToCSV(anotherChannelPath, exonSpots, allIntensities);
	}

	public static ArrayList<Double> calculateAnotherChannelSignals(ArrayList<RealPoint> exonSpots, Img<FloatType> img){
		ArrayList<Double> allIntesities = new ArrayList<>();

		int numDimensions = img.numDimensions();

		NLinearInterpolatorFactory< FloatType > factory = new NLinearInterpolatorFactory<>();
		RealRandomAccessible< FloatType > interpolant = Views.interpolate(Views.extendMirrorSingle( img ), factory);

		int [] kernelSize = new int[] {5, 5, 3};
		long [] min = new long[numDimensions];
		long [] max = new long[numDimensions];
		double [] offset = new double[numDimensions];

		long numPixels = getNumberPixels(kernelSize);

		for (int i = 0; i < exonSpots.size(); i++) {
			RealPoint spot = exonSpots.get(i);
			offset = getOffset(spot);

			for (int d = 0; d < img.numDimensions(); d++) {
				min[d] = (long)spot.getDoublePosition(d) - kernelSize[d]/2;
				max[d] = (long)spot.getDoublePosition(d) + kernelSize[d]/2;
			}

			// DEBUG: remove once done
			// System.out.println(Util.printCoordinates(min));
			// System.out.println(Util.printCoordinates(max));

			FinalInterval interval = new FinalInterval(min, max);
			double intensity = calulateAnotherChannelSignal(exonSpots.get(i), img, interpolant, interval, offset, numPixels);
			allIntesities.add(intensity);
			// break;
		}

		return allIntesities;
	}

	public static long getNumberPixels(int[] kernelSize) {
		long res = 1;
		for (int d = 0; d < kernelSize.length; d++)
			res *= kernelSize[d];
		return res;
	}


	public static double[] getOffset(RealPoint spot) {
		double [] offset = new double[spot.numDimensions()];
		for (int d = 0; d < spot.numDimensions(); d++)
			offset[d] = spot.getDoublePosition(d) - (long) spot.getDoublePosition(d);  
		return offset;
	}

	public static double calulateAnotherChannelSignal(RealPoint spot, RandomAccessible<FloatType> img, RealRandomAccessible<FloatType> rImg, FinalInterval interval, double [] offset, long numPixels) {
		Cursor<FloatType> cursor = Views.interval(img, interval).cursor();
		RealRandomAccess<FloatType> rra = rImg.realRandomAccess();

		RealSum sum = new RealSum();

		while(cursor.hasNext()){
			cursor.fwd();

			double [] position = new double[spot.numDimensions()];

			cursor.localize(position);

			rra.setPosition(position);
			rra.move(offset);
			rra.localize(position);

			sum.add(rra.get().get());
		}

		double intensity = sum.getSum()/numPixels;
		return intensity;
	}


	public static void main(String[] args) {

		String root = "/Volumes/1TB/2018-06-14-12-36-00-N2-full-stack";
		String smFishDbFilename = "SEA-12-Table 1.csv";

		File anotherChannelImagesPath = new File ("");
		File exonFolderPath = new File ("");
		File anotherChannelFolderPath = new File (""); 
		// File smFishDbPath = Paths.get(root, smFishDbFilename).toFile();

		setupProcess(root, smFishDbFilename);
		
		// processImages(root);

		System.out.println("DOGE!");
	}

}
