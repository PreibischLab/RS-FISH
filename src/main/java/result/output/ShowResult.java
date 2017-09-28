package result.output;

import java.util.ArrayList;

import fit.Spot;
import ij.measure.ResultsTable;
import mpicbg.spim.io.IOFunctions;

public class ShowResult {
	// TODO: Put the table here 
	// TODO: Put the writting part here, too (csv?)
	
	// this function will show the result of RANSAC
	// proper window -> dialog view with the columns
	public static void ransacResultTable(final ArrayList<Spot> spots, final ArrayList<Long> timePoint, final ArrayList<Long> channelPoint, ArrayList<Float> intensity) {
		IOFunctions.println("Running RANSAC ... ");
		// real output
		ResultsTable rt = new ResultsTable();
		String[] xyz = { "x", "y", "z" };
		int currentTimePoint = 0; 
		int totalSpotsPerTimePoint = 0;

		int currentChannelPoint = 0; 
		int totalSpotsPerChannelPoint = 0;

		for (Spot spot : spots) {	
			// if spot was not discarded
			if (spot.inliers.size() != 0){
				rt.incrementCounter();
				double[] pos = spot.getCenter();	
				for (int d = 0; d < spot.numDimensions(); ++d) {
					rt.addValue(xyz[d], String.format(java.util.Locale.US, "%.4f", pos[d]));
				}

				totalSpotsPerTimePoint++;
				if (totalSpotsPerTimePoint > timePoint.get(currentTimePoint)){
					currentTimePoint++;
					totalSpotsPerTimePoint = 0;
				}
				rt.addValue("t", currentTimePoint + 1); // user-friendly, starting the counting from 1

				totalSpotsPerChannelPoint++;
				if (totalSpotsPerChannelPoint > channelPoint.get(currentChannelPoint)){
					currentChannelPoint++;
					totalSpotsPerChannelPoint = 0;
				}
				rt.addValue("c", currentChannelPoint + 1); // user-friendly, starting the counting from 1				

				// TODO: REMOVE THIS IF - show the result always
				// if (gaussFit)
				rt.addValue("intensity",  String.format(java.util.Locale.US, "%.4f", intensity.get(rt.getCounter() - 1)) );

			}
		}
		IOFunctions.println("Spots found = " + rt.getCounter()); 
		rt.show("Results");
	}
}
