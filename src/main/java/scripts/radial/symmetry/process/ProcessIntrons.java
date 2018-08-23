package scripts.radial.symmetry.process;

import java.io.File;
import java.io.FileReader;
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

import cluster.radial.symmetry.process.IOFunctions;
import cluster.radial.symmetry.process.ImageData;
import radial.symmetry.utils.IOUtils;
import util.ImgLib2Util;
import util.opencsv.CSVReader;

public class ProcessIntrons {

	// process the exon signal to compute the intron signal around the exon one

	// same as process image but for all good images
	public static void processImages(File intronImagesFolderPath, File exonFolderPath, File intronFolderPath, File smFishDbPath) {
		// read the list of the good exon images 
		// imageData contains the information about all images
		ArrayList<ImageData> imageData = readDb(smFishDbPath);
		// find the corresponding intron images 

		// run processing on each image 

		// 
	}

	public static ArrayList <ImageData> readDb(File databasePath) {
		ArrayList <ImageData> imageData = new ArrayList<>(); 
		// some constants
		final int nColumns = 24;

		final String [] channels = new String[] {"C0", "C1", "C2", "C3", "C4"};

		final HashMap<String, Integer> lambdaIndices = new HashMap<>();
		lambdaIndices.put(channels[0], 2);
		lambdaIndices.put(channels[1], 5);
		lambdaIndices.put(channels[2], 8);
		lambdaIndices.put(channels[3], 11);
		lambdaIndices.put(channels[4], 14);

		final HashMap<String, Integer> stainIndices = new HashMap<>();
		stainIndices.put(channels[0], 3);
		stainIndices.put(channels[1], 6);
		stainIndices.put(channels[2], 9);
		stainIndices.put(channels[3], 12);
		stainIndices.put(channels[4], 15);

		final HashMap<String, Integer> paramIndices = new HashMap<>();
		paramIndices.put("signal", 17);
		paramIndices.put("integrity", 18);
		paramIndices.put("stage", 19);
		paramIndices.put("comment", 20); // quality
		paramIndices.put("new filename", 23);

		try {
			int toSkip = 1; // skip header
			CSVReader reader = new CSVReader(new FileReader(databasePath), ',', CSVReader.DEFAULT_QUOTE_CHARACTER, toSkip);
			String[] nextLine = new String [nColumns];
			// while there are rows in the file
			while ((nextLine = reader.readNext()) != null) {
				// parse the row; that is 25 elements long
				for (String channel : channels) {
					if (nextLine[stainIndices.get(channel)].equals("FISH") && conditionalPick(nextLine,paramIndices)) {
						int lambda = Integer.parseInt(nextLine[lambdaIndices.get(channel)]);
						String filename = channel + "-"+ nextLine[paramIndices.get("new filename")];
						boolean defects = !nextLine[paramIndices.get("comment")].equals(""); // empty string means no defect
						imageData.add(new ImageData(lambda, defects, filename));
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		for (ImageData id : imageData)
			System.out.println(id.getLambda() + " " + id.getFilename());

		System.out.println("Done reading the database");
		return imageData;
	}
	
	// drop bad quality images
	public static boolean conditionalPick(String[] nextLine, HashMap<String, Integer> paramIndices) {
		// true -> good quality
		// some parameters that are coming from Laura: 
		// integrity == 1 
		// signal in {3,4,5}
		boolean isGood = false;
		int signalQuality = nextLine[paramIndices.get("signal")].trim().equals("") ? 0 : Integer.parseInt(nextLine[paramIndices.get("signal")].trim());
		int integrity = nextLine[paramIndices.get("integrity")].trim().equals("") ? 0 : Integer.parseInt(nextLine[paramIndices.get("integrity")].trim());

		if (signalQuality >= 3 // good signal
				&& integrity == 1 // embryo looks fine
				&& nextLine[paramIndices.get("stage")].trim().equals("E") // only embryos
				&& !nextLine[paramIndices.get("comment")].trim().contains("z jumps") // skip
				&& !nextLine[paramIndices.get("comment")].trim().contains("z cut") // skip
				)
			isGood = true;
		return isGood;
	}
	
	// grab intronImagePath and exonPath and put the detections to the intronPath
	public static void processImage(File intronImagePath, File exonPath, File intronPath) {
		// reading 
		Img<FloatType> img = ImgLib2Util.openAs32Bit(intronImagePath);
		ArrayList<RealPoint> exonSpots = IOUtils.readPositionsFromCSV(exonPath, '\t');
		// processing
		ArrayList<Double> intronIntensity = calculateIntronSignals(exonSpots, img);
		// writing 
		IOUtils.writeIntensitiesToCSV(intronPath, intronIntensity, '\t');
	}

	public static ArrayList<Double> calculateIntronSignals(ArrayList<RealPoint> exonSpots, Img<FloatType> img){
		ArrayList<Double> intronIntensities = new ArrayList<>();

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

			// System.out.println(Util.printCoordinates(min));
			// System.out.println(Util.printCoordinates(max));

			FinalInterval interval = new FinalInterval(min, max);
			double intronIntensity = calulateIntronSignal(exonSpots.get(i), img, interpolant, interval, offset, numPixels);
			intronIntensities.add(intronIntensity);
			// break;
		}

		return intronIntensities;
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

	public static double calulateIntronSignal(RealPoint spot, RandomAccessible<FloatType> img, RealRandomAccessible<FloatType> rImg, FinalInterval interval, double [] offset, long numPixels) {
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

		double intronIntensity = sum.getSum()/numPixels;
		return intronIntensity;
	}


	public static void main(String[] args) {
		String root = "/Users/kkolyva/Desktop/2018-07-31-09-53-32-N2-all-results-together/test";
		// input
		String exonFolder = "csv-2";
		String intronImagesFolder = "median";
		String smFishDb = "smFISH-database/N2-Table 1.csv";
		// output 
		String intronFolder = "csv-dapi-intron";

		File exonFolderPath = Paths.get(root, exonFolder).toFile();
		File intronImagesFolderPath = Paths.get(root, intronImagesFolder).toFile();
		File smFishDbPath = Paths.get(root, smFishDb).toFile();
		File intronFolderPath = Paths.get(root, intronFolder).toFile();


		// IOUtils.checkPaths(...)

		ProcessIntrons.processImages(intronImagesFolderPath, exonFolderPath, intronFolderPath, smFishDbPath);
		System.out.println("DOGE!");
	}

}
