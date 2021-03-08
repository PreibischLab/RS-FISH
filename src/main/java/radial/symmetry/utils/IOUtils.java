package radial.symmetry.utils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import com.opencsv.CSVReader;

import ij.IJ;
import net.imglib2.RealPoint;

public class IOUtils {
	public static int getNumDimensions(File filepath) {
		int numDimensions = 0;
		try {
			CSVReader reader = new CSVReader(new FileReader(filepath)); // we don't care about the delimiter
			String[] nextLine = reader.readNext();
			numDimensions = nextLine.length; 
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return numDimensions;
	}
	
	// IMP: this reader assumes that images are 3D-stacks {x, y, z}
	public static ArrayList<RealPoint> readPositionsFromCSV(File filepath){
		ArrayList<RealPoint> peaks = new ArrayList<>();
		String[] nextLine;
		int numDimensions = getNumDimensions(filepath) - 3;
		IJ.log( "numDimensions=" + numDimensions );
		try {
			CSVReader reader = new CSVReader(new FileReader(filepath)); // figure the delimiter yourself
			int i = 0;
			while ((nextLine = reader.readNext()) != null) {
				++i;
				double [] pos = new double[numDimensions];
				for (int d = 0; d < numDimensions; d++)
				{
					try
					{
						pos[d] = Double.parseDouble(nextLine[d]);
					}
					catch (Exception e ) { IJ.log( "no entries in line: " + i ); break; }
				}
				peaks.add(new RealPoint(pos));
			}
			reader.close();
			IJ.log( "read " + peaks.size() + " spots." );
		} catch (IOException e) {
			e.printStackTrace();
		}

		return peaks;
	}

	public static void main( String[] args )
	{
		readPositionsFromCSV( new File( "/Users/spreibi/Documents/BIMSB/Publications/radialsymmetry/N2_702_cropped_1620 (high SNR)_ch0.csv" ) );
	}
}
