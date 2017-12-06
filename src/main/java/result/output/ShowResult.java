package result.output;

import java.util.ArrayList;

import fit.Spot;
import ij.measure.ResultsTable;
import mpicbg.spim.io.IOFunctions;

public class ShowResult {
	public static void ransacResultCsv(final ArrayList<Spot> spots, final ArrayList<Long> timePoint,
			final ArrayList<Long> channelPoint, ArrayList<Float> intensity) {
		// TODO: Put the writting part here, too (csv?)
	}

	// ineractive mode
	// this function will show the result of RANSAC
	// proper window -> dialog view with the columns
	public static void ransacResultTable(final ArrayList<Spot> spots, final ArrayList<Long> timePoint,
			final ArrayList<Long> channelPoint, ArrayList<Float> intensity, double histThreshold, int[] roiIndices,
			int currentRoiIdx) {
		IOFunctions.println("Running RANSAC ... ");
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
				if (roiIndices[idx] == currentRoiIdx) {

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
			}
			idx++;
		}
		IOFunctions.println("Spots found = " + rt.getCounter());
		rt.show("Results-" + (currentRoiIdx + 1));
	}

	/**
	 * @param intensity - list of the intensities
	 * @return - returns the min and max allowed intensities for the +/-35% offset
	 * */
	public static float [] filter70percent(final ArrayList<Float> intensity) {
		float [] thresholdVal = new float[]{0, 0};
		double mean = 0;
		
		for (float f : intensity)
			mean += f;
		
		mean /= intensity.size();
		
		// agree that we have to take +/- 35% from the median value => 70% in total
		float borderVal = 0.35f;  
		thresholdVal[0] = (float) ((1 - borderVal)*mean);
		thresholdVal[1] = (float) ((1 + borderVal)*mean);
		
		return thresholdVal;
	}
}
