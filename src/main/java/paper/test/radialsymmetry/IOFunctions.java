package paper.test.radialsymmetry;

import java.io.FileWriter;
import java.util.ArrayList;

import util.opencsv.CSVWriter;

public class IOFunctions {
	public static void writeCSV(ArrayList<double []> list, String fullPath) {
		CSVWriter writer = null;

		int numDimensions = list.size() > 0 ? list.get(0).length : 0;
		String[] nextLine = new String[numDimensions];

		try {
			// throw if can't create the file
			writer = new CSVWriter(new FileWriter(fullPath), '\t', CSVWriter.NO_QUOTE_CHARACTER);
			for (double [] element : list) {
				for (int d = 0; d < numDimensions; d++) {
					nextLine[d] = String.valueOf(element[d]);
				}
				// System.out.println(nextLine.toString().split(" "));
				writer.writeNext(nextLine);
			}
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	//	public static void readCSV(String from, String to) {
	//		CSVReader reader = null;
	//		String[] nextLine;
	//
	//		try {
	//			reader = new CSVReader(new FileReader(from), '\t');
	//		} catch (FileNotFoundException e) {
	//			e.printStackTrace();
	//		}
	//
	//		try {
	//			int i = 0;
	//			while ((nextLine = reader.readNext()) != null) {
	//				mX.setEntry(i, 0, Double.parseDouble(nextLine[0]));
	//				mX.setEntry(i, 1, Double.parseDouble(nextLine[1]));
	//				i++;
	//			}
	//		} catch (IOException e) {
	//			e.printStackTrace();
	//		}
	//	}

}