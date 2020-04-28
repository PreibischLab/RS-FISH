package result.output;

import java.util.ArrayList;

import fitting.Spot;
import ij.IJ;
import ij.measure.ResultsTable;

public class ShowResult {
	public static void ransacResultCsv(final ArrayList<Spot> spots, final ArrayList<Long> timePoint,
			final ArrayList<Long> channelPoint, ArrayList<Float> intensity) {
		// TODO: Put the writting part here, too (csv?)
	}

	// ineractive mode
	// this function will show the result of RANSAC
	// proper window -> dialog view with the columns
	public static void ransacResultTable(final ArrayList<Spot> spots, final ArrayList<Long> timePoint,
			final ArrayList<Long> channelPoint, ArrayList<Float> intensity, double histThreshold) {
		IJ.log("Running RANSAC ... ");
		// real output
		ResultsTable rt = new ResultsTable();
		String[] xyz = { "x", "y", "z" };
		int currentTimePoint = 0;
		int totalSpotsPerTimePoint = 0;

		int currentChannelPoint = 0;
		int totalSpotsPerChannelPoint = 0;

		int idx = 0;
		for (Spot spot : spots) {
			// if spot was not discarded
			if (spot.inliers.size() != 0) { // TODO: filtered already?
					if (intensity.get(idx) >= histThreshold) {
						rt.incrementCounter();
						double[] pos = spot.getCenter();
						for (int d = 0; d < spot.numDimensions(); ++d) {
							rt.addValue(xyz[d], String.format(java.util.Locale.US, "%.4f", pos[d]));
						}

						totalSpotsPerTimePoint++;
						if (totalSpotsPerTimePoint > timePoint.get(currentTimePoint)) {
							currentTimePoint++;
							totalSpotsPerTimePoint = 1;
						}
						rt.addValue("t", currentTimePoint + 1); // user-friendly, starting the counting from 1

						totalSpotsPerChannelPoint++;
						if (totalSpotsPerChannelPoint > channelPoint.get(currentChannelPoint)) {
							currentChannelPoint++;
							totalSpotsPerChannelPoint = 1;
						}
						rt.addValue("c", currentChannelPoint + 1); // user-friendly, starting the counting from 1

						rt.addValue("intensity", String.format(java.util.Locale.US, "%.4f", intensity.get(idx)));

					}
			}
			idx++;
		}
		IJ.log("Spots found = " + rt.getCounter());
		rt.show("Results");
	}
}
