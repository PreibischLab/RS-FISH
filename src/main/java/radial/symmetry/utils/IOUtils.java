package radial.symmetry.utils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import net.imglib2.RealPoint;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.real.FloatType;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

import cluster.radial.symmetry.process.ImageData;
import fitting.Spot;
import ij.IJ;
import util.opencsv.CSVReader;
import util.opencsv.CSVWriter;

public class IOUtils {

	public static ArrayList<ImageData> readCenters(File filePath) {
		ArrayList <ImageData> imageData = new ArrayList<>(); 
		try {
			int toSkip = 1; 
			final int nColumns = 2;

			String[] nextLine = new String [nColumns];
			CSVReader reader = new CSVReader(new FileReader(filePath), ',', CSVReader.DEFAULT_QUOTE_CHARACTER, toSkip);
			// while there are rows in the file
			while ((nextLine = reader.readNext()) != null) {
				String filename = nextLine[0];
				float center = Float.parseFloat(nextLine[1]);
				imageData.add(new ImageData(filename, center));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return imageData;
	}


	public static int getNumDimensions(File filepath, char separator) {
		int numDimensions = 0;
		try {
			CSVReader reader = new CSVReader(new FileReader(filepath), separator); // we don't care about the delimiter
			String[] nextLine = reader.readNext();
			numDimensions = nextLine.length; 
		} catch (IOException e) {
			e.printStackTrace();
		}
		return numDimensions;
	}

	public static void writeParametersToCsv(File path, double [] coeff) {
		try {
			String[] nextLine = new String [coeff.length];
			CSVWriter writer = new CSVWriter(new FileWriter(path.getAbsolutePath()), '\t', CSVWriter.NO_QUOTE_CHARACTER);
			for (int j = 0; j < coeff.length; j++) {
				nextLine[j] = String.valueOf(coeff[j]); // use max precision possible otherwise we don't capture the x^2 coefficient 
			}
			writer.writeNext(nextLine);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void writePositionsAndIntensitiesToCSV(File path, ArrayList<Spot> spots, ArrayList<Float> intensity) {
		try {
			String[] nextLine = new String [5];
			CSVWriter writer = new CSVWriter(new FileWriter(path.getAbsolutePath()), '\t', CSVWriter.NO_QUOTE_CHARACTER);
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

	// IMP: this reader assumes that images are 3D-stacks {x, y, z}
	public static ArrayList<RealPoint> readPositionsFromCSV(File filepath, char separator){
		ArrayList<RealPoint> peaks = new ArrayList<>();
		String[] nextLine;
		int numDimensions = getNumDimensions(filepath, separator) - 2;
		try {
			CSVReader reader = new CSVReader(new FileReader(filepath), separator); // figure the delimiter yourself
			while ((nextLine = reader.readNext()) != null) {
				double [] pos = new double[numDimensions];
				for (int d = 0; d < numDimensions; d++)
					pos[d] = Double.parseDouble(nextLine[d + 1]);
				peaks.add(new RealPoint(pos));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return peaks;
	}

	public static void writeIntensitiesToCSV(File filepath, ArrayList<Double> intensities, char separator) {
		try {
			// throw if can't create the file
			CSVWriter writer = new CSVWriter(new FileWriter(filepath), separator, CSVWriter.NO_QUOTE_CHARACTER);
			String [] nextLine = new String[1]; // this is actually painful
			for (double val : intensities) {
				nextLine[0] = String.valueOf(val);
				writer.writeNext(nextLine);
			}
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static boolean checkPaths(File [] filepaths) {
		boolean isFine = true;

		for (File path : filepaths)
			if (!path.exists()){
				isFine = false;
				break;
			}

		return isFine;
	}

	// TODO: refactor the reading parts; 
	// feels like it would be much easier to have a real database
	public static ArrayList <ImageData> readDb(File databasePath, String [] types) {
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

		final HashMap<String, Integer> typeIndices = new HashMap<>();
		typeIndices.put(channels[0], 4);
		typeIndices.put(channels[1], 7);
		typeIndices.put(channels[2], 10);
		typeIndices.put(channels[3], 13);
		typeIndices.put(channels[4], 16);

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
					if (conditionalPick(nextLine, paramIndices)) {
						for (String type : types) {
							if (nextLine[typeIndices.get(channel)].equals(type)) {
								int lambda = Math.round(Float.parseFloat(nextLine[lambdaIndices.get(channel)]));
								String filename = channel + "-"+ nextLine[paramIndices.get("new filename")];
								boolean defects = !nextLine[paramIndices.get("comment")].equals(""); // empty string means no defect
								// String type = nextLine[typeIndices.get(channel)];
								imageData.add(new ImageData(lambda, type, defects, filename));
							}
						}
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

		final HashMap<String, Integer> typeIndices = new HashMap<>();
		typeIndices.put(channels[0], 4);
		typeIndices.put(channels[1], 7);
		typeIndices.put(channels[2], 10);
		typeIndices.put(channels[3], 13);
		typeIndices.put(channels[4], 16);

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
					if (nextLine[stainIndices.get(channel)].equals("FISH") && conditionalPick(nextLine, paramIndices)) {
						int lambda = Math.round(Float.parseFloat(nextLine[lambdaIndices.get(channel)]));
						String filename = channel + "-"+ nextLine[paramIndices.get("new filename")];
						boolean defects = !nextLine[paramIndices.get("comment")].equals(""); // empty string means no defect
						String type = nextLine[typeIndices.get(channel)];
						imageData.add(new ImageData(lambda, type, defects, filename));
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

//		for (ImageData id : imageData)
//			System.out.println(id.getLambda() + " " + id.getFilename());

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

		System.out.println();

		int signalQuality = nextLine[paramIndices.get("signal")].trim().equals("") ? 0 : Math.round(Float.parseFloat(nextLine[paramIndices.get("signal")].trim()));
		int integrity = nextLine[paramIndices.get("integrity")].trim().equals("") ? 0 : Math.round(Float.parseFloat(nextLine[paramIndices.get("integrity")].trim()));

		if (signalQuality >= 3 // good signal
				&& integrity == 1 // embryo looks fine
				&& nextLine[paramIndices.get("stage")].trim().equals("E") // only embryos
				// && !nextLine[paramIndices.get("comment")].trim().contains("z jumps") // skip
				// && !nextLine[paramIndices.get("comment")].trim().contains("z cut") // skip
				&& nextLine[paramIndices.get("comment")].trim().isEmpty() // IMP: includes 2 above!
				)
			isGood = true;
		return isGood;
	}

}
