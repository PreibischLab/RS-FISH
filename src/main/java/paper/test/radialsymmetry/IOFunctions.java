package paper.test.radialsymmetry;

import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

public class IOFunctions {
	public static void writeCSV(ArrayList<double []> list, String fullPath) {
		CSVWriter writer = null;

		int numDimensions = list.size() > 0 ? list.get(0).length : 0;
		String[] nextLine = new String[numDimensions];

		try {
			// throw if can't create the file
			writer = new CSVWriter(new FileWriter(fullPath), '\t', CSVWriter.NO_QUOTE_CHARACTER);
			for (double [] element : list) {
				for (int d = 0; d < numDimensions; d++)
					nextLine[d] = String.valueOf(element[d]);
				// System.out.println(nextLine.toString().split(" "));
				writer.writeNext(nextLine);
			}
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void readCSV(ArrayList<double []> list, String fullPath, int numDimensions) {
		CSVReader reader = null;
		String[] nextLine;

		try {
			reader = new CSVReader(new FileReader(fullPath), '\t');
			while ((nextLine = reader.readNext()) != null) {
				final double [] pos = new double[numDimensions];
				for (int d = 0; d< numDimensions; d++)
					pos[d] = Double.parseDouble(nextLine[d]);
				list.add(pos);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
//		for (double [] element : list) {
//			String[] nextLine2 = new String[numDimensions];
//			for (int d = 0; d < numDimensions; d++) {
//				nextLine2[d] = String.valueOf(element[d]);
//			}
//			System.out.println(nextLine2[0] + " " + nextLine2[1]);
//
//		}
		
	}

}