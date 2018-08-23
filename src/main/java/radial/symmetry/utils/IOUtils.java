package radial.symmetry.utils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import net.imglib2.RealPoint;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

import fitting.Spot;
import util.opencsv.CSVReader;
import util.opencsv.CSVWriter;

public class IOUtils {
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
	
	public static boolean checkPaths(ArrayList<File> filepaths) {
		boolean isFine = true;
		
		for (File path : filepaths)
			if (!path.exists()){
				isFine = false;
				break;
			}
		
		return isFine;
	}
	
}
