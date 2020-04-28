package radial.symmetry.utils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import com.opencsv.CSVReader;

import net.imglib2.RealPoint;

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
}
