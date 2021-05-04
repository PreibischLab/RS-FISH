package radial.symmetry.utils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import gui.interactive.HelperFunctions;
import net.imglib2.RealPoint;

public class IOUtils {
	public static int getNumDimensions(File filepath) {
		int numDimensions = 0;
		try {
			CSVReader reader = new CSVReader(new FileReader(filepath)); // we don't care about the delimiter
			String[] nextLine = reader.readNext();
			if ( !nextLine[ 0 ].equals( "x" ) )
				numDimensions = nextLine.length+2; //x,y,z,i (from Tim)
			else
				numDimensions = nextLine.length; 
			reader.close();
		} catch (IOException | CsvValidationException e) {
			e.printStackTrace();
		}
		return numDimensions;
	}
	
	// IMP: this reader assumes that images are 3D-stacks {x, y, z}
	public static ArrayList<RealPoint> readPositionsFromCSV(File filepath){
		ArrayList<RealPoint> peaks = new ArrayList<>();
		String[] nextLine;
		int numDimensions = getNumDimensions(filepath) - 3;
		HelperFunctions.log( "numDimensions=" + numDimensions );
		try {
			CSVReader reader = new CSVReader(new FileReader(filepath)); // figure the delimiter yourself
			int i = 0;
			while ((nextLine = reader.readNext()) != null) {
				++i;
				double [] pos = new double[numDimensions];
				boolean failed = false;
				for (int d = 0; d < numDimensions; d++)
				{
					try
					{
						pos[d] = Double.parseDouble(nextLine[d]);
					}
					catch (Exception e ) { failed = true; HelperFunctions.log( "no entries in line: " + i ); break; }
				}
				if ( !failed )
					peaks.add(new RealPoint(pos));
			}
			reader.close();
			HelperFunctions.log( "read " + peaks.size() + " spots." );
		} catch (IOException | CsvValidationException e) {
			e.printStackTrace();
		}

		return peaks;
	}

	public static void main( String[] args )
	{
		readPositionsFromCSV( new File( "/Users/spreibi/Documents/BIMSB/Publications/radialsymmetry/N2_702_cropped_1620 (high SNR)_ch0.csv" ) );
	}
}
